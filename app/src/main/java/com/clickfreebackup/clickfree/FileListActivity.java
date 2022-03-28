package com.clickfreebackup.clickfree;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clickfreebackup.clickfree.usb.UsbFileAdapterListener;
import com.clickfreebackup.clickfree.usb.UsbFileRecyclerAdapter;
import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import timber.log.Timber;

import static com.clickfreebackup.clickfree.BaseActivity.USB_RETRY_DELAY;

public class FileListActivity extends AppCompatActivity implements UsbFileAdapterListener {
  private static final String ACTION_USB_PERMISSION = "com.github.mjdev.libaums.USB_PERMISSION";
  private static final int REQUEST_EXT_STORAGE_WRITE_PERM = 0;

  private UsbFileRecyclerAdapter adapter;
  private FileSystem currentFs;
  private UsbMassStorageDevice[] massStorageDevices;
  private LinearLayoutManager linearLayoutManager;
  private FileListPresenter fileListPresenter;
  private int currentDevice = -1;
  private final Deque<UsbFile> dirs = new ArrayDeque<>();

  private RecyclerView listView;
  private TextView title;

  private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      if (ACTION_USB_PERMISSION.equals(action)) {
        final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
          if (device != null) {
            setupDevice();
          }
        }
      } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
        final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (device != null) {
          discoverDevice();
        }
      } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
        final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null) {
          if (currentDevice != -1 && massStorageDevices.length - 1 >= currentDevice) {
            massStorageDevices[currentDevice].close();
          }
          discoverDevice();
        }
      }
    }
  };

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    showSnackBar(getString(R.string.loading_files));
    setContentView(R.layout.activity_file_list);
    listView = findViewById(R.id.listview);
    listView = findViewById(R.id.listview);
    title = findViewById(R.id.actionbar_title);
    ImageView backBtn = findViewById(R.id.backarrow);

    fileListPresenter = new FileListPresenter();
    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    registerReceiver(usbReceiver, filter);
    discoverDevice();
    linearLayoutManager = new LinearLayoutManager(this);
    listView.setLayoutManager(linearLayoutManager);
    listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        adapter.setState(newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
      }
    });

    backBtn.setOnClickListener(v -> onBackPressed());
  }

  @Override
  protected void onDestroy() {
    fileListPresenter.onDestroyActivity();
    unregisterReceiver(usbReceiver);
    if (currentDevice >= 0 && currentDevice <= massStorageDevices.length - 1) {
      final UsbMassStorageDevice device = massStorageDevices[currentDevice];
      device.close();
    }
    super.onDestroy();
  }

  private void discoverDevice() {
    final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    massStorageDevices = UsbMassStorageDevice.getMassStorageDevices(this);

    if (massStorageDevices.length == 0) {
      listView.setAdapter(null);
      return;
    }

    currentDevice = 0;
    UsbDevice device = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);

    if (device == null) {
      device = massStorageDevices[0].getUsbDevice();
    }

    if (usbManager.hasPermission(device)) {
      setupDevice();
    } else {
      PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
              ACTION_USB_PERMISSION), 0);
      usbManager.requestPermission(device, permissionIntent);
    }
  }

  private void setupDevice() {
    final Handler handler = new Handler();
    handler.postDelayed(() -> {
      try {
        massStorageDevices[currentDevice].init();
        currentFs = massStorageDevices[currentDevice].getPartitions().get(0).getFileSystem();

        Timber.d("Capacity: %s", currentFs.getCapacity());
        Timber.d("Occupied Space:%s", currentFs.getOccupiedSpace());
        Timber.d("Free Space: %s", currentFs.getFreeSpace());
        Timber.d("Chunk size: %s", currentFs.getChunkSize());

        showSnackBar(getString(R.string.loading_files));
        final UsbFile root = currentFs.getRootDirectory();
        final UsbFile[] usbFiles = root.listFiles();
        final List<UsbFile> files = Arrays.asList(usbFiles);

        dirs.push(root);
        listView.setAdapter(adapter = new UsbFileRecyclerAdapter(FileListActivity.this, FileListActivity.this,
                linearLayoutManager, files));
      } catch (IOException e) {
        Timber.e(e);
      }
    }, USB_RETRY_DELAY);
  }

  @Override
  public void onDirectorySelected(final UsbFile file) {
    try {
      title.setText(file.getName());
      fileListPresenter.initBitmapHandler(this, currentFs);

      showSnackBar(getString(R.string.loading_files));
      final UsbFile[] usbFiles = file.listFiles();
      final List<UsbFile> files = Arrays.asList(usbFiles);

      dirs.push(file);
      listView.setAdapter(adapter = new UsbFileRecyclerAdapter(FileListActivity.this, FileListActivity.this,
              linearLayoutManager, files));
    } catch (IOException e) {
      Timber.e(e);
    }
  }

  @Override
  public void onFileSelected(final UsbFile file) {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

      if (ActivityCompat.shouldShowRequestPermissionRationale(this,
              Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
        Toast.makeText(this, R.string.request_write_storage_perm, Toast.LENGTH_LONG).show();
      } else {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_EXT_STORAGE_WRITE_PERM);
      }

      return;
    }

    final FileListActivity.CopyTaskParam param = new FileListActivity.CopyTaskParam();
    param.from = file;
    param.to = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), file.getName());
    new FileListActivity.CopyTask().execute(param);
  }

  @Override
  public boolean setState(final boolean isIdleState) {
    if (fileListPresenter != null) {
      fileListPresenter.setScrollState(isIdleState);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void loadBitmap(UsbFile file, ImageView imageView) {
    if (fileListPresenter != null) {
      fileListPresenter.loadBitmap(this, file, imageView);
    }
  }

  private static class CopyTaskParam {
    /* package */ UsbFile from;
    /* package */ File to;
  }

  /**
   * Asynchronous task to copy a file uri the mass storage device connected
   * via USB to the internal storage.
   *
   * @author mjahnen
   */
  @SuppressLint("StaticFieldLeak")
  private class CopyTask extends AsyncTask<FileListActivity.CopyTaskParam, Integer, Void> {

    private final ProgressDialog dialog;
    private FileListActivity.CopyTaskParam param;

    public CopyTask() {
      dialog = new ProgressDialog(FileListActivity.this);
      dialog.setTitle("Copying file");
      dialog.setMessage("Copying a file to the internal storage,   this can take some time!");
      dialog.setIndeterminate(false);
      dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      dialog.setCancelable(false);
    }

    @Override
    protected void onPreExecute() {
      dialog.show();
    }

    @Override
    protected Void doInBackground(FileListActivity.CopyTaskParam... params) {
      final long time = System.currentTimeMillis();
      param = params[0];
      try {
        final OutputStream out = new BufferedOutputStream(new FileOutputStream(param.to));
        final InputStream inputStream = new UsbFileInputStream(param.from);
        final byte[] bytes = new byte[currentFs.getChunkSize()];
        int count;
        long total = 0;

        Timber.d("Copy file with length: %s", param.from.getLength());

        while ((count = inputStream.read(bytes)) != -1) {
          out.write(bytes, 0, count);
          total += count;
          int progress = (int) total;
          if (param.from.getLength() > Integer.MAX_VALUE) {
            progress = (int) (total / 1024);
          }
          publishProgress(progress);
        }

        out.close();
        inputStream.close();
      } catch (IOException e) {
        Timber.e(e);
      }
      Timber.d("copy time: %s", (System.currentTimeMillis() - time));
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      dialog.dismiss();

      final Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
      final File file = new File(param.to.getAbsolutePath());
      final String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
      final String mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

      final Uri uri;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        myIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        uri = FileProvider.getUriForFile(FileListActivity.this,
                "com.clickfreebackup.clickfree.provider",
                file);
      } else {
        uri = Uri.fromFile(file);
      }
      myIntent.setDataAndType(uri, mimeType);
      try {
        startActivity(myIntent);
      } catch (ActivityNotFoundException e) {
        Toast.makeText(FileListActivity.this, "Could no find an app for that file!",
                Toast.LENGTH_LONG).show();
      }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
      int max = (int) param.from.getLength();
      if (param.from.getLength() > Integer.MAX_VALUE) {
        max = (int) (param.from.getLength() / 1024);
      }
      dialog.setMax(max);
      dialog.setProgress(values[0]);
    }
  }

  @Override
  public void onBackPressed() {
    try {
      dirs.pop();
      final UsbFile dir = dirs.getFirst();

      if (dir.isRoot()) {
        title.setText(App.getContext().getString(R.string.view_files));
      } else {
        title.setText(dir.getName());
      }

      showSnackBar(getString(R.string.loading_files));
      final UsbFile[] usbFiles = dir.listFiles();
      final List<UsbFile> files = Arrays.asList(usbFiles);

      listView.setAdapter(adapter = new UsbFileRecyclerAdapter(FileListActivity.this, FileListActivity.this,
              linearLayoutManager, files));
    } catch (Exception e) {
      super.onBackPressed();
    }
  }

  private void showSnackBar(String message) {
    final View parentLayout = findViewById(android.R.id.content);
    final Snackbar snackbar = Snackbar.make(parentLayout, message, Snackbar.LENGTH_LONG);
    snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.colorAccent));
    snackbar.show();
  }
}

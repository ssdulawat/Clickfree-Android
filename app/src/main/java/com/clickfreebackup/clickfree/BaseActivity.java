package com.clickfreebackup.clickfree;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.clickfreebackup.clickfree.usb.UsbOtgManager;
import com.clickfreebackup.clickfree.util.Util;
import com.github.mjdev.libaums.UsbMassStorageDevice;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public abstract class BaseActivity extends AppCompatActivity {
    public static final long USB_RETRY_DELAY = 1000;
    private static final String ACTION_USB_PERMISSION = "com.github.mjdev.libaums.USB_PERMISSION";
    private UsbMassStorageDevice[] massStorageDevices;
    private int currentDevice = -1;
    private ProgressDialog progressDialog;
    private AlertDialog alertDialog;
    private final IntentFilter broadcastIntentFilter = new IntentFilter();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BackupService.BROADCAST_ACTION_PROGRESS:
                        ProgressParams progressParams = intent.getParcelableExtra(BackupService.PARAM_PROGRESS);
                        onProgressReceived(progressParams);
                        break;
                    case BackupService.BROADCAST_ACTION_MESSAGE:
                        String message = intent.getStringExtra(BackupService.PARAM_MESSAGE);
                        onMessageReceived(message);
                        break;
                    case BackupService.BROADCAST_ACTION_POPUP:
                        String title = intent.getStringExtra(BackupService.PARAM_TITLE);
                        String text = intent.getStringExtra(BackupService.PARAM_TEXT);
                        onPopupReceived(title, text);
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        broadcastIntentFilter.addAction(BackupService.BROADCAST_ACTION_MESSAGE);
        broadcastIntentFilter.addAction(BackupService.BROADCAST_ACTION_POPUP);
        broadcastIntentFilter.addAction(BackupService.BROADCAST_ACTION_PROGRESS);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);
    }

    public void showProgressDialog(String title, String text, boolean showCancelButton) {
        if (progressDialog != null) progressDialog.dismiss();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(title);
        progressDialog.setMessage(text);
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setProgressNumberFormat(null);
        progressDialog.setProgressPercentFormat(null);
        if (showCancelButton) {
            progressDialog.setButton(
                    ProgressDialog.BUTTON_NEGATIVE,
                    getString(R.string.stop),
                    (DialogInterface.OnClickListener) null);
        }

        progressDialog.show();

        if (showCancelButton) {
            progressDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(view -> confirmCancelBackup());
        }
    }

    public void hideProgressDialog() {
        if (progressDialog != null) progressDialog.dismiss();
    }

    public void setCurrentProgress(int progress) {
        runOnUiThread(() -> progressDialog.setProgress(progress));
    }

    public void setMaxProgress(int maxProgress) {
        runOnUiThread(() -> {
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
            progressDialog.setMax(maxProgress);
        });
    }

    private void confirmCancelBackup() {
        if (alertDialog != null && alertDialog.isShowing()) alertDialog.dismiss();

        alertDialog = new AlertDialog.Builder(this)
                .setTitle(null)
                .setMessage(R.string.confirm_cancel_backup)
                .setPositiveButton(
                        R.string.stop_backup,
                        (dialog, which) -> onStopBackupClicked()
                )
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    protected void onStopBackupClicked() {
        Intent startIntent = new Intent(this, BackupService.class);
        startIntent.setAction(BackupService.ACTION_STOP_FOREGROUND_SERVICE);
        startService(startIntent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getAction() != null && intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            discoverDevice();
        }
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!hasOtgSupport()) {
            showInfoPopup(getString(R.string.device_not_supported), getString(R.string.otg_not_supported));
        }

        registerReceiver(broadcastReceiver, broadcastIntentFilter);

        // If backup service is running, request current backup progress
        if (Util.isServiceRunning(BackupService.class)) {
            Intent startIntent = new Intent(this, BackupService.class);
            startIntent.setAction(BackupService.ACTION_REQUEST_PROGRESS);
            startService(startIntent);
        } else {
            hideProgressDialog();
        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!UsbOtgManager.getInstance().isBusy()) discoverDevice();
    }

    private boolean hasOtgSupport() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST);
    }

    public void showProgressDialog(ProgressParams progressParams) {
        runOnUiThread(() -> {
                    if (progressDialog == null || !progressDialog.isShowing()) {
                        showProgressDialog(progressParams.getTitle(), progressParams.getText(), progressParams.isAllowCancel());
                    }
                    if (progressParams.getTitle() != null)
                        progressDialog.setTitle(progressParams.getTitle());
                    if (progressParams.getText() != null)
                        progressDialog.setMessage(progressParams.getText());
                    if (progressParams.getMaxProgress() >= 0) {
                        progressDialog.setIndeterminate(false);
                        if (progressParams.isProgressPercents()) {
                            progressDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
                        } else {
                            progressDialog.setProgressPercentFormat(NumberFormat.getNumberInstance());
                        }
                        progressDialog.setMax(progressParams.getMaxProgress());
                    }
                    if (progressParams.isStopping()) {
                        progressDialog.setIndeterminate(true);
                        progressDialog.setTitle(getString(R.string.stopping));
                        progressDialog.setMessage(null);
                        progressDialog.setProgressPercentFormat(null);
                        progressDialog.getButton(ProgressDialog.BUTTON_NEGATIVE).setVisibility(View.GONE);
                    }
                    if (progressParams.getCurrentProgress() >= 0)
                        progressDialog.setProgress(progressParams.getCurrentProgress());
                }
        );
    }

    protected abstract void onProgressReceived(ProgressParams progressParams);

    protected abstract void onMessageReceived(String message);

    void showSnackBar(final String message, final int color) {}

    protected abstract void onPopupReceived(String title, String text);

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    discoverDevice();
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Timber.e("USB device attached");

                discoverDevice();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                Timber.d("USB device detached");

                onDeviceDetached(device);
                // determine if connected device is a mass storage device
                if (device != null) {
                    if (currentDevice != -1 && massStorageDevices.length - 1 >= currentDevice && massStorageDevices[currentDevice].getPartitions() != null) {
                        massStorageDevices[currentDevice].close();
                    }
                    // check if there are other devices or set action bar title
                    // to no device if not
                    discoverDevice();
                }
            }
        }
    };

    public void setMaxProgressAbsolute(int maxProgressAbsolute) {
        runOnUiThread(() -> {
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressNumberFormat("%1d of %2d");
            progressDialog.setMax(maxProgressAbsolute);
        });
    }

    public void setProgressDialogText(String title, String text) {
        runOnUiThread(() -> {
            if (progressDialog != null) {
                if (title != null) progressDialog.setTitle(title);
                progressDialog.setMessage(text);
            }
        });
    }

    public void showInfoPopup(String title, String text) {
        if (alertDialog != null && alertDialog.isShowing()) alertDialog.dismiss();

        alertDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(text)
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    abstract void onDeviceDetached(UsbDevice device);

    abstract void setupDevice();

    private void discoverDevice() {
      final Handler handler = new Handler();
      handler.postDelayed(() -> compositeDisposable.add(getMassStorageDevices()
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe((devices) -> {
                massStorageDevices = devices;
                processIncomingDevices();
              }, Timber::e)), USB_RETRY_DELAY);
    }

    private void processIncomingDevices() {
      final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

      if (massStorageDevices.length == 0) {
        Timber.e("No device found!");
        showSnackBar(getString(R.string.connecting_to_usb), ContextCompat.getColor(this, R.color.colorAccent));
        return;
      }

      currentDevice = 0;

      UsbDevice usbDevice = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
      if (usbDevice == null) {
        final List<UsbDevice> devices = new ArrayList<>(usbManager.getDeviceList().values());
        usbDevice = devices.get(0);
      }

      if (usbDevice != null && usbManager.hasPermission(usbDevice)) {
        Timber.e("Received usb device via intent");
        setupDevice();
      } else {
        final PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(usbDevice, permissionIntent);
      }
    }

    private Observable<UsbMassStorageDevice[]> getMassStorageDevices() {
      return Observable.fromCallable(() -> UsbMassStorageDevice.getMassStorageDevices(BaseActivity.this));
    }

    protected void startBackupService(String task) {
        Intent startIntent = new Intent(this, BackupService.class);
        startIntent.setAction(BackupService.ACTION_START_FOREGROUND_SERVICE);
        startIntent.putExtra(BackupService.TASK, task);
        startService(startIntent);
    }

    protected void startBackupServiceInsta(String task, HashMap<String, String> mediaUrlMap) {
        Intent startIntent = new Intent(this, BackupService.class);
        startIntent.setAction(BackupService.ACTION_START_FOREGROUND_SERVICE);
        startIntent.putExtra(BackupService.TASK, task);
        startIntent.putExtra(BackupService.IMAGE_URL_MAP, mediaUrlMap);
        startService(startIntent);
    }

    protected void startBackupServiceFb(String task, HashMap<String, HashSet<String>> mediaUrlMap) {
        Intent startIntent = new Intent(this, BackupService.class);
        startIntent.setAction(BackupService.ACTION_START_FOREGROUND_SERVICE);
        startIntent.putExtra(BackupService.TASK, task);
        startIntent.putExtra(BackupService.IMAGE_URL_MAP, mediaUrlMap);
        startService(startIntent);
    }

    @Override
    protected void onDestroy() {
        if (!Util.isServiceRunning(BackupService.class)) {
            if (massStorageDevices != null && massStorageDevices.length > 0 && massStorageDevices[0].getPartitions() != null) {
                massStorageDevices[0].close();
            }
        }

        unregisterReceiver(usbReceiver);
        if (!compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
            compositeDisposable.clear();
        }
        super.onDestroy();
    }
}

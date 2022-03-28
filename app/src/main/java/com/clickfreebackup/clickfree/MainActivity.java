package com.clickfreebackup.clickfree;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;

import com.clickfreebackup.clickfree.model.ContactBody;
import com.clickfreebackup.clickfree.repository.SendGridRepositoryListener;
import com.clickfreebackup.clickfree.usb.UsbFileParams;
import com.clickfreebackup.clickfree.util.SharedPreferencesManager;
import com.clickfreebackup.clickfree.view.FirstRunFragmentListener;
import com.clickfreebackup.clickfree.view.MainDialogFragment;
import com.clickfreebackup.clickfree.view.MainDialogListener;
import com.clickfreebackup.clickfree.view.first_run_fragment.FirstRunScreensFragment;
import com.clickfreebackup.clickfree.view.frequent_questions_fragment.FrequentQuestionListener;
import com.clickfreebackup.clickfree.view.frequent_questions_fragment.FrequentQuestionsFragment;
import com.google.android.material.snackbar.Snackbar;
import com.wouterhabets.slidingcontentdrawer.widget.SlidingDrawerLayout;
import com.wouterhabets.slidingcontentdrawer.widget.SlidingDrawerToggle;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

import static com.clickfreebackup.clickfree.BackupService.TASK_BACKUP_CAMERA_PHOTOS;

public class MainActivity extends BaseActivity implements MainView, FirstRunFragmentListener, MainDialogListener, FrequentQuestionListener {
    public static final String STORAGE_REQUEST_PERMISSION_DIALOG_TYPE = "STORAGE_REQUEST_PERMISSION_DIALOG_TYPE";
    public static final String CAMERA_REQUEST_PERMISSION_DIALOG_TYPE = "CAMERA_REQUEST_PERMISSION_DIALOG_TYPE";
    public static final String CONTACT_REQUEST_PERMISSION_DIALOG_TYPE = "CONTACT_REQUEST_PERMISSION_DIALOG_TYPE";
    public static final String CONTACT_US_DIALOG_TYPE = "CONTACT_US_DIALOG_TYPE";
    private static final String CONTACT_US = "CONTACT_US";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int LAUNCH_COUNTER_LIMIT = 19;
    private static final int REQUEST_CAPTURE_IMAGE = 1888;
    private static final int PERMISSION_REQUEST_CODE_CAMERA = 200;
    private static final int PERMISSION_REQUEST_CODE_STORAGE = 201;
    private static final int PERMISSION_REQUEST_CODE_CONTACT = 202;
    private static final String ARG_IMAGE_FILE = "imageFile";
    private ImageView backupAll;
    private ArrayList<Uri> photoUris;
    private FirstRunScreensFragment mFirstRunScreensFragment;
    private SharedPreferencesManager mSharedPreferencesManager;
    private TextView formatDevice, getHelp, frequentQuestions;
    private File imageFile;
    private SlidingDrawerLayout drawerLayout;
    private LinearLayout backupContact, cameraUse;
    private MainActivityPresenter presenter;
    private LinearLayout photos, storage;
    private View backupEverythingLayout;
    private MainDialogFragment mMainDialogFragment;
    private FragmentManager supportFragmentManager;
    private FrequentQuestionsFragment mFrequentQuestionsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSharedPreferencesManager = SharedPreferencesManager.getInstance(this);
        if (savedInstanceState != null && savedInstanceState.containsKey(ARG_IMAGE_FILE)) {
            String imageFilePath = savedInstanceState.getString(ARG_IMAGE_FILE);
            imageFile = new File(imageFilePath);
        }

        presenter =
                (getLastNonConfigurationInstance() instanceof MainActivityPresenter ?
                        (MainActivityPresenter) getLastNonConfigurationInstance() : null);
        if (presenter == null) {
            presenter = new MainActivityPresenter();
        }

        presenter.attachView(this);

        setContentView(R.layout.activity_main);

        supportFragmentManager = getSupportFragmentManager();

        showFirstRunScreens();

        showRateUsScreen();

        Toolbar toolbar = findViewById(R.id.toolbar);

        initViews();

        if (!hasStoragePermission()) {
            requestStoragePermission();
        }

        formatDevice.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setCancelable(false);
            builder.setMessage("Are you sure you want to remove/delete all the data?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", (dialog, id) -> {
                        dialog.dismiss();
                        presenter.onFormatButtonClicked();
                    })
                    .setNegativeButton("No", (dialog, id) -> dialog.cancel());
            AlertDialog alert = builder.create();
            alert.show();
        });

        storage.setOnClickListener(v -> presenter.onStorageClicked());

        photos.setOnClickListener(view -> {
            if (hasStoragePermission()) {
                presenter.onBackupPhotoVideoClicked();
            } else {
                mMainDialogFragment = new MainDialogFragment(this,
                        STORAGE_REQUEST_PERMISSION_DIALOG_TYPE,
                        getResources().getText(R.string.permission_access_text).toString(),
                        getResources().getText(R.string.photo_video_permission_description).toString()
                );
                mMainDialogFragment.show(supportFragmentManager, STORAGE_REQUEST_PERMISSION_DIALOG_TYPE);
            }
        });

        cameraUse.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                presenter.onCameraClicked();
            } else {
                mMainDialogFragment = new MainDialogFragment(
                        this,
                        CAMERA_REQUEST_PERMISSION_DIALOG_TYPE,
                        getResources().getText(R.string.permission_access_text).toString(),
                        getResources().getText(R.string.camera_permission_description).toString()
                );
                mMainDialogFragment.show(supportFragmentManager, CAMERA_REQUEST_PERMISSION_DIALOG_TYPE);
            }
        });

        backupContact.setOnClickListener(v -> {
            if (hasContactPermission()) {
                presenter.onBackupContactsClicked();
            } else {
                mMainDialogFragment = new MainDialogFragment(
                        this,
                        CONTACT_REQUEST_PERMISSION_DIALOG_TYPE,
                        getResources().getText(R.string.permission_access_text).toString(),
                        getResources().getText(R.string.contact_permission_description).toString()
                );
                mMainDialogFragment.show(supportFragmentManager, CONTACT_REQUEST_PERMISSION_DIALOG_TYPE);
            }
        });

        SlidingDrawerLayout drawer = findViewById(R.id.drawer_layout);
        SlidingDrawerToggle toggle = new SlidingDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        backupAll.setOnClickListener(v -> {
            if (hasContactPermission()) {
                if (hasStoragePermission()) {
                    presenter.onBackupEverythingClicked();
                } else {
                    requestStoragePermission();
                }
            } else {
                requestContactPermission();
            }
        });

        getHelp.setOnClickListener(v -> openHelpInBrowser());

        frequentQuestions.setOnClickListener(view -> {
            mFrequentQuestionsFragment = new FrequentQuestionsFragment(this);
            supportFragmentManager.beginTransaction().add(R.id.slides_container, mFrequentQuestionsFragment).commit();
        });
    }

    private void showRateUsScreen() {
        final int launchCounter = mSharedPreferencesManager.getLaunchCounter();
        if (launchCounter < LAUNCH_COUNTER_LIMIT) {
            mSharedPreferencesManager.setLaunchCounter(launchCounter + 1);
        } else if (launchCounter == LAUNCH_COUNTER_LIMIT) {
            mSharedPreferencesManager.setLaunchCounter(launchCounter + 1);
            mMainDialogFragment = new MainDialogFragment(this, CONTACT_US_DIALOG_TYPE, "", "");
            mMainDialogFragment.show(supportFragmentManager, CONTACT_US);
        }
    }

    private void showFirstRunScreens() {
        if (mSharedPreferencesManager.isFirstStart()) {
            mFirstRunScreensFragment = new FirstRunScreensFragment(this);
            supportFragmentManager.beginTransaction().add(R.id.slides_container, mFirstRunScreensFragment).commit();
        }
    }

    @Override
    protected void onProgressReceived(ProgressParams progressParams) {
        presenter.onProgressReceived(progressParams);
    }

    @Override
    protected void onMessageReceived(String message) {
        showToast(message);
    }

    @Override
    public void showSnackBar(final String message, final int color) {
        final View bottomLine = findViewById(R.id.screen_bottom_line_view);
        final Snackbar snackbar = Snackbar.make(bottomLine, message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(color);
        snackbar.setAnchorView(bottomLine);
        snackbar.show();
    }

    @Override
    protected void onPopupReceived(String title, String text) {
        showInfoPopup(title, text);
    }

    @Override
    public void startBackupContactsTask() {
        startBackupService(BackupService.TASK_BACKUP_CONTACTS);
    }

    @Override
    public void startBackupEverythingTask() {
        startBackupService(BackupService.TASK_BACKUP_EVERYTHING);
    }

    @Override
    public void startClearStorageTask() {
        startBackupService(BackupService.TASK_CLEAR_STORAGE);
    }

    public void initViews() {
        getHelp = findViewById(R.id.getHelp);
        formatDevice = findViewById(R.id.formatDevice);
        frequentQuestions = findViewById(R.id.frequent_questions);
        backupAll = findViewById(R.id.backupall);
        cameraUse = findViewById(R.id.cameraUse);
        photos = findViewById(R.id.photosvideos);
        backupEverythingLayout = findViewById(R.id.backup_everything_layout);
        storage = findViewById(R.id.storage);
        backupContact = findViewById(R.id.backupContact);
        drawerLayout = findViewById(R.id.drawer_layout);

        addPulseAnimation();
    }

    private void addPulseAnimation() {
        ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                backupEverythingLayout,
                PropertyValuesHolder.ofFloat("scaleX", 1.08f),
                PropertyValuesHolder.ofFloat("scaleY", 1.08f));
        //scaleDown.
        scaleDown.setRepeatCount(3);
        scaleDown.setDuration(310);
        scaleDown.setRepeatMode(ObjectAnimator.REVERSE);
        scaleDown.setStartDelay(2000);
        scaleDown.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                scaleDown.start();
            }
        });

        scaleDown.start();
    }

    private void openHelpInBrowser() {
        drawerLayout.closeDrawer();

        String url = "https://www.datalogixxmemory.com/";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            return false;
        }
        return true;
    }

    @Override
    public void openStorage() {
        startActivity(new Intent(getApplicationContext(), FileListActivity.class));
    }

    @Override
    public void openPhotoVideoBackup() {
        startActivity(new Intent(getApplicationContext(), PhotosVideosListActivity.class));
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE_CAMERA);
    }

    private boolean hasStoragePermission() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE_STORAGE);
    }

    private boolean hasContactPermission() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestContactPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_CODE_CONTACT);
    }

    @Override
    public void showPhotoVideoSelectionPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(false);
        builder.setMessage(getString(R.string.select_photo_video))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.photo), (dialog, id) -> {
                    photoUris = new ArrayList<>();
                    presenter.onTakePhotoClicked();
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.video
                ), (dialog, id) -> {
                    presenter.onMakeVideoClicked();
                    dialog.cancel();
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Save photo to storage
        if (requestCode == REQUEST_CAPTURE_IMAGE && resultCode == Activity.RESULT_OK) {
            if (imageFile == null) Timber.e(TAG, "ImageFile is null!!! How could that happen?!");
            Uri photoUri = Uri.fromFile(imageFile);
            imageFile = null;
            handlePhotoTaken(photoUri);
            launchPhotoCamera();
        } else if (requestCode == REQUEST_CAPTURE_IMAGE && resultCode == Activity.RESULT_CANCELED) {
            presenter.onPhotosTaken(photoUris);
        }
    }

    private void handlePhotoTaken(Uri photoUri) {
        if (photoUris != null) {
            photoUris.add(photoUri);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        imageFile = new File(storageDir, imageFileName + ".jpg");

        return imageFile;
    }

    private File createVideoFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        imageFile = new File(storageDir, imageFileName + ".mp4");

        return imageFile;
    }

    @Nullable
    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return presenter; // save presenter through activity configuration changes (screen rotation, etc.)
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presenter.onCameraClicked();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            case PERMISSION_REQUEST_CODE_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //getImagePaths(this);
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            case PERMISSION_REQUEST_CODE_CONTACT:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presenter.onBackupContactsClicked();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void launchPhotoCamera() {
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (pictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Timber.e(ex);
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.clickfreebackup.clickfree.provider", photoFile);
                pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(pictureIntent, REQUEST_CAPTURE_IMAGE);
            }
        }
    }

    @Override
    public void launchVideoCamera() {
        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (videoIntent.resolveActivity(getPackageManager()) != null) {
            File videoFile = null;
            try {
                videoFile = createVideoFile();
            } catch (IOException ex) {
                Timber.e(ex);
            }
            if (videoFile != null) {
                Uri videoURI = FileProvider.getUriForFile(this, "com.clickfreebackup.clickfree.provider", videoFile);
                videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI);
                startActivityForResult(videoIntent, REQUEST_CAPTURE_IMAGE);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (imageFile != null) {
            outState.putString(ARG_IMAGE_FILE, imageFile.getAbsolutePath());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    void onDeviceDetached(UsbDevice device) {
        if (presenter != null) presenter.onDeviceDetached(device);
    }

    @Override
    void setupDevice() {
        if (presenter != null) presenter.setupDevice();
    }

    @Override
    protected void onDestroy() {
        if (presenter != null) {
            presenter.detachView();
            presenter.cleanUp();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        SlidingDrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void startSaveCameraPhotoFilesTask(ArrayList<UsbFileParams> fileParams) {
        Intent startIntent = new Intent(this, BackupService.class);
        startIntent.setAction(BackupService.ACTION_START_FOREGROUND_SERVICE);
        startIntent.putExtra(BackupService.TASK, TASK_BACKUP_CAMERA_PHOTOS);
        startIntent.putExtra(BackupService.PARAM_CAMERA_PHOTOS, fileParams);
        startService(startIntent);
    }

    @Override
    public void onUsbConnected() {
        showSnackBar(getString(R.string.usb_connected), ContextCompat.getColor(this, R.color.green));
    }

    @Override
    public void onInstructionsPassed() {
        mSharedPreferencesManager.setFirstStart(false);
        supportFragmentManager.beginTransaction().remove(mFirstRunScreensFragment).commit();
    }

    @Override
    public void onSendEmailButtonClicked(final ContactBody contactBody, final SendGridRepositoryListener sendGridRepositoryListener) {
        presenter.onSendEmailButtonClicked(contactBody, sendGridRepositoryListener);
    }

    @Override
    public void onDismissDialog() {
        mMainDialogFragment.dismiss();
    }

    @Override
    public void onStoragePermissionClicked() {
        requestStoragePermission();
    }

    @Override
    public void onContactPermissionClicked() {
        requestContactPermission();
    }

    @Override
    public void onCameraPermissionClicked() {
        requestCameraPermission();
    }

    @Override
    public void onBackPressedFQ() {
        supportFragmentManager.beginTransaction().remove(mFrequentQuestionsFragment).commit();
    }
}

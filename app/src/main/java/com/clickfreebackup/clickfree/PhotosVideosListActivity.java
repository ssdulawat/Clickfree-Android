package com.clickfreebackup.clickfree;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clickfreebackup.clickfree.model.InstagramMediaItem;
import com.clickfreebackup.clickfree.model.UserData;
import com.clickfreebackup.clickfree.repository.FacebookDataListener;
import com.clickfreebackup.clickfree.usb.UsbFileParams;
import com.clickfreebackup.clickfree.util.SharedPreferencesManager;
import com.clickfreebackup.clickfree.view.MainDialogFragment;
import com.clickfreebackup.clickfree.view.MainDialogListener;
import com.facebook.FacebookSdk;
import com.google.android.material.snackbar.Snackbar;
import com.jaiselrahman.filepicker.activity.FilePickerActivity;
import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.model.MediaFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import timber.log.Timber;

import static com.clickfreebackup.clickfree.BackupService.TASK_BACKUP_FACEBOOK_PHOTO;
import static com.clickfreebackup.clickfree.BackupService.TASK_BACKUP_INSTAGRAM_PHOTO;
import static com.clickfreebackup.clickfree.BackupService.TASK_BACKUP_SELECTED;
import static com.clickfreebackup.clickfree.BackupService.TASK_BACKUP_SELECTED_FACEBOOK_PHOTO;
import static com.clickfreebackup.clickfree.BackupService.TASK_BACKUP_SELECTED_INSTAGRAM_PHOTO;
import static com.clickfreebackup.clickfree.util.Const.FACEBOOK;
import static com.clickfreebackup.clickfree.util.Const.INSTAGRAM;
import static com.clickfreebackup.clickfree.util.Util.getIdFromUrl;

public class PhotosVideosListActivity extends BaseActivity implements PhotosVideosView, InstagramDataListener,
        FacebookDataListener, FacebookProgressListener, MainDialogListener {
    public static final String FACEBOOK_USER_ACCESS_TOKEN = "facebook_access_token";
    public static final String FACEBOOK_USER_ID = "facebook_id";
    private static final int COPY_STORAGE_PROVIDER_RESULT = 0;
    private static final String COMING_SOON_DIALOG_TYPE = "coming soon";
    private static final String ACTION_USB_PERMISSION = "com.github.mjdev.libaums.USB_PERMISSION";
    private PhotosVideosPresenter presenter;
    private FragmentManager supportFragmentManager;
    private FacebookAuthDialog facebookAuthDialog;
    private ProgressBar progressBar;
    private RecyclerView imageGalleryRecyclerview;
    private AlertDialog alertDialog;
    private AlertDialog facebookRetrievingAlertDialog;
    private ToolbarView galleryToolbar;
    private LinearLayout galleryLayout;
    private MainDialogFragment mMainDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());
        supportFragmentManager = getSupportFragmentManager();
        presenter = (getLastNonConfigurationInstance() instanceof PhotosVideosPresenter ?
                (PhotosVideosPresenter) getLastNonConfigurationInstance() : null);
        if (presenter == null) {
            presenter = new PhotosVideosPresenter(this,
                    this,
                    SharedPreferencesManager.getInstance(this));
        }
        presenter.attachView(this);

        setContentView(R.layout.activity_photos_videos_list);

        setupViews();
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
    protected void onPopupReceived(String title, String text) {
        showInfoPopup(title, text);
    }

    @Override
    public void startSaveSelectedFilesTask(ArrayList<UsbFileParams> fileParams) {
        Intent startIntent = new Intent(this, BackupService.class);
        startIntent.setAction(BackupService.ACTION_START_FOREGROUND_SERVICE);
        startIntent.putExtra(BackupService.TASK, TASK_BACKUP_SELECTED);
        startIntent.putExtra(BackupService.PARAM_SELECTED_FILES, fileParams);
        startService(startIntent);
        presenter.clearSelectedFiles();
    }

    private void setupViews() {
        final LinearLayout backupAllPhotosVideos = findViewById(R.id.photosvideos);
        final LinearLayout selectPhotosVideos = findViewById(R.id.selectPhotosVideos);
        final LinearLayout backupInstagramPhotos = findViewById(R.id.backup_instagram_photos);
        final LinearLayout backupFacebookPhotos = findViewById(R.id.backup_facebook_photos);
        progressBar = findViewById(R.id.progress_bar);
        galleryToolbar = findViewById(R.id.gallery_toolbar);
        galleryLayout = findViewById(R.id.gallery_layout);
        imageGalleryRecyclerview = findViewById(R.id.imageGalleryRecyclerview);
        imageGalleryRecyclerview.setHasFixedSize(true);
        imageGalleryRecyclerview.setLayoutManager(new GridLayoutManager(getApplicationContext(), 3));

        ImageView backArrow = findViewById(R.id.backarrow);
        backArrow.setOnClickListener(view -> finish());

        backupAllPhotosVideos.setOnClickListener(view -> presenter.onBackupAllMediaFilesClicked());
        selectPhotosVideos.setOnClickListener(view -> presenter.onSelectPhotosClicked());
        backupInstagramPhotos.setOnClickListener(view -> {
//            todo: after facebook confirmation execute: checkIfUserLoggedInInstagram();
            mMainDialogFragment = new MainDialogFragment(
                    this,
                    COMING_SOON_DIALOG_TYPE,
                    getResources().getText(R.string.coming_soon_text).toString(),
                    getResources().getText(R.string.feature_will_be_added_text).toString()
            );
            mMainDialogFragment.show(supportFragmentManager, COMING_SOON_DIALOG_TYPE);
        });
        backupFacebookPhotos.setOnClickListener(view -> {
//            todo: after facebook confirmation execute: checkIfUserLoggedInFacebook();
            mMainDialogFragment = new MainDialogFragment(
                    this,
                    COMING_SOON_DIALOG_TYPE,
                    getResources().getText(R.string.coming_soon_text).toString(),
                    getResources().getText(R.string.feature_will_be_added_text).toString()
            );
            mMainDialogFragment.show(supportFragmentManager, COMING_SOON_DIALOG_TYPE);
        });
    }

    private void checkIfUserLoggedInInstagram() {
        presenter.setInstagramListeners(this, this);
        presenter.callInstagramUsername();
    }

    private void checkIfUserLoggedInFacebook() {
        final HashMap<String, String> facebookUserIdentifiers = presenter.getFacebookUserIdentifiers();
        final String userAccessToken = facebookUserIdentifiers.get(FACEBOOK_USER_ACCESS_TOKEN);
        final String userId = facebookUserIdentifiers.get(FACEBOOK_USER_ID);
        if (userAccessToken != null && !userAccessToken.isEmpty() && userId != null && !userId.isEmpty()) {
            presenter.getFacebookUserName(userId);
        } else {
            createFacebookAuthDialogFragment(false);
        }
    }

    private void createFacebookAuthDialogFragment(boolean isLogged) {
        facebookAuthDialog = new FacebookAuthDialog(presenter, this, isLogged);
        supportFragmentManager.beginTransaction()
                .add(R.id.main_container, facebookAuthDialog).commit();
    }

    private void createInstagramAuthWebView() {
        final InstagramAuthDialog instagramAuthDialog = new InstagramAuthDialog(this, this, presenter);
        instagramAuthDialog.setCancelable(true);
        instagramAuthDialog.show();
    }

    @Override
    void onDeviceDetached(UsbDevice device) {
        presenter.onDeviceDetached(device);
    }

    @Override
    void setupDevice() {
        presenter.setupDevice();
    }

    @Override
    public void showImagePicker() {
        ArrayList<MediaFile> mediaFiles = new ArrayList<>();

        Intent intent = new Intent(PhotosVideosListActivity.this, FilePickerActivity.class);
        intent.putExtra(FilePickerActivity.CONFIGS, new Configurations.Builder()
                .setCheckPermission(true)
                .setSelectedMediaFiles(mediaFiles)
                .enableImageCapture(true)
                .enableVideoCapture(true)
                .setIgnoreHiddenFile(true)
                .setShowVideos(true)
                .setSkipZeroSizeFiles(true)
                .build());

        presenter.setUsbManagerNotReady();
        presenter.clearSelectedFiles();
        startActivityForResult(intent, COPY_STORAGE_PROVIDER_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == COPY_STORAGE_PROVIDER_RESULT) {
            if (data != null) {
                presenter.onSelectedMediaFilesReceived(new ArrayList<>(data.getParcelableArrayListExtra(FilePickerActivity.MEDIA_FILES)));
            }
        }
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void startBackupAllPhotoVideoTask() {
        startBackupService(BackupService.TASK_BACKUP_PHOTOVIDEO);
    }

    @Override
    protected void onDestroy() {
        if (presenter != null) {
            presenter.detachView();
            presenter.cleanUp();
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return presenter;
    }

    @Override
    public void onInstagramMediaData(List<InstagramMediaItem> instagramMediaItems, ClearListener clearListener) {
        if (instagramMediaItems != null) {
            presenter.onBackupInstagramPhoto(instagramMediaItems);
            clearListener.clearListeners();
        }
        removeCookie();
    }

    @Override
    public void onInstagramUserLoggedAs(UserData userData) {
        if (userData != null && userData.getUsername() != null && !userData.getUsername().isEmpty()) {
            showInstagramLoggedInDialog(userData.getUsername());
        } else {
            createInstagramAuthWebView();
        }
    }

    @Override
    public void onTokenExpired(boolean isTokenExpired) {
        if (isTokenExpired) {
            showInstagramTokenExpired();
        }
    }

    @Override
    public void onFacebookMediaData(HashMap<String, HashSet<String>> facebookMediaSources) {
        onSaveFacebookMediaData(facebookMediaSources);
        supportFragmentManager.beginTransaction().remove(facebookAuthDialog).commit();
    }

    @Override
    public void onFacebookUsername(String username) {
        showFacebookLoggedInDialog(username);
    }

    private void showFacebookLoggedInDialog(String username) {
        final Resources resources = getResources();
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(resources.getString(R.string.notification_title_confirm))
                .setMessage(resources.getString(R.string.notification_logged_in_description_text, username))
                .setPositiveButton(resources.getString(R.string.notification_text_continue), (dialog, which) -> createFacebookAuthDialogFragment(true))
                .setNegativeButton(resources.getString(R.string.com_facebook_loginview_log_out_button), (dialogInterface, i) -> {
                    removeCookie();
                    presenter.onFacebookLogOut();
                    alertDialog.hide();
                })
                .show();
    }

    private void showInstagramLoggedInDialog(String username) {
        final Resources resources = getResources();
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(resources.getString(R.string.notification_title_confirm))
                .setMessage(resources.getString(R.string.notification_logged_in_description_text, username))
                .setPositiveButton(resources.getString(R.string.notification_text_continue), (dialog, which) -> presenter.onUserMedia())
                .setNegativeButton(resources.getString(R.string.com_facebook_loginview_log_out_button), (dialogInterface, i) -> {
                    removeCookie();
                    presenter.onInstagramLogOut();
                    alertDialog.hide();
                })
                .show();
    }

    private void showInstagramTokenExpired() {
        final Resources resources = getResources();
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(resources.getString(R.string.notification_title_token_expired))
                .setMessage(resources.getString(R.string.notification_token_expired_description_text))
                .setPositiveButton(resources.getString(R.string.ok), (dialog, which) -> {
                    removeCookie();
                    presenter.onInstagramLogOut();
                    alertDialog.hide();
                })
                .show();
    }

    private void onSaveFacebookMediaData(HashMap<String, HashSet<String>> facebookMediaSources) {
        if (facebookMediaSources != null && !facebookMediaSources.isEmpty())
            presenter.onHandleFacebookMediaSources(facebookMediaSources);
    }

    private void removeCookie() {
        Timber.d("Clear Cookie");
        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
    }

    @Override
    public void onBackupInstagramPhoto(HashMap<String, String> mediaUrlMap) {
        showInstagramDownloadOptions(mediaUrlMap);
    }

    @Override
    public void onBackupFacebookPhoto(HashMap<String, HashSet<String>> mediaUrlMap) {
        showFbDownloadOptions(mediaUrlMap);
    }

    @Override
    public void startBackupFacebookPhoto(String task, HashMap<String, HashSet<String>> mediaUrlMap) {
        startBackupServiceFb(task, mediaUrlMap);
    }

    @Override
    public void startBackupInstagramPhoto(String task, HashMap<String, String> mediaUrlMap) {
        startBackupServiceInsta(task, mediaUrlMap);
    }

    @Override
    public void onSelectedInstagramPhoto(String task, HashMap<String, String> mediaUrlMap) {
        startBackupServiceInsta(task, mediaUrlMap);
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
    public void onUsbConnected() {
        showSnackBar(getString(R.string.usb_connected), ContextCompat.getColor(this, R.color.green));
    }

    @Override
    public void onUsbConnecting() {
        showSnackBar(getString(R.string.connecting_to_usb), ContextCompat.getColor(this, R.color.colorAccent));
    }

    @Override
    public void onProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAlert(String dataSource) {
        instagramLoadingAlert(dataSource);
    }

    @Override
    public void offAlert() {
        facebookRetrievingAlertDialog.dismiss();
    }

    private void instagramLoadingAlert(final String dataSource) {
        if (facebookRetrievingAlertDialog == null || !facebookRetrievingAlertDialog.isShowing()) {
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            final View facebookAlertView = layoutInflater.inflate(R.layout.facebook_loading_alert_layout, null);
            final TextView progressTitle = facebookAlertView.findViewById(R.id.progress_title_text);

            switch (dataSource) {
                case INSTAGRAM:
                    progressTitle.setText(getString(R.string.instagram_data_retrieving_text));
                    break;
                case FACEBOOK:
                    progressTitle.setText(getString(R.string.fb_data_retrieving_text));
                    break;
                default:
                    break;
            }

            facebookRetrievingAlertDialog = new AlertDialog.Builder(this).create();
            facebookRetrievingAlertDialog.setView(facebookAlertView);

            facebookRetrievingAlertDialog.show();
        }
    }

    @Override
    public void offProgress() {
        progressBar.setVisibility(View.GONE);
    }

    private void showFbDownloadOptions(HashMap<String, HashSet<String>> mediaUrlMap) {
        final View downloadOptionLayout = getLayoutInflater().inflate(R.layout.download_option_layout, null);
        downloadOptionLayout.findViewById(R.id.download_all_btn).setOnClickListener(v -> {
            startBackupFacebookPhoto(TASK_BACKUP_FACEBOOK_PHOTO, mediaUrlMap);
            alertDialog.dismiss();
        });
        downloadOptionLayout.findViewById(R.id.select_photos_btn).setOnClickListener(v -> {
            showFacebookImageGallery(mediaUrlMap);
            alertDialog.dismiss();
        });

        alertDialog = new AlertDialog.Builder(this).setView(downloadOptionLayout).create();
        alertDialog.show();
    }

    private void showInstagramDownloadOptions(HashMap<String, String> mediaUrlMap) {
        final View downloadOptionLayout = getLayoutInflater().inflate(R.layout.download_option_layout, null);
        ((TextView) downloadOptionLayout.findViewById(R.id.title)).setText(R.string.instagram_backup_text);
        downloadOptionLayout.findViewById(R.id.download_all_btn).setOnClickListener(v -> {
            startBackupInstagramPhoto(TASK_BACKUP_INSTAGRAM_PHOTO, mediaUrlMap);
            alertDialog.dismiss();
        });
        downloadOptionLayout.findViewById(R.id.select_photos_btn).setOnClickListener(v -> {
            showInstagramImageGallery(mediaUrlMap);
            alertDialog.dismiss();
        });

        alertDialog = new AlertDialog.Builder(this).setView(downloadOptionLayout).create();
        alertDialog.show();
    }

    private void showInstagramImageGallery(HashMap<String, String> mediaUrlMap) {
        galleryLayout.setVisibility(View.VISIBLE);
        final FacebookGalleryAdapter facebookGalleryAdapter = new FacebookGalleryAdapter(this, mediaUrlMap, getWindowWidth());
        imageGalleryRecyclerview.setAdapter(facebookGalleryAdapter);
        galleryToolbar.findViewById(R.id.done_text).setOnClickListener(view -> {
            final HashMap<String, String> newInstagramUrlMap = facebookGalleryAdapter.getNewInstagramUrlMap();
            if (newInstagramUrlMap != null && !newInstagramUrlMap.isEmpty()) {
                startBackupInstagramPhoto(TASK_BACKUP_SELECTED_INSTAGRAM_PHOTO, newInstagramUrlMap);
            }
            galleryLayout.setVisibility(View.GONE);
        });
    }

    private void showFacebookImageGallery(HashMap<String, HashSet<String>> mediaUrlMap) {
        galleryLayout.setVisibility(View.VISIBLE);
        final HashMap<String, String> mapFacebookUrlCollection = getMapFacebookUrlCollection(mediaUrlMap);
        final FacebookGalleryAdapter facebookGalleryAdapter = new FacebookGalleryAdapter(this, mapFacebookUrlCollection, getWindowWidth());
        imageGalleryRecyclerview.setAdapter(facebookGalleryAdapter);
        galleryToolbar.findViewById(R.id.done_text).setOnClickListener(view -> {
            final HashMap<String, String> newFacebookUrlMap = facebookGalleryAdapter.getNewInstagramUrlMap();
            if (newFacebookUrlMap != null && !newFacebookUrlMap.isEmpty()) {
                startBackupInstagramPhoto(TASK_BACKUP_SELECTED_FACEBOOK_PHOTO, newFacebookUrlMap);
            }
            galleryLayout.setVisibility(View.GONE);
        });
    }

    private HashMap<String, String> getMapFacebookUrlCollection(HashMap<String, HashSet<String>> mediaUrlMap) {
        final HashMap<String, String> facebookUrlCollection = new HashMap<>();
        for (String key : mediaUrlMap.keySet()) {
            final HashSet<String> urlSet = mediaUrlMap.get(key);
            if (urlSet != null && !urlSet.isEmpty()) {
                for (String url : urlSet) {
                    facebookUrlCollection.put(getIdFromUrl(url), url);
                }
            }
        }
        return facebookUrlCollection;
    }

    private int getWindowWidth() {
        final Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);

        return size.x;
    }

    @Override
    public void onDismissDialog() {
        mMainDialogFragment.dismiss();
    }

    @Override
    public void onStoragePermissionClicked() {
//      Do nothing
    }

    @Override
    public void onCameraPermissionClicked() {
//      Do nothing
    }

    @Override
    public void onContactPermissionClicked() {
//      Do nothing
    }
}

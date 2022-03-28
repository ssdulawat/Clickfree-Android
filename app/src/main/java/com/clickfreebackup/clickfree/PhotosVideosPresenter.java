package com.clickfreebackup.clickfree;

import static com.clickfreebackup.clickfree.BaseActivity.USB_RETRY_DELAY;
import static com.clickfreebackup.clickfree.PhotosVideosListActivity.FACEBOOK_USER_ACCESS_TOKEN;
import static com.clickfreebackup.clickfree.PhotosVideosListActivity.FACEBOOK_USER_ID;

import android.hardware.usb.UsbDevice;
import android.net.Uri;

import androidx.core.content.ContextCompat;

import com.clickfreebackup.clickfree.model.InstagramMediaItem;
import com.clickfreebackup.clickfree.repository.FacebookDataListener;
import com.clickfreebackup.clickfree.repository.FacebookRepositoryImpl;
import com.clickfreebackup.clickfree.repository.InstagramRepositiryImpl;
import com.clickfreebackup.clickfree.usb.UsbFileParams;
import com.clickfreebackup.clickfree.usb.UsbOtgManager;
import com.clickfreebackup.clickfree.util.SharedPreferencesManager;
import com.facebook.AccessToken;
import com.jaiselrahman.filepicker.model.MediaFile;

import org.reactivestreams.Subscription;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

public class PhotosVideosPresenter {
    private static final String TAG = "PhotosVideosPresenter";
    private final UsbOtgManager usbOtgManager = UsbOtgManager.getInstance();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final PublishSubject<Boolean> isUsbManagerReady = PublishSubject.create();
    private final ArrayList<UsbFileParams> selectedMediaFiles = new ArrayList<>();

    PhotosVideosPresenter(final FacebookDataListener facebookDataListener,
                          final FacebookProgressListener facebookProgressListener,
                          final SharedPreferencesManager sharedPreferences) {
        facebookRepository = new FacebookRepositoryImpl(facebookDataListener, facebookProgressListener);
        instagramRepository = new InstagramRepositiryImpl(sharedPreferences);
        listenUsbManager();
    }

    private FacebookRepositoryImpl facebookRepository;

    private InstagramRepositiryImpl instagramRepository;

    private PhotosVideosView view;

    void attachView(PhotosVideosView view) {
        this.view = view;
    }

    void cleanUp() {
        compositeDisposable.dispose();
        usbOtgManager.disconnect();
    }

    void detachView() {
        view = null;
    }

    private boolean verifyConnection() {
        boolean isConnected = usbOtgManager.isConnected();

        if (!isConnected) view.showToast(App.getContext().getString(R.string.error_usb_connect));
        return isConnected;
    }

    public void onDeviceDetached(UsbDevice device) {
        usbOtgManager.disconnect();
    }

    public void setupDevice() {
        isUsbManagerReady.onNext(false);
        compositeDisposable.add(usbOtgManager.setupDevice()
                .delaySubscription(USB_RETRY_DELAY, TimeUnit.MILLISECONDS)
                .doOnSubscribe(subscription -> view.onUsbConnecting())
                .subscribe(result -> {
                    Timber.d("USB device is ready");
                    isUsbManagerReady.onNext(true);
                    view.onUsbConnected();
                }, throwable -> {
                    Timber.e(throwable);
                    isUsbManagerReady.onNext(false);
                }));
    }

    public void onSelectPhotosClicked() {
        if (verifyConnection()) {
            view.showImagePicker();
        }
    }

    public void onProgressReceived(ProgressParams progressParams) {
        if (!progressParams.isShowDialog()) {
            view.hideProgressDialog();
        } else {
            view.showProgressDialog(progressParams);
        }
    }

    public void onBackupAllMediaFilesClicked() {
        if (!verifyConnection()) return;

        view.startBackupAllPhotoVideoTask();
    }

    void onBackupInstagramPhoto(List<InstagramMediaItem> instagramMediaItems) {
        if (!verifyConnection()) return;

        final HashMap<String, String> mediaUrlMap = new HashMap<>();
        for (InstagramMediaItem instagramMediaItem : instagramMediaItems) {
            mediaUrlMap.put(instagramMediaItem.getTimestamp(), instagramMediaItem.getMediaUrl());
        }
        view.onBackupInstagramPhoto(mediaUrlMap);
    }

    void onHandleFacebookMediaSources(HashMap<String, HashSet<String>> facebookMediaSources) {
        view.onBackupFacebookPhoto(facebookMediaSources);
    }

    void onFacebookLogOut() {
        facebookRepository.facebookLogOut(compositeDisposable);
    }

    void onSelectedMediaFilesReceived(List<MediaFile> files) {
        if (files.isEmpty()) return;

        for (MediaFile mediaFile : files) {
            Uri uri = Uri.fromFile(new File(mediaFile.getPath()));
            selectedMediaFiles.add(new UsbFileParams(uri, ContentType.SELECTED_MEDIA));
        }
    }

    HashMap<String, String> getFacebookUserIdentifiers() {
        final AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
        final HashMap<String, String> userData = new HashMap<>();

        if (currentAccessToken != null && currentAccessToken.getToken() != null && !currentAccessToken.getToken().isEmpty()) {
            userData.put(FACEBOOK_USER_ACCESS_TOKEN, currentAccessToken.getToken());
            userData.put(FACEBOOK_USER_ID, currentAccessToken.getUserId());
        }

        return userData;
    }

    void getFacebookUserName(String userId) {
        facebookRepository.getUserName(userId);
    }

    void getFacebookPhotos(String accessToken, String userId) {
        facebookRepository.getFacebookPhotos(accessToken, userId);
    }

    void setInstagramListeners(final InstagramDataListener instagramListeners, final FacebookProgressListener facebookProgressListener) {
        instagramRepository.setFacebookProgressListener(facebookProgressListener);
        instagramRepository.setInstagramDataListener(instagramListeners);
    }

    void callInstagramUsername() {
        instagramRepository.callUsername();
    }

    void onInstagramLogOut() {
        instagramRepository.onInstagramLogOut();
    }

    void callUserToken(final String code, final String instagramAppId, final String instagramAppSecret) {
        instagramRepository.callUserToken(code, instagramAppId, instagramAppSecret);
    }

    void onUserMedia() {
        instagramRepository.onUserMedia();
    }

    void setUsbManagerNotReady() {
        isUsbManagerReady.onNext(false);
        usbOtgManager.disconnect();
    }

    void clearSelectedFiles() {
        selectedMediaFiles.clear();
    }

    void listenUsbManager() {
        compositeDisposable.add(isUsbManagerReady
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if(result && !selectedMediaFiles.isEmpty()){
                        view.startSaveSelectedFilesTask(selectedMediaFiles);
                    }
                }, Timber::e));
    }
}

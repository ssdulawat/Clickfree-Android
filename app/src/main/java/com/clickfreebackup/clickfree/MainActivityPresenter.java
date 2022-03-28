package com.clickfreebackup.clickfree;

import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Handler;

import com.clickfreebackup.clickfree.model.ContactBody;
import com.clickfreebackup.clickfree.repository.SendGridRepository;
import com.clickfreebackup.clickfree.repository.SendGridRepositoryImpl;
import com.clickfreebackup.clickfree.repository.SendGridRepositoryListener;
import com.clickfreebackup.clickfree.usb.UsbFileParams;
import com.clickfreebackup.clickfree.usb.UsbOtgManager;
import com.clickfreebackup.clickfree.util.RetryWithDelay;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.clickfreebackup.clickfree.BaseActivity.USB_RETRY_DELAY;

class MainActivityPresenter {
    private static final String TAG = "MainActivityPresenter";
    private UsbOtgManager usbOtgManager = UsbOtgManager.getInstance();
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private MainView view;
    private SendGridRepository mSendGridRepository;

    MainActivityPresenter() {
        mSendGridRepository = new SendGridRepositoryImpl();
    }

    void attachView(MainView view) {
        this.view = view;
    }

    void cleanUp() {
        compositeDisposable.dispose();
    }

    void detachView() {
        view = null;
    }

    private boolean verifyConnection() {
        boolean isConnected = usbOtgManager.isConnected();

        if (!isConnected) view.showToast(App.getContext().getString(R.string.error_usb_connect));
        return isConnected;
    }

    public void onProgressReceived(ProgressParams progressParams) {
        if (!progressParams.isShowDialog()) {
            view.hideProgressDialog();
        } else {
            view.showProgressDialog(progressParams);
        }
    }

    void onBackupContactsClicked() {
        view.startBackupContactsTask();
    }

    void onBackupEverythingClicked() {
        view.startBackupEverythingTask();
    }

    public void onDeviceDetached(UsbDevice device) {
        usbOtgManager.disconnect();
    }

    public void setupDevice() {
        compositeDisposable.add(usbOtgManager.setupDevice()
                .delaySubscription(USB_RETRY_DELAY, TimeUnit.MILLISECONDS)
                .subscribe(result -> {
                    if (result) {
                        view.onUsbConnected();
                    }
                    Timber.d("USB device is ready");
                }, Timber::e));
    }

    public void onPhotoTaken(Uri uri) {
        if (!verifyConnection()) return;

        File file = new File(uri.getPath());
        UsbFileParams params = new UsbFileParams(uri, ContentType.CAMERA_ROLL);

        new Handler().postDelayed(() -> {
            view.showProgressDialog(App.getContext().getString(R.string.saving_photo), null, true);
        }, 200);

        compositeDisposable.add(
                UsbOtgManager.getInstance().writeToUsb(params, null)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .delaySubscription(1000, TimeUnit.MILLISECONDS) // FIXME: Delay as a workaround to let the app connect to USB storage before saving the image. Is there a better way?
                        .doOnSuccess((isCopySuccessful) -> {
                            if (isCopySuccessful && file.exists()) file.delete();
                        })
                        .doOnSuccess(isSuccessful -> {
                            if (!isSuccessful)
                                throw new IllegalStateException("Writing photo/video has failed.");
                        })
                        .retryWhen(new RetryWithDelay(1, 1000))
                        .subscribe(
                                (copySuccessful) -> {
                                    view.hideProgressDialog();
                                    if (copySuccessful)
                                        view.showInfoPopup(App.getContext().getString(R.string.media_file_saved), App.getContext().getString(R.string.photo_saved_desc));
                                    else view.showToast("Saving photo/video failed.");
                                },
                                (error) -> {
                                    view.hideProgressDialog();
                                    Timber.e(error, "Saving photo/video failed.");
                                    view.showToast("Saving photo/video failed.");
                                })
        );
    }

    void onPhotosTaken(final ArrayList<Uri> photoUris) {
        if (!verifyConnection() || photoUris == null || photoUris.isEmpty()) return;

        view.startSaveCameraPhotoFilesTask(convertToUsbFileParams(photoUris));
    }

    private ArrayList<UsbFileParams> convertToUsbFileParams(List<Uri> photoUris) {
        final ArrayList<UsbFileParams> fileParamsList = new ArrayList<>();
        for (Uri photoUri : photoUris) {
            fileParamsList.add(new UsbFileParams(photoUri, ContentType.CAMERA_ROLL));
        }
        return fileParamsList;
    }

    public void onFormatButtonClicked() {
        if (!verifyConnection()) return;

        view.startClearStorageTask();
    }

    public void onCameraClicked() {
        if (verifyConnection()) {
            view.showPhotoVideoSelectionPopup();
        }
    }

    public void onStorageClicked() {
        if (verifyConnection()) {
            view.openStorage();
        }
    }

    public void onBackupPhotoVideoClicked() {
        if (verifyConnection()) {
            view.openPhotoVideoBackup();
        }
    }

    void onMakeVideoClicked() {
        view.launchVideoCamera();
    }

    void onTakePhotoClicked() {
        view.launchPhotoCamera();
    }

    void onSendEmailButtonClicked(final ContactBody contactBody, final SendGridRepositoryListener sendGridRepositoryListener) {
        mSendGridRepository.sendEmail(contactBody, sendGridRepositoryListener);
    }
}


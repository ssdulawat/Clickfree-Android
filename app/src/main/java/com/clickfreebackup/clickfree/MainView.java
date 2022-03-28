package com.clickfreebackup.clickfree;

import com.clickfreebackup.clickfree.usb.UsbFileParams;

import java.util.ArrayList;

public interface MainView {

    void showToast(String message);

    void showProgressDialog(String title, String text, boolean showCancelButton);

    void showProgressDialog(ProgressParams progressParams);

    void setProgressDialogText(String title, String text);

    void hideProgressDialog();

    void launchPhotoCamera();

    void setCurrentProgress(int progress);

    void openStorage();

    void openPhotoVideoBackup();

    void setMaxProgress(int maxProgress);

    void showInfoPopup(String title, String text);

    void startBackupContactsTask();

    void startBackupEverythingTask();

    void startClearStorageTask();

    void launchVideoCamera();

    void showPhotoVideoSelectionPopup();

    void startSaveCameraPhotoFilesTask(ArrayList<UsbFileParams> fileParams);

    void onUsbConnected();
}

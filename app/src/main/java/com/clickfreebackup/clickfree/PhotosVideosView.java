package com.clickfreebackup.clickfree;

import com.clickfreebackup.clickfree.usb.UsbFileParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public interface PhotosVideosView {
    void showToast(String message);

    void showProgressDialog(String title, String text, boolean allowCancel);

    void showProgressDialog(ProgressParams progressParams);

    void hideProgressDialog();

    void setCurrentProgress(int progress);

    void setMaxProgress(int maxProgress);

    void showImagePicker();

    void setMaxProgressAbsolute(int maxProgressAbsolute);

    void showInfoPopup(String title, String text);

    void startBackupAllPhotoVideoTask();

    void startSaveSelectedFilesTask(ArrayList<UsbFileParams> fileParams);

    void onBackupFacebookPhoto(HashMap<String, HashSet<String>> mediaUrlMap);

    void onBackupInstagramPhoto(HashMap<String, String> mediaUrlMap);

    void startBackupFacebookPhoto(String task, HashMap<String, HashSet<String>> mediaUrlMap);

    void startBackupInstagramPhoto(String task, HashMap<String, String> mediaUrlMap);

    void onSelectedInstagramPhoto(String task, HashMap<String, String> mediaUrlMap);

    void onUsbConnected();

    void onUsbConnecting();
}

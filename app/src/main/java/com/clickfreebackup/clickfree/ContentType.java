package com.clickfreebackup.clickfree;

/**
 * Type of data to backup: Contacts (vcf) file, media file, etc.
 */
public enum ContentType {
    PHOTO_VIDEO("Photo_And_Video_Backups"), CONTACTS("Contacts"),
    CAMERA_ROLL("ClickFree_Camera_Roll"), SELECTED_MEDIA("Selected_Photo_And_Video"),
    INSTAGRAM_PHOTO("Instagram_Photo_Backups"), FACEBOOK_PHOTO("Facebook_Photo_Backups"),
    SELECTED_INSTAGRAM_PHOTO("Selected_Instagram_Photo"), SELECTED_FACEBOOK_PHOTO("Selected_Facebook_Photo");

    /**
     * Name of a directory on the USB storage where the files of the corresponding type are stored.
     */
    public final String dirName;

    ContentType(String dirName) {
        this.dirName = dirName;
    }
}

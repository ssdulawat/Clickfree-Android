package com.clickfreebackup.clickfree.usb;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.clickfreebackup.clickfree.ContentType;

/**
 * Meta data required for writing a file to USB storage in host mode.
 */
public class UsbFileParams implements Parcelable {
    private String id = "";
    private String folder = "";
    private final Uri uri;
    private final ContentType contentType;

    public UsbFileParams(String id, Uri uri, ContentType contentType) {
        this.uri = uri;
        this.contentType = contentType;
        this.id = id;
    }

    public UsbFileParams(String folder, String id, Uri uri, ContentType contentType) {
        this.uri = uri;
        this.contentType = contentType;
        this.id = id;
        this.folder = folder;
    }

    public UsbFileParams(Uri uri, ContentType contentType) {
        this.uri = uri;
        this.contentType = contentType;
    }

    public String getId() {
        return id;
    }

    public String getFolder() {
        return folder;
    }

    public Uri getUri() {
        return uri;
    }

    public ContentType getContentType() {
        return contentType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.uri, flags);
        dest.writeInt(this.contentType == null ? -1 : this.contentType.ordinal());
        dest.writeString(this.id == null ? "" : this.id);
        dest.writeString(this.folder == null ? "" : this.folder);
    }

    protected UsbFileParams(Parcel in) {
        this.uri = in.readParcelable(Uri.class.getClassLoader());
        int tmpContentType = in.readInt();
        this.contentType = tmpContentType == -1 ? null : ContentType.values()[tmpContentType];
        this.id = in.readString();
        this.folder = in.readString();
    }

    public static final Parcelable.Creator<UsbFileParams> CREATOR = new Parcelable.Creator<UsbFileParams>() {
        @Override
        public UsbFileParams createFromParcel(Parcel source) {
            return new UsbFileParams(source);
        }

        @Override
        public UsbFileParams[] newArray(int size) {
            return new UsbFileParams[size];
        }
    };
}

package com.clickfreebackup.clickfree;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * Information concerning progress of completion of a certain long-term operation.
 */
public class ProgressParams implements Parcelable {
    private String title;
    private String text;
    private int maxProgress = -1;
    private int currentProgress = -1;
    private boolean show;
    private boolean isProgressPercents = true;
    private boolean allowCancel = true;
    public static final int STOPPING_PROCCESS = -100;

    public ProgressParams(String title, String text, int maxProgress, int currentProgress, boolean show, boolean isProgressPercents) {
        this.title = title;
        this.text = text;
        this.maxProgress = maxProgress;
        this.currentProgress = currentProgress;
        this.show = show;
        this.isProgressPercents = isProgressPercents;
    }

    public boolean isProgressPercents() {
        return isProgressPercents;
    }

    public void setProgressPercents(boolean progressPercents) {
        isProgressPercents = progressPercents;
    }

    public boolean hasProgress() {
        return currentProgress >= 0;
    }

    public static ProgressParams setText(String title, String text) {
        return new ProgressParams(title, text, -1, -1, true, false);
    }

    public static ProgressParams hide() {
        return new ProgressParams(null, null, -1, -1, false, false);
    }

    public static ProgressParams stopping() {
        return new ProgressParams(null, null, -1, STOPPING_PROCCESS, true, false);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public void setMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;
    }

    public boolean isShowDialog() {
        return show;
    }

    public boolean isStopping() {
        return currentProgress == STOPPING_PROCCESS;
    }

    public void setShow(boolean show) {
        this.show = show;
    }

    @NonNull
    @Override
    public String toString() {
        if (!isShowDialog()) return "Progress: [ " + title + " ] ; [ " + text + " ] ::: hide";
        else return "Progress: [ " + title + " ] ; [ " + text + " ] " + "Max progress: " + maxProgress + ", current progress: " + currentProgress;
    }

    public boolean isAllowCancel() {
        return allowCancel;
    }

    public void setAllowCancel(boolean allowCancel) {
        this.allowCancel = allowCancel;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.title);
        dest.writeString(this.text);
        dest.writeInt(this.maxProgress);
        dest.writeInt(this.currentProgress);
        dest.writeByte(this.show ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isProgressPercents ? (byte) 1 : (byte) 0);
        dest.writeByte(this.allowCancel ? (byte) 1 : (byte) 0);
    }

    protected ProgressParams(Parcel in) {
        this.title = in.readString();
        this.text = in.readString();
        this.maxProgress = in.readInt();
        this.currentProgress = in.readInt();
        this.show = in.readByte() != 0;
        this.isProgressPercents = in.readByte() != 0;
        this.allowCancel = in.readByte() != 0;
    }

    public static final Creator<ProgressParams> CREATOR = new Creator<ProgressParams>() {
        @Override
        public ProgressParams createFromParcel(Parcel source) {
            return new ProgressParams(source);
        }

        @Override
        public ProgressParams[] newArray(int size) {
            return new ProgressParams[size];
        }
    };
}

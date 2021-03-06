package com.clickfreebackup.clickfree.model;

import com.google.gson.annotations.SerializedName;

public class Cursors {
    @SerializedName("before")
    private String before;
    @SerializedName("after")
    private String after;

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }
}

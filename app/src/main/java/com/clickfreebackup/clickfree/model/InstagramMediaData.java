package com.clickfreebackup.clickfree.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class InstagramMediaData {
    @SerializedName("data")
    private List<InstagramMediaItem> data;
    @SerializedName("paging")
    private Paging paging;

    public List<InstagramMediaItem> getData() {
        return data;
    }

    public void setData(List<InstagramMediaItem> data) {
        this.data = data;
    }

    public Paging getPaging() {
        return paging;
    }
}

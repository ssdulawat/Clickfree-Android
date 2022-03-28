package com.clickfreebackup.clickfree.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FacebookMediaData {
    @SerializedName("data")
    @Expose
    private List<FacebookMediaItem> data = null;
    @SerializedName("paging")
    @Expose
    private Paging paging;

    public List<FacebookMediaItem> getData() {
        return data;
    }

    public void setData(List<FacebookMediaItem> data) {
        this.data = data;
    }

    public Paging getPaging() {
        return paging;
    }

    public void setPaging(Paging paging) {
        this.paging = paging;
    }
}


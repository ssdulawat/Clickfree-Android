package com.clickfreebackup.clickfree.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FacebookMediaItem {
    @SerializedName("images")
    @Expose
    private List<FacebookImageItem> images = null;
    @SerializedName("id")
    @Expose
    private String id;

    public List<FacebookImageItem> getImages() {
        return images;
    }

    public void setImages(List<FacebookImageItem> images) {
        this.images = images;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

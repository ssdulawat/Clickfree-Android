package com.clickfreebackup.clickfree.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class InstagramMediaItem {

    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("media_url")
    @Expose
    private String mediaUrl;
    @SerializedName("timestamp")
    @Expose
    private String timestamp;

    public InstagramMediaItem(String id, String mediaUrl, String timestamp) {
        this.id = id;
        this.mediaUrl = mediaUrl;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public String getTimestamp() {
        return timestamp;
    }
}

package com.clickfreebackup.clickfree.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class FacebookImageItem {
    @SerializedName("height")
    @Expose
    private Integer height;
    @SerializedName("source")
    @Expose
    private String source;
    @SerializedName("width")
    @Expose
    private Integer width;

    public FacebookImageItem(Integer height, String source, Integer width) {
        this.height = height;
        this.source = source;
        this.width = width;
    }

    public String getSource() {
        return source;
    }
}

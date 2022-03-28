package com.clickfreebackup.clickfree.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ErrorBody {
    @SerializedName("message")
    @Expose
    private String message;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("code")
    @Expose
    private Integer code;
    @SerializedName("fbtrace_id")
    @Expose
    private String fbtraceId;

    public Integer getCode() {
        return code;
    }
}

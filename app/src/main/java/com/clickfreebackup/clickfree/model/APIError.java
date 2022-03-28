package com.clickfreebackup.clickfree.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class APIError {
    @SerializedName("error")
    @Expose
    private ErrorBody errorBody;

    public ErrorBody getError() {
        return errorBody;
    }
}

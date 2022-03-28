package com.clickfreebackup.clickfree.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class UserToken {

    @SerializedName("access_token")
    @Expose
    private String token;

    @SerializedName("user_id")
    @Expose
    private long UserId;

    public UserToken(String token, long userId) {
        this.token = token;
        UserId = userId;
    }

    public String getToken() {
        return token;
    }

    public long getUserId() {
        return UserId;
    }
}

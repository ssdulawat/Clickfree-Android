package com.clickfreebackup.clickfree.model;

import com.google.gson.annotations.SerializedName;

public class UserData {

    @SerializedName("username")
    private String username;
    @SerializedName("id")
    private String userId;

    public UserData(final String username, final String userId) {
        this.username = username;
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }
}

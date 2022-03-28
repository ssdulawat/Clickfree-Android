package com.clickfreebackup.clickfree.model;

import com.google.gson.annotations.SerializedName;

public class Contact {

    @SerializedName("email")
    private String email;

    public Contact(String email) {
        this.email = email;
    }

    @SerializedName("email")
    public String getEmail() {
        return email;
    }

    @SerializedName("email")
    public void setEmail(String email) {
        this.email = email;
    }
}

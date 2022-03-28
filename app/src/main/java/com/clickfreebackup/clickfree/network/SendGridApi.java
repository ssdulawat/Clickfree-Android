package com.clickfreebackup.clickfree.network;

import com.clickfreebackup.clickfree.model.ContactBody;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.PUT;

public interface SendGridApi {
    @PUT("marketing/contacts")
    @Headers({"Content-Type: application/json"})
    Call<Void> saveEmail(@Body ContactBody contactBody);
}

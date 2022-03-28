package com.clickfreebackup.clickfree.network;

import com.clickfreebackup.clickfree.model.UserToken;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface AuthInstagramApi {

    @FormUrlEncoded
    @POST("oauth/access_token")
    Call<UserToken> getUserToken(
            @Field("app_id") String appId,
            @Field("app_secret") String appSecret,
            @Field("grant_type") String grandType,
            @Field("redirect_uri") String redirectUri,
            @Field("code") String code
    );
}

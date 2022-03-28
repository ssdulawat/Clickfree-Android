package com.clickfreebackup.clickfree.network;

import com.clickfreebackup.clickfree.model.InstagramMediaData;
import com.clickfreebackup.clickfree.model.UserData;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MediaInstagramApi {
    @GET("me/media")
    Call<InstagramMediaData> getUserPhotos(@Query("fields") String fields, @Query("access_token") String token);

    @GET("v1.0/{id}/media")
    Call<InstagramMediaData> getNextCollection(@Path("id") String id,
                                               @Query("access_token") String token,
                                               @Query("fields") String fields,
                                               @Query("limit") String limit,
                                               @Query("after") String after);

    @GET("{id}")
    Call<UserData> getUserData(@Path("id") String id,
                               @Query("fields") String fields,
                               @Query("access_token") String accessToken);
}

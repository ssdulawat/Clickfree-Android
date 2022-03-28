package com.clickfreebackup.clickfree.network;

import com.clickfreebackup.clickfree.model.FacebookMediaData;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FacebookApi {
    @GET("{user_id}/photos")
    Call<FacebookMediaData> getUserToken(@Path("user_id") String userId,
                                         @Query("fields") String fields,
                                         @Query("access_token") String token);

    @GET("v5.0/{id}/photos")
    Call<FacebookMediaData> getNextCollection(@Path("id") String id,
                                              @Query("access_token") String token,
                                              @Query("fields") String fields,
                                              @Query("limit") String limit,
                                              @Query("after") String after);
}

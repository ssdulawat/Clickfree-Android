package com.clickfreebackup.clickfree.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FacebookService {

    public final static String FACEBOOK_API_BASE_URL = "https://graph.facebook.com/";
    private static FacebookService mInstance;
    private Retrofit mRetrofitFacebook;

    private FacebookService() {

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();

        mRetrofitFacebook = new Retrofit.Builder()
                .baseUrl(FACEBOOK_API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static FacebookService getInstance() {
        if (mInstance == null) {
            mInstance = new FacebookService();
        }
        return mInstance;
    }

    public FacebookApi getFacebookApi() {
        return mRetrofitFacebook.create(FacebookApi.class);
    }
}

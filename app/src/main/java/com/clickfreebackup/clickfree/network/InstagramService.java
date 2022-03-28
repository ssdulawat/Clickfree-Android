package com.clickfreebackup.clickfree.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class InstagramService {

    public final static String INSTAGRAM_AUTH_API_BASE_URL = "https://api.instagram.com/";
    public final static String INSTAGRAM_MEDIA_API_BASE_URL = "https://graph.instagram.com/";
    private static InstagramService mInstance;
    private Retrofit mRetrofitAuthInstagram;
    private Retrofit mRetrofitMediaInstagram;

    private InstagramService() {

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        final GsonConverterFactory gsonConverterFactory = GsonConverterFactory.create();

        mRetrofitAuthInstagram = new Retrofit.Builder()
                .baseUrl(INSTAGRAM_AUTH_API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(gsonConverterFactory)
                .build();

        mRetrofitMediaInstagram = new Retrofit.Builder()
                .baseUrl(INSTAGRAM_MEDIA_API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(gsonConverterFactory)
                .build();
    }

    public static InstagramService getInstance() {
        if (mInstance == null) {
            mInstance = new InstagramService();
        }
        return mInstance;
    }

    public AuthInstagramApi getAuthInstagramApi() {
        return mRetrofitAuthInstagram.create(AuthInstagramApi.class);
    }

    public MediaInstagramApi getMediaInstagramApi() {
        return mRetrofitMediaInstagram.create(MediaInstagramApi.class);
    }

    public Retrofit getRetrofitMediaInstagram() {
        return mRetrofitMediaInstagram;
    }
}

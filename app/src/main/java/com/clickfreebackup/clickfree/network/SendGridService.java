package com.clickfreebackup.clickfree.network;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SendGridService {
    private final String HEADER_API_KEY = "Authorization";
    private final String API_KEY = "Bearer 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";
    private static SendGridService mInstance;
    private Retrofit mRetrofitSendGrid;

    private SendGridService() {
        final String BASE_URL = "https://api.sendgrid.com/v3/";
        final HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.level(HttpLoggingInterceptor.Level.BODY);
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(httpLoggingInterceptor)
                .addInterceptor(chain -> {
                    final Request request = chain.request().newBuilder()
                            .header(HEADER_API_KEY, API_KEY)
                            .build();

                    return chain.proceed(request);
                })
                .build();
        final GsonConverterFactory gsonConverterFactory = GsonConverterFactory.create();

        mRetrofitSendGrid = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(gsonConverterFactory)
                .client(okHttpClient)
                .build();
    }

    public static SendGridService getInstance() {
        if (mInstance == null) {
            mInstance = new SendGridService();
        }
        return mInstance;
    }

    public SendGridApi getSendGridApi() {
        return mRetrofitSendGrid.create(SendGridApi.class);
    }
}

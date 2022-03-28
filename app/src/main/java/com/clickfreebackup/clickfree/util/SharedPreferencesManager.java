package com.clickfreebackup.clickfree.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class SharedPreferencesManager {
    private static final String MY_APP_PREFERENCES = "back_up_preferences";
    private static final String PREF_INSTAGRAM_USER_ID = "instagram_user_id";
    private static final String PREF_INSTAGRAM_ACCESS_TOKEN = "instagram_access_token";
    private static final String PREF_FIRST_START = "first_start";
    private static final String LAUNCH_COUNTER = "launch_counter";

    private SharedPreferences sharedPrefs;
    private static SharedPreferencesManager instance;

    private SharedPreferencesManager(Context context) {
        sharedPrefs = context.getApplicationContext().getSharedPreferences(MY_APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPreferencesManager getInstance(Context context) {
        if (instance == null)
            instance = new SharedPreferencesManager(context);

        return instance;
    }

    public String getInstagramUserId() {
        return sharedPrefs.getString(PREF_INSTAGRAM_USER_ID, "");
    }

    public void setInstagramUserId(final String instagramUserId) {
        sharedPrefs.edit().putString(PREF_INSTAGRAM_USER_ID, instagramUserId).apply();
    }

    public String getInstagramAccessToken() {
        return sharedPrefs.getString(PREF_INSTAGRAM_ACCESS_TOKEN, "");
    }

    public void setInstagramAccessToken(final String instagramAccessToken) {
        sharedPrefs.edit().putString(PREF_INSTAGRAM_ACCESS_TOKEN, instagramAccessToken).apply();
    }

    public boolean isFirstStart() {
        return sharedPrefs.getBoolean(PREF_FIRST_START, true);
    }

    public void setFirstStart(final boolean isFirstStart) {
        sharedPrefs.edit().putBoolean(PREF_FIRST_START, isFirstStart).apply();
    }

    public int getLaunchCounter() {
        return sharedPrefs.getInt(LAUNCH_COUNTER, 1);
    }

    public void setLaunchCounter(final int launchCounter) {
        sharedPrefs.edit().putInt(LAUNCH_COUNTER, launchCounter).apply();
    }
}

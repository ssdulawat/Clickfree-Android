package com.clickfreebackup.clickfree;

import android.app.Application;
import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import timber.log.Timber;

/**
 * Application copying files (photos, videos, contacts) from an Android device to an external USB storage in USB-host
 * mode.
 * Communication with the USB device is implemented using libaums library.
 * Long-term backup tasks are performed in a foreground service, using broadcasts to update UI (progress bars,
 * dialogs, etc).
 */
public class App extends Application {
  private static Context context;

  public static Context getContext() {
    return context;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    context = getApplicationContext();
    FirebaseApp.initializeApp(context);
    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
    Timber.plant(new Timber.DebugTree());
  }
}

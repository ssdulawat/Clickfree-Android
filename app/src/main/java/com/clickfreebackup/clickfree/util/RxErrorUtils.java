package com.clickfreebackup.clickfree.util;

import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import timber.log.Timber;

public class RxErrorUtils {

    public static void setErrorHandler() {
        RxJavaPlugins.setErrorHandler(throwable -> {
            if (throwable instanceof UndeliverableException) {
                Timber.d(throwable.getCause());
                return;
            }
        });
    }

}

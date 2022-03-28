package com.clickfreebackup.clickfree.util;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class RetryWithDelay implements Function<Flowable<? extends Throwable>, Flowable<?>> {
    private final int maxRetries;
    private final int retryDelayMillis;
    private int retryCount;

    public RetryWithDelay(final int maxRetries, final int retryDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.retryCount = 0;
    }

    @Override
    public Flowable<?> apply(final Flowable<? extends Throwable> attempts) {
        return attempts
                .flatMap((Function<Throwable, Flowable<?>>) throwable -> {
                    if (++retryCount < maxRetries) {
                        // When this Observable calls onNext, the original
                        // Observable will be retried (i.e. re-subscribed).
                        return Flowable.timer(retryDelayMillis, TimeUnit.MILLISECONDS, Schedulers.computation());
                    }

                    // Max retries hit. Just pass the error along.
                    return Flowable.error(throwable);
                });
    }
}

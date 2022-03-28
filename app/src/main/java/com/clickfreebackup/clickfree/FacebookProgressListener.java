package com.clickfreebackup.clickfree;

public interface FacebookProgressListener {
    void onProgress();

    void onAlert(final String dataSource);

    void offProgress();

    void offAlert();
}

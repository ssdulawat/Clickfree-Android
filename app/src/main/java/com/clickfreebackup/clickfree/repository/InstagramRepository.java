package com.clickfreebackup.clickfree.repository;

import com.clickfreebackup.clickfree.FacebookProgressListener;
import com.clickfreebackup.clickfree.InstagramDataListener;

public interface InstagramRepository {

    void callUsername();

    void callUserToken(final String code, final String instagramAppId, final String instagramAppSecret);

    void setInstagramDataListener(InstagramDataListener instagramDataListener);

    void setFacebookProgressListener(FacebookProgressListener facebookProgressListener);

    void onInstagramLogOut();

    void onUserMedia();
}

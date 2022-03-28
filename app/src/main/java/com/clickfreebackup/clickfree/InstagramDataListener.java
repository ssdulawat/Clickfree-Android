package com.clickfreebackup.clickfree;

import com.clickfreebackup.clickfree.model.InstagramMediaItem;
import com.clickfreebackup.clickfree.model.UserData;

import java.util.List;

public interface InstagramDataListener {

    void onInstagramMediaData(List<InstagramMediaItem> instagramMediaItems, ClearListener clearListener);

    void onInstagramUserLoggedAs(UserData userData);

    void onTokenExpired(boolean isTokenExpired);
}

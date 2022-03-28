package com.clickfreebackup.clickfree.repository;

import java.util.HashMap;
import java.util.HashSet;

import io.reactivex.annotations.Nullable;

public interface FacebookDataListener {
    void onFacebookMediaData(@Nullable HashMap<String, HashSet<String>> facebookMediaSources);

    void onFacebookUsername(@Nullable String username);
}

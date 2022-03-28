package com.clickfreebackup.clickfree.repository;

import io.reactivex.disposables.CompositeDisposable;

public interface FacebookRepository {

    void getUserName(String userId);

    void getFacebookPhotos(String accessToken, String userId);

    void facebookLogOut(CompositeDisposable disposable);
}

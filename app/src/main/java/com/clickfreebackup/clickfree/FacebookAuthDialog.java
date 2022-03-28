package com.clickfreebackup.clickfree;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.clickfreebackup.clickfree.repository.FacebookDataListener;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.util.Arrays;

import timber.log.Timber;

public class FacebookAuthDialog extends Fragment {
    private static final String READ_PERMISSION_EMAIL = "email";
    private static final String READ_PERMISSION_USER_PHOTOS = "user_photos";
    private CallbackManager callbackManager;
    private FacebookDataListener facebookDataListener;
    private String token;
    private boolean isLogged;
    private PhotosVideosPresenter photosVideosPresenter;

    public FacebookAuthDialog(PhotosVideosPresenter photosVideosPresenter, FacebookDataListener facebookDataListener, boolean isLogged) {
        this.facebookDataListener = facebookDataListener;
        this.photosVideosPresenter = photosVideosPresenter;
        this.isLogged = isLogged;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.facebook_auth_dialog, container, false);
        final LoginButton loginButton = view.findViewById(R.id.login_button);
        if (!isLogged) {
            loginButton.setReadPermissions(Arrays.asList(READ_PERMISSION_EMAIL, READ_PERMISSION_USER_PHOTOS));
            loginButton.setFragment(this);
            loginButton.setLoginBehavior(LoginBehavior.WEB_VIEW_ONLY);
            callbackManager = CallbackManager.Factory.create();
            loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    Timber.d("Login Result success");
                    final AccessToken accessToken = loginResult.getAccessToken();
                    if (accessToken != null) {
                        token = accessToken.getToken();
                        photosVideosPresenter.getFacebookPhotos(token, accessToken.getUserId());
                    }
                }

                @Override
                public void onCancel() {
                    facebookDataListener.onFacebookMediaData(null);
                    Timber.d("registerCallback onCancel");
                }

                @Override
                public void onError(FacebookException exception) {
                    Timber.d(exception);
                }
            });
            loginButton.performClick();
        } else {
            final AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
            token = currentAccessToken.getToken();
            photosVideosPresenter.getFacebookPhotos(token, currentAccessToken.getUserId());
        }
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        facebookDataListener = null;
        callbackManager = null;
    }
}

package com.clickfreebackup.clickfree.repository;

import com.clickfreebackup.clickfree.ClearListener;
import com.clickfreebackup.clickfree.FacebookProgressListener;
import com.clickfreebackup.clickfree.InstagramDataListener;
import com.clickfreebackup.clickfree.model.APIError;
import com.clickfreebackup.clickfree.model.Cursors;
import com.clickfreebackup.clickfree.model.InstagramMediaData;
import com.clickfreebackup.clickfree.model.InstagramMediaItem;
import com.clickfreebackup.clickfree.model.UserData;
import com.clickfreebackup.clickfree.model.UserToken;
import com.clickfreebackup.clickfree.network.InstagramService;
import com.clickfreebackup.clickfree.util.ErrorUtils;
import com.clickfreebackup.clickfree.util.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static com.clickfreebackup.clickfree.util.Const.INSTAGRAM;

public class InstagramRepositiryImpl implements ClearListener, InstagramRepository {
    final static String QUERY_LIMIT = "25";
    public final static String REDIRECTION_BASE_URL = "https://www.google.com/";
    private final static String INSTAGRAM_GRAND_TYPE_PARAMETER = "authorization_code";
    private final static String MEDIA_PROPERTIES = "id,media_url,timestamp";
    private static final String FIELDS_PARAM = "username";
    private InstagramDataListener instagramDataListener;
    private FacebookProgressListener facebookProgressListener;
    private SharedPreferencesManager sharedPreferences;
    private List<InstagramMediaItem> instagramMediaItems;
    private String afterParameter;
    private String token;

    public InstagramRepositiryImpl(SharedPreferencesManager sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    @Override
    public void callUsername() {
        final String instagramAccessToken = sharedPreferences.getInstagramAccessToken();
        final String instagramUserId = sharedPreferences.getInstagramUserId();

        facebookProgressListener.onProgress();

        if (!instagramAccessToken.isEmpty() && !instagramUserId.isEmpty()) {
            InstagramService.getInstance().getMediaInstagramApi().getUserData(instagramUserId, FIELDS_PARAM, instagramAccessToken).enqueue(new Callback<UserData>() {
                @Override
                public void onResponse(Call<UserData> call, Response<UserData> response) {
                    if (response.isSuccessful()) {
                        final UserData userData = response.body();
                        if (userData != null) {
                            onInstagramUserLoggedAs(userData);
                        }
                    } else {
                        if (getApiErrorCode(ErrorUtils.parseError(response)) == 190) {
                            facebookProgressListener.offProgress();
                            instagramDataListener.onTokenExpired(true);
                        }
                    }
                }

                @Override
                public void onFailure(Call<UserData> call, Throwable t) {
                    Timber.d(t);
                    onInstagramUserLoggedAs(null);
                }
            });
        } else {
            onInstagramUserLoggedAs(null);
        }
    }

    private int getApiErrorCode(APIError apiError) {
        int errorCode = 0;
        if (apiError != null && apiError.getError() != null) {
            errorCode = apiError.getError().getCode();
        }

        return errorCode;
    }

    private void onInstagramUserLoggedAs(final UserData userData) {
        facebookProgressListener.offProgress();
        instagramDataListener.onInstagramUserLoggedAs(userData);
    }

    @Override
    public void callUserToken(final String code, final String instagramAppId, final String instagramAppSecret) {
        Timber.d("Fetch user's token");

        facebookProgressListener.onProgress();

        InstagramService.getInstance().getAuthInstagramApi().getUserToken(instagramAppId, instagramAppSecret,
                INSTAGRAM_GRAND_TYPE_PARAMETER, REDIRECTION_BASE_URL, code).enqueue(new Callback<UserToken>() {
            @Override
            public void onResponse(Call<UserToken> call, Response<UserToken> response) {
                final UserToken body = response.body();
                if (body != null && !body.getToken().isEmpty()) {
                    token = body.getToken();
                    sharedPreferences.setInstagramAccessToken(token);
                    sharedPreferences.setInstagramUserId(String.valueOf(body.getUserId()));
                    callUserMedia(token);
                }
            }

            @Override
            public void onFailure(Call<UserToken> call, Throwable t) {
                Timber.d(t);
            }
        });
    }

    private void callUserMedia(String token) {
        Timber.d("Fetch user's media");

        facebookProgressListener.onAlert(INSTAGRAM);

        InstagramService.getInstance().getMediaInstagramApi().getUserPhotos(MEDIA_PROPERTIES, token).enqueue(new Callback<InstagramMediaData>() {
            @Override
            public void onResponse(Call<InstagramMediaData> call, Response<InstagramMediaData> response) {
                instagramMediaItems = new ArrayList<>();
                processingInstagramResponseData(response.body());
            }

            @Override
            public void onFailure(Call<InstagramMediaData> call, Throwable t) {
                Timber.d(t);
                facebookProgressListener.offAlert();
            }
        });
    }

    private void processingInstagramResponseData(final InstagramMediaData data) {
        if (data != null && !data.getData().isEmpty()) {
            instagramMediaItems.addAll(data.getData());
            setAfterParameter(data.getPaging().getCursors());
            onInstagramMediaData(data);
        }
    }

    private void setAfterParameter(Cursors cursors) {
        if (cursors != null) {
            afterParameter = cursors.getAfter();
        }
    }

    private void onInstagramMediaData(InstagramMediaData data) {
        if (data.getPaging().getNext() != null && !data.getPaging().getNext().isEmpty()) {
            fetchNextPhotoCollection(getId(data.getPaging().getNext()));
        } else {
            facebookProgressListener.offAlert();
            instagramDataListener.onInstagramMediaData(instagramMediaItems, InstagramRepositiryImpl.this);
        }
    }

    private void fetchNextPhotoCollection(String id) {
        InstagramService.getInstance().getMediaInstagramApi().getNextCollection(id, token, MEDIA_PROPERTIES, QUERY_LIMIT, afterParameter).enqueue(new Callback<InstagramMediaData>() {
            @Override
            public void onResponse(Call<InstagramMediaData> call, Response<InstagramMediaData> response) {
                processingInstagramResponseData(response.body());
            }

            @Override
            public void onFailure(Call<InstagramMediaData> call, Throwable t) {
                Timber.d(t);
                facebookProgressListener.offAlert();
            }
        });
    }

    private String getId(String url) {
        return url.split("v1.0")[1].split("/")[1];
    }

    @Override
    public void setInstagramDataListener(InstagramDataListener instagramDataListener) {
        this.instagramDataListener = instagramDataListener;
    }

    @Override
    public void setFacebookProgressListener(FacebookProgressListener facebookProgressListener) {
        this.facebookProgressListener = facebookProgressListener;
    }

    @Override
    public void onInstagramLogOut() {
        sharedPreferences.setInstagramUserId("");
        sharedPreferences.setInstagramAccessToken("");
    }

    @Override
    public void clearListeners() {
        instagramDataListener = null;
        facebookProgressListener = null;
    }

    @Override
    public void onUserMedia() {
        token = sharedPreferences.getInstagramAccessToken();
        callUserMedia(token);
    }
}

package com.clickfreebackup.clickfree.repository;

import android.os.Bundle;

import com.clickfreebackup.clickfree.FacebookProgressListener;
import com.clickfreebackup.clickfree.model.Cursors;
import com.clickfreebackup.clickfree.model.FacebookMediaData;
import com.clickfreebackup.clickfree.model.FacebookMediaItem;
import com.clickfreebackup.clickfree.network.FacebookService;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static com.clickfreebackup.clickfree.network.FacebookService.FACEBOOK_API_BASE_URL;
import static com.clickfreebackup.clickfree.repository.InstagramRepositiryImpl.QUERY_LIMIT;
import static com.clickfreebackup.clickfree.util.Const.FACEBOOK;

public class FacebookRepositoryImpl implements FacebookRepository {
    private static final String REQUEST_FIELD_NAME = "name";
    private static final String REQUEST_PARAM_FIELDS = "fields";
    private static final String REQUEST_FIELDS = "images";
    private static final String REQUEST_FIELD_ALBUMS = "albums";
    private static final String REQUEST_FIELD_PHOTOS = "photos";
    private static final String REQUEST_FIELD_SOURCE = "source";
    private static final String REQUEST_FIELD_DATA = "data";
    private static final String REQUEST_FIELD_ID = "id";
    private GraphRequest graphRequest;
    private FacebookDataListener facebookDataListener;
    private FacebookProgressListener facebookProgressListener;
    private final HashMap<String, HashSet<String>> facebookMediaSources = new HashMap<>();
    private final HashSet<String> urlImageSet = new HashSet<>();
    private String afterParameter;
    private String accessToken;

    public FacebookRepositoryImpl(FacebookDataListener facebookDataListener, FacebookProgressListener facebookProgressListener) {
        this.facebookDataListener = facebookDataListener;
        this.facebookProgressListener = facebookProgressListener;

        graphRequest = new GraphRequest();
    }

    @Override
    public void getUserName(String userId) {
        userId = userId + "/";
        final Bundle parameters = new Bundle();
        parameters.putString(REQUEST_PARAM_FIELDS, REQUEST_FIELD_NAME);

        graphRequest.setAccessToken(AccessToken.getCurrentAccessToken());
        graphRequest.setGraphPath(userId);
        graphRequest.setParameters(parameters);
        graphRequest.setHttpMethod(HttpMethod.GET);
        graphRequest.setCallback(this::processingNameResult);

        graphRequest.executeAsync();
    }

    @Override
    public void getFacebookPhotos(String accessToken, String userId) {
        this.accessToken = accessToken;
        facebookProgressListener.onAlert(FACEBOOK);
        getFacebookPhotosRequest(accessToken, userId);
    }

    @Override
    public void facebookLogOut(CompositeDisposable disposable) {
        Single.create((SingleOnSubscribe<String>) e -> LoginManager.getInstance().logOut())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Timber.d("single onSubscribe");
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(String o) {
                        Timber.d("Facebook log out - Success");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.d(e, "Facebook log out - APIError");
                    }
                });
    }

    private void processingNameResult(GraphResponse graphNameResponse) {
        String name = "";
        try {
            final JSONObject nameResponseJSONObject = graphNameResponse.getJSONObject();
            if (nameResponseJSONObject != null) {
                name = nameResponseJSONObject.getString(REQUEST_FIELD_NAME);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        facebookDataListener.onFacebookUsername(name);
    }

    private void getFacebookPhotosRequest(String accessToken, String userId) {
        FacebookService.getInstance().getFacebookApi().getUserToken(userId, REQUEST_FIELDS, accessToken).enqueue(new Callback<FacebookMediaData>() {
            @Override
            public void onResponse(Call<FacebookMediaData> call, Response<FacebookMediaData> response) {
                processingFacebookResponseData(response.body());
            }

            @Override
            public void onFailure(Call<FacebookMediaData> call, Throwable t) {
                facebookProgressListener.offAlert();
                facebookDataListener.onFacebookMediaData(null);
                Timber.d(t);
            }
        });
    }

    private void processingFacebookResponseData(final FacebookMediaData body) {
        if (body != null && body.getData() != null && !body.getData().isEmpty()) {
            for (FacebookMediaItem mediaItem : body.getData()) {
                urlImageSet.add(mediaItem.getImages().get(0).getSource());
            }
            setAfterParameter(body.getPaging().getCursors());
            onFacebookMediaData(body);
        }
    }

    private void setAfterParameter(Cursors cursors) {
        if (cursors != null) {
            afterParameter = cursors.getAfter();
        }
    }

    private void onFacebookMediaData(FacebookMediaData data) {
        if (data.getPaging().getNext() != null && !data.getPaging().getNext().isEmpty()) {
            fetchNextPhotoCollection(getId(data.getPaging().getNext()));
        } else {
            facebookMediaSources.put(REQUEST_FIELDS, urlImageSet);
            getAlbums();
        }
    }

    private void fetchNextPhotoCollection(String id) {
        FacebookService.getInstance().getFacebookApi().getNextCollection(id, accessToken, REQUEST_FIELDS, QUERY_LIMIT, afterParameter).enqueue(new Callback<FacebookMediaData>() {
            @Override
            public void onResponse(Call<FacebookMediaData> call, Response<FacebookMediaData> response) {
                processingFacebookResponseData(response.body());
            }

            @Override
            public void onFailure(Call<FacebookMediaData> call, Throwable t) {
                facebookProgressListener.offAlert();
                facebookDataListener.onFacebookMediaData(null);
                Timber.d(t);
            }
        });
    }

    private String getId(String url) {
        return url.split(FACEBOOK_API_BASE_URL)[1].split("/")[1];
    }

    private void getAlbums() {
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/" + AccessToken.getCurrentAccessToken().getUserId() + "/" + REQUEST_FIELD_ALBUMS,
                null,
                HttpMethod.GET,
                response -> {
                    Timber.d(response.toString());
                    try {
                        if (response.getError() == null) {
                            final JSONObject joMain = response.getJSONObject();
                            if (joMain.has(REQUEST_FIELD_DATA)) {
                                final JSONArray jaData = joMain.optJSONArray(REQUEST_FIELD_DATA);
                                int length = jaData.length();
                                for (int i = 0; i < length; i++) {
                                    getFacebookAlbumImages(jaData.getJSONObject(i).optString(REQUEST_FIELD_NAME),
                                            jaData.getJSONObject(i).optString(REQUEST_FIELD_ID),
                                            (length - 1) == i);
                                }
                            }
                        } else {
                            Timber.d(response.getError().toString());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
        ).executeAsync();
    }

    private void getFacebookAlbumImages(final String albumName, final String albumId, final boolean isLastAlbum) {
        final Bundle parameters = new Bundle();
        parameters.putString(REQUEST_PARAM_FIELDS, REQUEST_FIELDS);
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/" + albumId + "/" + REQUEST_FIELD_PHOTOS,
                parameters,
                HttpMethod.GET,
                response -> {
                    Timber.v("Facebook Photos response: %s", response);
                    try {
                        if (response.getError() == null) {
                            final JSONObject joMain = response.getJSONObject();
                            if (joMain.has(REQUEST_FIELD_DATA)) {
                                final JSONArray jaData = joMain.optJSONArray(REQUEST_FIELD_DATA);
                                final HashSet<String> urlAlbumPhotoSet = new HashSet<>();
                                for (int i = 0; i < jaData.length(); i++) {
                                    final JSONArray jaImages = jaData.getJSONObject(i).getJSONArray(REQUEST_FIELDS);
                                    if (jaImages.length() > 0) {
                                        urlAlbumPhotoSet.add(jaImages.getJSONObject(0).getString(REQUEST_FIELD_SOURCE));
                                    }
                                }
                                facebookMediaSources.put(albumName, urlAlbumPhotoSet);
                                if (isLastAlbum) {
                                    facebookProgressListener.offAlert();
                                    facebookDataListener.onFacebookMediaData(facebookMediaSources);
                                }
                            }
                        } else {
                            Timber.v(response.getError().toString());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        ).executeAsync();
    }
}

package com.clickfreebackup.clickfree;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import timber.log.Timber;

import static com.clickfreebackup.clickfree.network.InstagramService.INSTAGRAM_AUTH_API_BASE_URL;
import static com.clickfreebackup.clickfree.repository.InstagramRepositiryImpl.REDIRECTION_BASE_URL;

public class InstagramAuthDialog extends Dialog implements ClearListener {
    private final static String INSTAGRAM_CODE_PARAMETER = "code=";
    private final static String INSTAGRAM_SCOPE = "user_profile,user_media";
    private String requestUrl;
    private final String redirectUrl;
    private final String instagramAppId;
    private final String instagramAppSecret;
    private final PhotosVideosPresenter presenter;
    private InstagramDataListener instagramDataListener;

    public InstagramAuthDialog(@NonNull Context context, InstagramDataListener instagramDataListener, PhotosVideosPresenter presenter) {
        super(context);
        redirectUrl = context.getResources().getString(R.string.redirect_url);
        instagramAppId = context.getResources().getString(R.string.instagram_app_id);
        instagramAppSecret = context.getResources().getString(R.string.instagram_app_secret);
        this.presenter = presenter;
        this.instagramDataListener = instagramDataListener;
        requestUrl = INSTAGRAM_AUTH_API_BASE_URL + "oauth/authorize?" + "app_id=" + instagramAppId +
                "&" + "redirect_uri=" + redirectUrl + "&" + "scope=" + INSTAGRAM_SCOPE + "&" + "response_type=code";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.instagram_auth_dialog);
        initializeWebView();
        if (getWindow() != null) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void initializeWebView() {
        final WebView webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.setPadding(0, 0, 0, 0);
        webView.setInitialScale(getScale());
        webView.loadUrl(requestUrl);
        webView.setWebViewClient(webViewClient);
    }

    private int getScale() {
        Double val = (double) ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getWidth() / 390d;
        val = val * 100d;
        return val.intValue();
    }

    final WebViewClient webViewClient = new WebViewClient() {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith(redirectUrl)) {
                InstagramAuthDialog.this.dismiss();
                return true;
            }
            return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            if (url.contains(REDIRECTION_BASE_URL) && url.contains(INSTAGRAM_CODE_PARAMETER)) {
                final String code = getCodeFromUrl(url);
                getUserToken(code);
                dismiss();
            }
        }
    };

    private String getCodeFromUrl(String url) {
        if (url != null && !url.isEmpty() && url.split(INSTAGRAM_CODE_PARAMETER).length > 1) {
            return url.split(INSTAGRAM_CODE_PARAMETER)[1].split("#_")[0];
        } else {
            instagramDataListener.onInstagramMediaData(null, InstagramAuthDialog.this);
            return "";
        }
    }

    private void getUserToken(String code) {
        Timber.d("Fetch user's token");
        presenter.callUserToken(code, instagramAppId, instagramAppSecret);
    }

    @Override
    public void clearListeners() {
        instagramDataListener = null;
    }
}

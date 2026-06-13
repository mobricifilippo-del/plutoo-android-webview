package com.plutoo.app;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.graphics.Bitmap;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.content.Intent;
import android.app.Activity;
import android.content.ActivityNotFoundException;

import androidx.appcompat.app.AppCompatActivity;

import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private AdView adView;
    private RewardedAd rewardedAd;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 1001;

    private static final String REWARDED_AD_UNIT_ID =
            "ca-app-pub-5458345293928736/7078342992";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileAds.initialize(this, initializationStatus -> {});

        FrameLayout layout = new FrameLayout(this);

        adView = new AdView(this);

        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        adParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        adView.setLayoutParams(adParams);

        adView.setAdUnitId("ca-app-pub-5458345293928736/3837438698");
        adView.setAdSize(AdSize.BANNER);
        adView.loadAd(new AdRequest.Builder().build());

        webView = new WebView(this);

        FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        webParams.bottomMargin = AdSize.BANNER.getHeightInPixels(this);
        webView.setLayoutParams(webParams);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                10
        ));
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);

        layout.addView(webView);
        layout.addView(progressBar);
        layout.addView(adView);

        setContentView(layout);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        setupWebView();

        webView.addJavascriptInterface(new PlutooJsBridge(), "AndroidBridge");

        loadRewardedAd();

        webView.loadUrl("https://plutoo-official.vercel.app/?app=android");
    }

    private void loadRewardedAd() {
        RewardedAd.load(
                this,
                REWARDED_AD_UNIT_ID,
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        rewardedAd = null;
                    }
                }
        );
    }

    private void showRewardedAd() {
        if (rewardedAd == null) {
            notifyRewardFailed();
            loadRewardedAd();
            return;
        }

        final boolean[] rewardEarned = {false};

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                loadRewardedAd();

                if (!rewardEarned[0]) {
                    notifyRewardFailed();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                rewardedAd = null;
                loadRewardedAd();
                notifyRewardFailed();
            }
        });

        rewardedAd.show(this, rewardItem -> {
            rewardEarned[0] = true;
            notifyRewardEarned();
        });
    }

    public class PlutooJsBridge {
        @JavascriptInterface
        public void showRewarded() {
            runOnUiThread(() -> showRewardedAd());
        }
    }

    private void notifyRewardEarned() {
        if (webView == null) return;

        runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(
                        "window.onRewardEarned && window.onRewardEarned();",
                        null
                );
            }
        });
    }

    private void notifyRewardFailed() {
        if (webView == null) return;

        runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(
                        "window.onRewardFailed && window.onRewardFailed();",
                        null
                );
            }
        });
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setGeolocationEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setUseWideViewPort(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();

                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        return false;
                    }

                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        // Ignora se non c'è app compatibile
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        request.grant(request.getResources());
                    } catch (Exception e) {
                        // Ignora
                    }
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallbackNew,
                    FileChooserParams fileChooserParams) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }

                filePathCallback = filePathCallbackNew;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");

                try {
                    startActivityForResult(
                            Intent.createChooser(intent, "Seleziona immagine"),
                            FILE_CHOOSER_REQUEST
                    );
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    return false;
                }

                return true;
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                WebView.HitTestResult result = view.getHitTestResult();
                String url = result != null ? result.getExtra() : null;

                if (url != null) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    try {
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        // Ignora
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView == null) {
            super.onBackPressed();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(
                    "window.handleAndroidBack && window.handleAndroidBack();",
                    value -> {
                        if ("\"HANDLED\"".equals(value)) {
                            return;
                        }

                        if (webView.canGoBack()) {
                            webView.goBack();
                        } else {
                            MainActivity.super.onBackPressed();
                        }
                    }
            );
        } else {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }

        if (adView != null) {
            adView.destroy();
            adView = null;
        }

        rewardedAd = null;

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;

            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            }

            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}

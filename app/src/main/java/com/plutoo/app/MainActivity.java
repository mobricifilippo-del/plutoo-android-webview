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
import android.content.ClipData;
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

import com.google.android.gms.ads.AdError;
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

    private boolean rewardShowing = false;
    private boolean rewardEarnedPending = false;

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
    if (rewardShowing) return;

    if (rewardedAd == null) {
        notifyRewardFailed();
        loadRewardedAd();
        return;
    }

    rewardShowing = true;
    rewardEarnedPending = false;

    rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
        @Override
        public void onAdDismissedFullScreenContent() {
            rewardedAd = null;
            rewardShowing = false;

            if (!rewardEarnedPending) {
                notifyRewardFailed();
            }

            rewardEarnedPending = false;
            loadRewardedAd();
        }

        @Override
        public void onAdFailedToShowFullScreenContent(AdError adError) {
            rewardedAd = null;
            rewardShowing = false;
            rewardEarnedPending = false;

            loadRewardedAd();
            notifyRewardFailed();
        }
    });

    rewardedAd.show(this, rewardItem -> {
        rewardEarnedPending = true;
        notifyRewardEarned();
    });
    }

    public class PlutooJsBridge {
        @JavascriptInterface
        public void showRewarded() {
            runOnUiThread(() -> showRewardedAd());
        }

        @JavascriptInterface
        public void openUrl(String url) {
            runOnUiThread(() -> openExternalUrl(url));
        }
    }

    private void notifyRewardEarned() {
        evaluateJavascriptSafe("window.onRewardEarned && window.onRewardEarned();");
    }

    private void notifyRewardFailed() {
        evaluateJavascriptSafe("window.onRewardFailed && window.onRewardFailed();");
    }

    private void evaluateJavascriptSafe(String js) {
        if (webView == null || js == null) return;

        runOnUiThread(() -> {
            if (webView == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(js, null);
            } else {
                webView.loadUrl("javascript:" + js);
            }
        });
    }

    private void openExternalUrl(String url) {
        if (url == null || url.trim().isEmpty()) return;

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception ignored) {}
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

                    openExternalUrl(url);
                    return true;
                }

                return false;
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;

                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false;
                }

                openExternalUrl(url);
                return true;
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
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallbackNew,
                    FileChooserParams fileChooserParams
            ) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }

                filePathCallback = filePathCallbackNew;

                Intent intent;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                } else {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                intent.addCategory(Intent.CATEGORY_OPENABLE);

                boolean allowMultiple = false;
                String[] acceptTypes = null;

                if (fileChooserParams != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    acceptTypes = fileChooserParams.getAcceptTypes();
                    allowMultiple = fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE;
                }

                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);

                String mimeType = resolveMimeType(acceptTypes);
                intent.setType(mimeType);

                String[] cleanedTypes = cleanAcceptTypes(acceptTypes);
                if (cleanedTypes.length > 0) {
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, cleanedTypes);
                }

                try {
                    startActivityForResult(
                            Intent.createChooser(intent, "Seleziona file"),
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

                if (url != null && !url.trim().isEmpty()) {
                    openExternalUrl(url);
                    return true;
                }

                return false;
            }
        });
    }

    private String resolveMimeType(String[] acceptTypes) {
        if (acceptTypes == null || acceptTypes.length == 0) {
            return "*/*";
        }

        boolean hasImage = false;
        boolean hasVideo = false;
        boolean hasAny = false;

        for (String type : acceptTypes) {
            if (type == null) continue;

            String t = type.trim().toLowerCase();

            if (t.isEmpty()) continue;
            if ("*/*".equals(t)) hasAny = true;
            if (t.startsWith("image/")) hasImage = true;
            if (t.startsWith("video/")) hasVideo = true;
        }

        if (hasAny || (hasImage && hasVideo)) return "*/*";
        if (hasImage) return "image/*";
        if (hasVideo) return "video/*";

        return "*/*";
    }

    private String[] cleanAcceptTypes(String[] acceptTypes) {
        if (acceptTypes == null || acceptTypes.length == 0) {
            return new String[]{"image/*", "video/*"};
        }

        java.util.ArrayList<String> cleaned = new java.util.ArrayList<>();

        for (String type : acceptTypes) {
            if (type == null) continue;

            String t = type.trim();

            if (!t.isEmpty()) {
                cleaned.add(t);
            }
        }

        if (cleaned.isEmpty()) {
            cleaned.add("image/*");
            cleaned.add("video/*");
        }

        return cleaned.toArray(new String[0]);
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
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }

        if (webView != null) {
            webView.onPause();
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (webView != null) {
            webView.onResume();
        }

        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }

        rewardedAd = null;
        rewardShowing = false;
        rewardEarnedPending = false;

        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.removeJavascriptInterface("AndroidBridge");
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }

        if (adView != null) {
            adView.destroy();
            adView = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;

            Uri[] results = null;

            if (resultCode == Activity.RESULT_OK && data != null) {
                ClipData clipData = data.getClipData();

                if (clipData != null && clipData.getItemCount() > 0) {
                    results = new Uri[clipData.getItemCount()];

                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        results[i] = clipData.getItemAt(i).getUri();
                    }

                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }

            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}

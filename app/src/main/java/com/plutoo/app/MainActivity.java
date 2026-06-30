package com.plutoo.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.graphics.Bitmap;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;

import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private AdView adView;
    private boolean rewardedAdLoading = false;
    private boolean pendingRewardRequest = false;
    private int rewardedRetryAttempt = 0;
    private final Handler rewardedHandler = new Handler(Looper.getMainLooper());

    private static final long REWARDED_PENDING_TIMEOUT_MS = 15000;

    private final Runnable rewardedPendingTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!pendingRewardRequest) return;

            pendingRewardRequest = false;
            rewardedAdLoading = false;
            rewardedAd = null;

            Toast.makeText(
                    MainActivity.this,
                    "Video non disponibile. Riprova.",
                    Toast.LENGTH_SHORT
            ).show();

            notifyRewardFailed();
            loadRewardedAd();
        }
    };

    private RewardedAd rewardedAd;
    private BillingClient billingClient;
    private boolean billingReady = false;
    private ProductDetails plusProductDetails = null;
    private String pendingPlanId = null;
    private String lastPurchasedPlanId = null;
    private boolean plusPurchaseReady = false;

    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 1001;

    private static final int LOCATION_PERMISSION_REQUEST = 2001;

    private String pendingGeoOrigin;
    private GeolocationPermissions.Callback pendingGeoCallback;

    private static final String REWARDED_AD_UNIT_ID =
            "ca-app-pub-5458345293928736/7078342992";

    // ─── LIFECYCLE ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileAds.initialize(this, initializationStatus -> {});

        FrameLayout layout = new FrameLayout(this);

        // Banner AdMob
        adView = new AdView(this);
        adView.setAdUnitId("ca-app-pub-5458345293928736/3837438698");
        adView.setAdSize(AdSize.BANNER);

        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        adParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        adView.setLayoutParams(adParams);
        adView.loadAd(new AdRequest.Builder().build());
        adView.setVisibility(View.GONE);

        // WebView
        webView = new WebView(this);
        FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        webParams.bottomMargin = 0;
        webView.setLayoutParams(webParams);

        // ProgressBar
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

        initBillingClient();

        webView.addJavascriptInterface(new PlutooJsBridge(), "AndroidBridge");

        loadRewardedAd();

        webView.loadUrl("https://plutoo-official.vercel.app/?app=android");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adView != null) adView.pause();
        if (webView != null) webView.onPause();
    }

    @Override
protected void onResume() {
    super.onResume();
    if (webView != null) webView.onResume();
    if (adView != null) adView.resume();
    loadRewardedAd();
}

    @Override
    protected void onDestroy() {
        // Chiudi callback file picker pendente
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }

        rewardedAd = null;

        rewardedAdLoading = false;
        pendingRewardRequest = false;
        rewardedRetryAttempt = 0;
        rewardedHandler.removeCallbacksAndMessages(null);

        billingReady = false;
        if (billingClient != null) {
            billingClient.endConnection();
            billingClient = null;
        }

        if (adView != null) {
            adView.destroy();
            adView = null;
        }

        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.removeJavascriptInterface("AndroidBridge");
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }

    // ─── BACK ─────────────────────────────────────────────────────────────────

    @Override
public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus && webView != null) {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }
}

@Override
public void onBackPressed() {
        if (webView == null) {
            super.onBackPressed();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(
                "window.plutooGoBack && window.plutooGoBack();",
                    value -> {
                        if ("\"HANDLED\"".equals(value)) {
                            return;
                        }
                        if (webView != null && webView.canGoBack()) {
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

    // ─── FILE PICKER ──────────────────────────────────────────────────────────

    @Override
public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode != LOCATION_PERMISSION_REQUEST) return;

    boolean granted = grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED;

    if (pendingGeoCallback != null && pendingGeoOrigin != null) {
        pendingGeoCallback.invoke(pendingGeoOrigin, granted, false);
    }

    pendingGeoCallback = null;
    pendingGeoOrigin = null;
}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != FILE_CHOOSER_REQUEST) return;

        if (filePathCallback == null) return;

        Uri[] results = null;

        if (resultCode == Activity.RESULT_OK && data != null) {
            // Selezione multipla (ClipData)
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                results = new Uri[count];
                for (int i = 0; i < count; i++) {
                    results[i] = data.getClipData().getItemAt(i).getUri();
                }
            } else if (data.getData() != null) {
                // Selezione singola
                results = new Uri[]{data.getData()};
            }
        }

        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }

    // ─── ADMOB REWARD ─────────────────────────────────────────────────────────

    private void loadRewardedAd() {
    if (rewardedAd != null || rewardedAdLoading) {
        return;
    }

    rewardedAdLoading = true;

    RewardedAd.load(
            this,
            REWARDED_AD_UNIT_ID,
            new AdRequest.Builder().build(),
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(RewardedAd ad) {
                    
                    rewardedAd = ad;
                    rewardedAdLoading = false;
                    rewardedRetryAttempt = 0;
                    rewardedHandler.removeCallbacks(rewardedPendingTimeoutRunnable);

                    if (pendingRewardRequest) {
                        pendingRewardRequest = false;
                        showRewardedAd();
                    }
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    rewardedAd = null;
                    rewardedAdLoading = false;
                    rewardedHandler.removeCallbacks(rewardedPendingTimeoutRunnable);

                    if (pendingRewardRequest) {
                        pendingRewardRequest = false;
                        notifyRewardFailed();
                    }

                    int delayMs = Math.min(
                            30000,
                            (int) Math.pow(2, Math.min(rewardedRetryAttempt, 4)) * 1000
                    );

                    rewardedRetryAttempt++;

                    rewardedHandler.postDelayed(() -> loadRewardedAd(), delayMs);
                }
            }
    );
    }

    private void showRewardedAd() {
    if (rewardedAd == null) {
        if (pendingRewardRequest) {
            return;
        }

        pendingRewardRequest = true;

        Toast.makeText(
                MainActivity.this,
                "Caricamento video...",
                Toast.LENGTH_SHORT
        ).show();

        rewardedHandler.removeCallbacks(rewardedPendingTimeoutRunnable);
        rewardedHandler.postDelayed(
                rewardedPendingTimeoutRunnable,
                REWARDED_PENDING_TIMEOUT_MS
        );

        loadRewardedAd();
        return;
    }

    final RewardedAd adToShow = rewardedAd;
    rewardedAd = null;

    final boolean[] rewardEarned = {false};

    adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
        @Override
        public void onAdDismissedFullScreenContent() {
            loadRewardedAd();

            if (rewardEarned[0]) {
                notifyRewardEarned();
            } else {
                notifyRewardFailed();
            }
        }

        @Override
        public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
            loadRewardedAd();
            notifyRewardFailed();
        }
    });

    adToShow.show(MainActivity.this, rewardItem -> {
        rewardEarned[0] = true;
    });
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

    private void notifyPlusPurchased(String planId) {
        if (webView == null) return;
        final String safePlanId = (planId != null) ? planId : "unknown";
        runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(
                        "window.onPlusPurchased && window.onPlusPurchased('" + safePlanId + "');",
                        null
                );
            }
        });
    }

    // ─── GOOGLE PLAY BILLING ──────────────────────────────────────────────────

    private void initBillingClient() {
        billingReady = false;

        billingClient = BillingClient.newBuilder(this)
                .setListener((billingResult, purchases) -> handlePurchases(billingResult, purchases))
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .build()
                )
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                billingReady = billingResult != null
                        && billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK;
                if (billingReady) {
                    queryPlusProductDetails();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                billingReady = false;
                plusProductDetails = null;
            }
        });
    }

    private void queryPlusProductDetails() {
        if (billingClient == null || !billingReady) return;

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                        java.util.Collections.singletonList(
                                QueryProductDetailsParams.Product.newBuilder()
                                        .setProductId("plutoo_plus")
                                        .setProductType(BillingClient.ProductType.SUBS)
                                        .build()
                        )
                )
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
        && productDetailsList != null
        && productDetailsList.getProductDetailsList() != null
        && !productDetailsList.getProductDetailsList().isEmpty()) {
    plusProductDetails = productDetailsList.getProductDetailsList().get(0);
} else {
    plusProductDetails = null;
            }
        });
    }

    private void purchasePlus(String planId) {
        if (!billingReady || billingClient == null || plusProductDetails == null) return;

        java.util.List<ProductDetails.SubscriptionOfferDetails> offers =
                plusProductDetails.getSubscriptionOfferDetails();
        if (offers == null || offers.isEmpty()) return;

        String offerToken = null;
        for (ProductDetails.SubscriptionOfferDetails offer : offers) {
            if (planId.equals(offer.getBasePlanId())) {
                offerToken = offer.getOfferToken();
                break;
            }
        }
        if (offerToken == null) return;

        BillingFlowParams.ProductDetailsParams productDetailsParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(plusProductDetails)
                        .setOfferToken(offerToken)
                        .build();

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                        java.util.Collections.singletonList(productDetailsParams)
                )
                .build();

        pendingPlanId = planId;
billingClient.launchBillingFlow(MainActivity.this, billingFlowParams);
    }

    private void handlePurchases(BillingResult billingResult, java.util.List<Purchase> purchases) {
        int responseCode = billingResult.getResponseCode();

        if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) return;

        if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
    plusPurchaseReady = true;

    String resolvedPlanId = null;

    if ("monthly".equals(pendingPlanId) || "yearly".equals(pendingPlanId)) {
        resolvedPlanId = pendingPlanId;
        lastPurchasedPlanId = pendingPlanId;
    } else if ("monthly".equals(lastPurchasedPlanId) || "yearly".equals(lastPurchasedPlanId)) {
        resolvedPlanId = lastPurchasedPlanId;
    }

    plusPurchaseReady = false;
    pendingPlanId = null;

    if (resolvedPlanId == null) {
        Toast.makeText(
                MainActivity.this,
                "Abbonamento già attivo. Riapri Plutoo Plus.",
                Toast.LENGTH_SHORT
        ).show();
        return;
    }

    notifyPlusPurchased(resolvedPlanId);
    return;
        }

        if (responseCode != BillingClient.BillingResponseCode.OK) return;

        if (purchases == null || purchases.isEmpty()) return;

        for (Purchase purchase : purchases) {
            if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) continue;
            if (!purchase.getProducts().contains("plutoo_plus")) continue;

            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams ackParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();

            billingClient.acknowledgePurchase(ackParams, ackResult -> {
    if (ackResult.getResponseCode()
            == BillingClient.BillingResponseCode.OK) {
        plusPurchaseReady = true;
        if (pendingPlanId != null) {
            lastPurchasedPlanId = pendingPlanId;
        }
        notifyPlusPurchased(lastPurchasedPlanId);
        plusPurchaseReady = false;
        pendingPlanId = null;
    }
});
            } else {
                plusPurchaseReady = true;
                if (pendingPlanId != null) {
                    lastPurchasedPlanId = pendingPlanId;
                }
                notifyPlusPurchased(lastPurchasedPlanId);
                plusPurchaseReady = false;
                pendingPlanId = null;
            }
        }
    }

    // ─── JAVASCRIPT BRIDGE ────────────────────────────────────────────────────

    public class PlutooJsBridge {

        // Reward AdMob
        @JavascriptInterface
        public void showRewarded() {
            runOnUiThread(() -> showRewardedAd());
        }

     @JavascriptInterface
        public boolean isBillingReady() {
            return billingReady;
        }

        @JavascriptInterface
        public void purchasePlus(String planId) {
            if (planId == null || planId.isEmpty()) return;
            runOnUiThread(() -> MainActivity.this.purchasePlus(planId));
        }

        // FIX C1/E1: apertura URL esterni (Maps, browser, geo:)
        @JavascriptInterface
        public void openUrl(String url) {
            if (url == null || url.isEmpty()) return;
            runOnUiThread(() -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    // Nessuna app compatibile: ignora
                }
            });
        }

        // FIX D1/D2: controllo visibilità banner nativo da JS
        @JavascriptInterface
        public void setBannerVisible(boolean visible) {
            if (adView == null || webView == null) return;
            runOnUiThread(() -> {
                if (visible) {
                    adView.setVisibility(View.VISIBLE);
                    // Ripristina il margine inferiore della WebView
                    FrameLayout.LayoutParams lp =
                            (FrameLayout.LayoutParams) webView.getLayoutParams();
                    if (lp != null) {
                        lp.bottomMargin = AdSize.BANNER.getHeightInPixels(MainActivity.this);
                        webView.setLayoutParams(lp);
                    }
                } else {
                    adView.setVisibility(View.GONE);
                    // Rimuove il margine: nessuno spazio nero
                    FrameLayout.LayoutParams lp =
                            (FrameLayout.LayoutParams) webView.getLayoutParams();
                    if (lp != null) {
                        lp.bottomMargin = 0;
                        webView.setLayoutParams(lp);
                    }
                }
            });
        }
    }

    // ─── WEBVIEW SETUP ────────────────────────────────────────────────────────

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setGeolocationEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setUseWideViewPort(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.clearCache(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) return false;

                String url = request.getUrl().toString();

                // HTTP/HTTPS: gestiti dal WebView
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false;
                }

                // Schemi esterni (geo:, intent:, market:, ecc.): delega ad Android
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    // Nessuna app compatibile
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(0);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null) progressBar.setProgress(newProgress);
            }

            @Override
public void onGeolocationPermissionsShowPrompt(
        String origin, GeolocationPermissions.Callback callback) {

    if (ContextCompat.checkSelfPermission(
            MainActivity.this,
            Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED) {
        callback.invoke(origin, true, false);
        return;
    }

    pendingGeoOrigin = origin;
    pendingGeoCallback = callback;

    ActivityCompat.requestPermissions(
            MainActivity.this,
            new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            },
            LOCATION_PERMISSION_REQUEST
    );
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

            // FIX B1: file picker per tutti gli input type="file"
            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams fileChooserParams) {

                // Chiudi eventuale callback pendente precedente
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                    filePathCallback = null;
                }
                filePathCallback = callback;

                // Determina i tipi MIME accettati
                String[] acceptTypes = null;
                if (fileChooserParams != null) {
                    acceptTypes = fileChooserParams.getAcceptTypes();
                }

                String mimeType = "*/*";
                if (acceptTypes != null && acceptTypes.length > 0) {
                    // Filtra tipi vuoti
                    StringBuilder sb = new StringBuilder();
                    for (String t : acceptTypes) {
                        if (t != null && !t.trim().isEmpty()) {
                            if (sb.length() > 0) sb.append(",");
                            sb.append(t.trim());
                        }
                    }
                    if (sb.length() > 0) mimeType = sb.toString();
                }

                // Selezione multipla
                boolean allowMultiple = fileChooserParams != null &&
                        fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(mimeType.contains(",") ? "*/*" : mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Tipi multipli: usa EXTRA_MIME_TYPES
                if (mimeType.contains(",")) {
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeType.split(","));
                }

                if (allowMultiple) {
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }

                try {
                    startActivityForResult(
                            Intent.createChooser(intent, "Seleziona file"),
                            FILE_CHOOSER_REQUEST
                    );
                } catch (Exception e) {
                    filePathCallback.onReceiveValue(null);
                    filePathCallback = null;
                    return false;
                }

                return true;
            }

            @Override
            public boolean onCreateWindow(
                    WebView view, boolean isDialog,
                    boolean isUserGesture, android.os.Message resultMsg) {
                WebView.HitTestResult result = view.getHitTestResult();
                String url = result != null ? result.getExtra() : null;
                if (url != null) {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        // Ignora
                    }
                }
                return false;
            }
        });
    }
}

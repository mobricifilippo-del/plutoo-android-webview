package com.plutoo.app;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.SslError;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    /* ------- AdMob ------- */
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;

    private String admobInterstitialId;
    private String admobRewardedId;

    @Override
    protected void onDestroy() {
        try {
            webView.removeJavascriptInterface("AndroidBridge");
            webView.destroy();
        } catch (Throwable ignored) {}
        super.onDestroy();
    }

    @SuppressLint({"SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* --------- AdMob init --------- */
        MobileAds.initialize(this, initializationStatus -> {});
        admobInterstitialId = getString(R.string.admob_interstitial_id);
        admobRewardedId     = getString(R.string.admob_rewarded_id);
        preloadInterstitial();
        preloadRewarded();

        /* --------- WebView setup --------- */
        webView = findViewById(R.id.webview);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " PlutooWebView/1.0");

        CookieManager.getInstance().setAcceptCookie(true);
        webView.setBackgroundColor(Color.BLACK);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new SafeClient());

        /* Bridge verso JS */
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        /* Carica la webapp Vercel */
        String startUrl = getString(R.string.start_url);
        webView.loadUrl(startUrl);

        /* Back = history WebView */
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) webView.goBack();
                else finish();
            }
        });
    }

    /* =========================
       WebViewClient sicuro
       ========================= */
    private class SafeClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            injectBridge();
        }
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            // Comportamento sicuro: non ignorare gli errori SSL
            handler.cancel();
        }
    }

    /* =========================
       Bridge: override openRewardDialog(message, after)
       ========================= */
    private void injectBridge() {
        String js =
            "(function(){try{"
          + "window.openRewardDialog=function(message,after){"
          + "  window.__pl_after=after;"
          + "  if(window.AndroidBridge&&AndroidBridge.requestAd){"
          + "    AndroidBridge.requestAd(String(message||''));"
          + "  }else{try{if(typeof after==='function')after();}catch(e){}}"
          + "};"
          + "}catch(e){}})();";
        webView.evaluateJavascript(js, null);
    }

    private void runAfterCallback() {
        String cb =
            "(function(){try{var f=window.__pl_after;window.__pl_after=null;"
          + "if(typeof f==='function')f();}catch(e){}})();";
        webView.post(() -> webView.evaluateJavascript(cb, null));
    }

    /* =========================
       AdMob helpers
       ========================= */
    private AdRequest adRequest() {
        AdRequest.Builder b = new AdRequest.Builder();
        // Se vuoi forzare un test device ID, valorizza R.string.admob_test_device_id
        String testId = getString(R.string.admob_test_device_id);
        if (testId != null && !testId.trim().isEmpty()) {
            // Da luglio 2023 non si usa più addTestDevice; il device test si configura da console.
            // Qui lasciamo semplicemente la possibilità di tenere un riferimento.
        }
        return b.build();
    }

    private void preloadInterstitial() {
        InterstitialAd.load(this, admobInterstitialId, adRequest(),
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd ad) {
                    interstitialAd = ad;
                }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    interstitialAd = null;
                }
            });
    }

    private void preloadRewarded() {
        RewardedAd.load(this, admobRewardedId, adRequest(),
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) {
                    rewardedAd = ad;
                }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    rewardedAd = null;
                }
            });
    }

    private void showInterstitial(@NonNull Runnable onClosed) {
        InterstitialAd ad = interstitialAd;
        if (ad == null) {
            preloadInterstitial();
            onClosed.run();
            return;
        }
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() {
                interstitialAd = null;
                preloadInterstitial();
                onClosed.run();
            }
            @Override public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                interstitialAd = null;
                preloadInterstitial();
                onClosed.run();
            }
        });
        ad.show(this);
    }

    private void showRewarded(@NonNull Runnable onReward) {
        RewardedAd ad = rewardedAd;
        if (ad == null) {
            preloadRewarded();
            // fallback: non blocchiamo l'UX
            onReward.run();
            return;
        }
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                preloadRewarded();
            }
            @Override public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                rewardedAd = null;
                preloadRewarded();
                onReward.run();
            }
        });
        ad.show(this, new OnUserEarnedRewardListener() {
            @Override public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                onReward.run();
            }
        });
    }

    /* =========================
       Android ↔ Web JS Bridge
       ========================= */
    private class AndroidBridge {
        @JavascriptInterface
        public void requestAd(String message) {
            String msg = (message == null ? "" : message).toLowerCase();
            boolean isInterstitial = msg.contains("sponsor") || msg.contains("veterin");

            runOnUiThread(() -> {
                if (isInterstitial) {
                    showInterstitial(() -> runAfterCallback());
                } else {
                    showRewarded(() -> runAfterCallback());
                }
            });
        }
        @JavascriptInterface
        public void log(String s) { /* no-op: utile in debug */ }
    }
}

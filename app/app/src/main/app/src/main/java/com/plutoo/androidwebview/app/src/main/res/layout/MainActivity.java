package com.plutoo.androidwebview;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.LoadAdError;

public class MainActivity extends AppCompatActivity {

    // URL ufficiale (Vercel)
    private static final String WEB_URL = "https://plutoo-official.vercel.app/";

    // AdMob – i TUOI ID delle unità
    // Banner (home)
    private static final String ADMOB_BANNER_ID = "ca-app-pub-5458345293928736/8955087050";
    // Interstitial (videomatch)
    private static final String ADMOB_INTERSTITIAL_ID = "ca-app-pub-5458345293928736/8626895942";

    private WebView webView;
    private AdView adView;
    private InterstitialAd interstitialAd;
    private boolean interstitialShownOnce = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // layout con WebView + banner (activity_main.xml)
        setContentView(R.layout.activity_main);

        // --- Mobile Ads init
        MobileAds.initialize(this, initializationStatus -> {});

        // --- Banner
        adView = findViewById(R.id.adView);
        adView.setAdUnitId(ADMOB_BANNER_ID);
        adView.loadAd(new AdRequest.Builder().build());

        // --- Interstitial (carico all'avvio, lo mostro la prima volta che la pagina finisce di caricarsi)
        loadInterstitial();

        // --- WebView
        webView = findViewById(R.id.webview);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setMediaPlaybackRequiresUserGesture(false); // per video autoplay (se consentito dalla pagina)

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView v, String url) {
                super.onPageFinished(v, url);
                // Mostra l’interstitial solo una volta dopo il primo caricamento
                if (!interstitialShownOnce && interstitialAd != null) {
                    interstitialAd.show(MainActivity.this);
                    interstitialShownOnce = true;
                }
            }
        });

        webView.loadUrl(WEB_URL);
    }

    private void loadInterstitial() {
        AdRequest request = new AdRequest.Builder().build();
        InterstitialAd.load(
                this,
                ADMOB_INTERSTITIAL_ID,
                request,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        interstitialAd = null;
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) adView.resume();
    }

    @Override
    protected void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

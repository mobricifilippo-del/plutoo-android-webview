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
import com.google.android.gms.ads.RequestConfiguration;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    // URL ufficiale (Vercel)
    private static final String WEB_URL = "https://plutoo-official.vercel.app/";

    private WebView webView;
    private AdView adView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1) Forza TEST ADS su questo device (sicurezza policy)
        // Metti il tuo device ID quando lo avrai (lo leggiamo da Logcat in futuro).
        RequestConfiguration requestConfiguration = new RequestConfiguration.Builder()
                .setTestDeviceIds(Arrays.asList(
                        AdRequest.DEVICE_ID_EMULATOR // sempre ok per emulatore
                        // , "INSERISCI_QUI_IL_TUO_TEST_DEVICE_ID"  // facoltativo: lo aggiungeremo più avanti
                ))
                .build();
        MobileAds.setRequestConfiguration(requestConfiguration);

        // 2) Inizializza AdMob
        MobileAds.initialize(this, initializationStatus -> { });

        // 3) WebView
        webView = findViewById(R.id.webview);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(WEB_URL);

        // 4) Banner (usa l'ID di TEST messo nel layout)
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}

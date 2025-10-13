package com.plutoo.wrappertest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * WebView wrapper:
 * - Geolocalizzazione HTML5 (prompt gestito)
 * - Cache disattivata per evitare schermate “bloccate”/vecchie
 * - Una sola Activity launcher (già messa nel Manifest del PASSO 1) → niente doppia icona
 */
public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String BASE = "https://plutoo-official.vercel.app/"; // <-- tua webapp
    private static final int GEO_REQ = 1001;

    // Per completare la callback della geolocalizzazione dopo il runtime-permission
    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Pulizia storage/cookie → evita vecchie cache o redirect
        try { WebStorage.getInstance().deleteAllData(); } catch (Exception ignored) {}
        try {
            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            cm.removeAllCookies(null);
            cm.flush();
        } catch (Exception ignored) {}

        webView = findViewById(R.id.webView);

        // ---- WebView settings sicuri e compatibili con PWA/HTML5 ----
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setLoadsImagesAutomatically(true);

        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setSupportZoom(false);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);

        // Niente cache → sempre versione più recente dell’app web
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // Niente mixed content (solo HTTPS)
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        // User-Agent con suffisso (utile per debug lato web)
        ws.setUserAgentString(ws.getUserAgentString() + " PlutooWebView/1.0");

        webView.setBackgroundColor(Color.BLACK);

        // Mantieni la navigazione in-app
        webView.setWebViewClient(new WebViewClient());

        // Gestione geolocalizzazione HTML5 (navigator.geolocation)
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // Se abbiamo già i permessi runtime → concedi subito a questa origine
                if (hasGeoPermission()) {
                    callback.invoke(origin, true, false);
                } else {
                    // Richiedi permessi runtime e memorizza la callback per dopo
                    pendingGeoOrigin = origin;
                    pendingGeoCallback = callback;
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                            GEO_REQ
                    );
                }
            }
        });

        // Carica la webapp con cache-buster e ancoraggio home (evita landing “bloccate”)
        String url = BASE + "#home?t=" + System.currentTimeMillis();
        webView.clearCache(true);
        webView.loadUrl(url);

        // Back: torna nella history della WebView, altrimenti chiudi
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) webView.goBack();
                else finish();
            }
        });
    }

    private boolean hasGeoPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Completa la richiesta geolocalizzazione HTML5 dopo il runtime-permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GEO_REQ && pendingGeoCallback != null && pendingGeoOrigin != null) {
            boolean granted = hasGeoPermission();
            try {
                pendingGeoCallback.invoke(pendingGeoOrigin, granted, false);
            } catch (Exception ignored) {}
            pendingGeoCallback = null;
            pendingGeoOrigin = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        try {
            if (webView != null) {
                webView.stopLoading();
                webView.destroy();
                webView = null;
            }
        } catch (Exception ignored) {}
        super.onDestroy();
    }
}

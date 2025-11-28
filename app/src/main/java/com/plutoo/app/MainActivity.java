package com.plutoo.app;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.graphics.Bitmap;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout contenitore
        FrameLayout layout = new FrameLayout(this);

        // WebView
        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Progress bar in alto
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                10
        ));
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);

        // Aggiungo al layout
        layout.addView(webView);
        layout.addView(progressBar);

        setContentView(layout);

        // ✅ MODIFICATO: uso software rendering invece di hardware
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Configurazione WebView
        setupWebView();

        // Carica Plutoo
        webView.loadUrl("https://plutoo-official.vercel.app/");
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
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Client per navigazione interna e progress bar
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Manteniamo tutto dentro la WebView, tranne link esterni speciali
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();

                    // Se è http/https restiamo nella WebView
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        return false;
                    }

                    // Per altri schemi (tel:, mailto:, ecc.) usiamo le app esterne
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

        // Chrome client (geolocalizzazione, progress bar, permessi media)
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // Concediamo sempre la geolocalizzazione (Android chiederà il permesso all'utente)
                callback.invoke(origin, true, false);
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        request.grant(request.getResources());
                    } catch (Exception e) {
                        // Se qualcosa va storto, ignoriamo il permesso invece di far crashare l'app
                    }
                }
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                // Gestiamo finestre nuove aprendo il browser esterno
                WebView.HitTestResult result = view.getHitTestResult();
                String url = result != null ? result.getExtra() : null;
                if (url != null) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    try {
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        // Nessun browser disponibile, ignora
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

        // Gestione back via JS (per chiudere storie, modali, ecc.)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(
                    "window.handleAndroidBack && window.handleAndroidBack();",
                    value -> {
                        // Se JS ha gestito il back (ritorna "HANDLED"), non facciamo altro
                        if ("\"HANDLED\"".equals(value)) {
                            return;
                        }

                        // Altrimenti comportamento standard WebView
                        if (webView.canGoBack()) {
                            webView.goBack();
                        } else {
                            MainActivity.super.onBackPressed();
                        }
                    }
            );
        } else {
            // Vecchie versioni senza evaluateJavascript
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
        super.onDestroy();
    }
}

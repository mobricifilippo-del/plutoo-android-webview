package com.plutoo.app;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.os.Build;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.graphics.Bitmap;
import android.widget.ProgressBar;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Crea layout programmaticamente
        FrameLayout layout = new FrameLayout(this);

        // Crea WebView
        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Crea ProgressBar
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                10  // Altezza della progress bar
        ));
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);

        // Aggiungi views al layout
        layout.addView(webView);
        layout.addView(progressBar);

        setContentView(layout);

        setupWebView();

        // Carica l'URL
        webView.loadUrl("https://plutoo-official.vercel.app/");
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();

        // Impostazioni base
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // Cache
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Supporto per localStorage e sessionStorage
        webSettings.setDomStorageEnabled(true);

        // Supporto per geolocalizzazione
        webSettings.setGeolocationEnabled(true);

        // Media
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // Zoom
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);

        // Viewport
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        // Mixed content (per HTTPS/HTTP)
        if (Build.VERSION.SDK_INT >= 21) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // WebViewClient per gestire navigazione
        webView.setWebViewClient(new WebViewClient() {
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

        // WebChromeClient per features avanzate
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                                                           GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= 21) {
                    request.grant(request.getResources());
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

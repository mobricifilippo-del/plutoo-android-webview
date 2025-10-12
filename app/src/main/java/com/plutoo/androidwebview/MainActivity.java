package com.plutoo.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://plutoo-official.vercel.app/";
    private static final String ALLOWED_HOST = "plutoo-official.vercel.app";

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);

        // --- WebView settings
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setAllowFileAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cm.setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if (ALLOWED_HOST.equalsIgnoreCase(u.getHost())) {
                    // Rimani dentro la WebView
                    return false;
                }
                // Apri tutto il resto nel browser
                startActivity(new Intent(Intent.ACTION_VIEW, u));
                return true;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Il dominio è HTTPS valido su Vercel, ma se arrivano warning proseguiamo per evitare blocchi tipo ERR_CACHE_MISS
                if (error != null && error.getUrl() != null) {
                    Uri u = Uri.parse(error.getUrl());
                    if (ALLOWED_HOST.equalsIgnoreCase(u.getHost())) {
                        handler.proceed();
                        return;
                    }
                }
                handler.cancel();
            }
        });

        // Back: torna nella cronologia web, esci solo se non si può tornare
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack();
                else finish();
            }
        });

        if (savedInstanceState != null) {
            // Ripristina stato (tab che avevi aperto, scroll, ecc.)
            webView.restoreState(savedInstanceState);
        } else {
            // Se entri da deep link (es. tap su link plutoo), apri quello;
            // altrimenti carica la root dell'app (che porta alla Home → Entra → resto dell'app).
            Uri deep = getIntent() != null ? getIntent().getData() : null;
            if (deep != null && ALLOWED_HOST.equalsIgnoreCase(deep.getHost())) {
                webView.loadUrl(deep.toString());
            } else {
                webView.loadUrl(BASE_URL);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Uri deep = intent.getData();
        if (deep != null && ALLOWED_HOST.equalsIgnoreCase(deep.getHost())) {
            webView.loadUrl(deep.toString());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}

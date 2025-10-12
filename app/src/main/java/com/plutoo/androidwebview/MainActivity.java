package com.plutoo.androidwebview;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private WebView webView;

    // ✅ HOME della tua web-app (non pagina profilo)
    private static final String START_URL = "https://plutoo-official.vercel.app/";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);

        // ——— Config completa e sicura della WebView
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            // Mantieni tutta la navigazione dentro la WebView
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                v.loadUrl(r.getUrl().toString());
                return true;
            }

            @Override public void onPageStarted(WebView v, String url, Bitmap favicon) {
                // se per qualsiasi motivo si finisce su una pagina interna al primo avvio, ritorna alla HOME
                if (isFirstLoadToWrongPage(url)) {
                    v.stopLoading();
                    v.loadUrl(bustedStartUrl());
                }
            }
        });

        // ——— Pulisci cache/stato per evitare redirect persistenti (service worker / SW cache)
        webView.clearHistory();
        webView.clearCache(true);
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();

        if (savedInstanceState != null) {
            // Se torni da rotazione, ripristina stato WebView
            webView.restoreState(savedInstanceState);
        } else {
            // Carica SEMPRE la HOME, con cache-buster per evitare profilo in cache
            webView.loadUrl(bustedStartUrl());
        }

        // Tasto indietro → cronologia WebView
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack(); else finish();
            }
        });
    }

    private String bustedStartUrl() {
        long t = System.currentTimeMillis();
        return START_URL + (START_URL.contains("?") ? "&" : "?") + "t=" + t;
    }

    // Se al primo caricamento troviamo una rotta interna (es. /profile, /dog/123, ecc.), forziamo HOME
    private boolean isFirstLoadToWrongPage(String url) {
        if (url == null) return false;
        // consenti solo la home root del dominio
        return url.startsWith("https://plutoo-official.vercel.app/")
                && !url.equals("https://plutoo-official.vercel.app/")
                && !url.startsWith("https://plutoo-official.vercel.app/?");
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        webView.saveState(out);
    }
}

package com.plutoo.androidwebview;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);

        // JS + DOM storage
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);

        // === ANTI-CACHE in sviluppo ===
        s.setCacheMode(WebSettings.LOAD_NO_CACHE); // non usare cache HTTP
        webView.clearCache(true);                  // svuota cache su ogni avvio
        webView.clearHistory();

        // Evita di aprire il browser esterno
        webView.setWebViewClient(new WebViewClient());

        // Cache-buster lato URL (cambia il numero quando fai deploy)
        String version = "v=28"; // ↑ incrementa quando pubblichi
        webView.loadUrl("https://plutoo-official.vercel.app/?" + version);

        setContentView(webView);
    }
}

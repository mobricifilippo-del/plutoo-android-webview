package com.plutoo.app;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String BASE_URL = "https://plutoo-official.vercel.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Carica il layout con la WebView
            setContentView(R.layout.activity_main);

            webView = findViewById(R.id.webView);
            if (webView == null) {
                // Se per qualche motivo non trova la WebView, chiude pulito
                finish();
                return;
            }

            // Impostazioni MINIME ma sufficienti
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);

            // Mantieni tutto dentro l'app
            webView.setWebViewClient(new WebViewClient());

            // Carica Plutoo
            webView.loadUrl(BASE_URL);

        } catch (Throwable t) {
            // Qualsiasi eccezione in onCreate la gestiamo qui
            TextView tv = new TextView(this);
            tv.setText("Errore di inizializzazione dell'app Plutoo.");
            setContentView(tv);
        }
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

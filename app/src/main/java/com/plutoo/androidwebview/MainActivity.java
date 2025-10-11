package com.plutoo.androidwebview;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    private static final String APP_URL = "https://plutoo-official.vercel.app/";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        // --- WebView setup sicuro ma completo
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // 1) rimuovi eventuale overlay/modale “Profilo”
                String jsHideOverlay =
                        "(() => {"
                      + "  function kill() {"
                      + "    const labels = Array.from(document.querySelectorAll('*'))"
                      + "      .filter(n => n && n.textContent && n.textContent.trim().toLowerCase() === 'profilo');"
                      + "    if (labels.length) {"
                      + "      const dialog = labels[0].closest('[role=\"dialog\"], .modal, .dialog, .overlay');"
                      + "      if (dialog) dialog.remove();"
                      + "    }"
                      + "    const backdrops = document.querySelectorAll('.modal-backdrop, .overlay, .backdrop');"
                      + "    backdrops.forEach(b => b.remove());"
                      + "    document.body.style.overflow = 'auto';"
                      + "  }"
                      + "  kill();"
                      + "  window.__plutooKiller = window.__plutooKiller || setInterval(kill, 500);"
                      + "})();";

                view.evaluateJavascript(jsHideOverlay, null);

                // 2) abilita lo scroll nel body se bloccato da CSS
                String jsEnableScroll =
                        "document.documentElement.style.overflow='auto';"
                      + "document.body.style.overflow='auto'";
                view.evaluateJavascript(jsEnableScroll, null);
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl(APP_URL);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}

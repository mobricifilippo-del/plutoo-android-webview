package com.plutoo.androidwebview;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    // ✅ Torniamo al tuo dominio Vercel
    private static final String START_URL = "https://plutoo-official.vercel.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        // User-Agent mobile Chrome
        String chromeMobileUA =
                "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/126.0.0.0 Mobile Safari/537.36";
        s.setUserAgentString(chromeMobileUA);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        CookieManager.getInstance().setAcceptCookie(true);

        // tap/focus
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setOnTouchListener((v, e) -> { if (!v.hasFocus()) v.requestFocus(); return false; });
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return false; }
            @Override public void onReceivedSslError(WebView v, SslErrorHandler h, SslError e) { h.proceed(); } // DEBUG
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectUnblockScript();
            }
        });

        if (savedInstanceState != null) webView.restoreState(savedInstanceState);
        else webView.loadUrl(START_URL);
    }

    /** Rimuove overlay/modali e riabilita i tocchi **/
    private void injectUnblockScript() {
        String js =
            "(function(){try{"
          + "var sel=['[class*=\"modal\"]','[id*=\"modal\"]','[class*=\"dialog\"]','[class*=\"overlay\"]','[class*=\"backdrop\"]'].join(',');"
          + "document.querySelectorAll(sel).forEach(function(el){"
          + "  var z = parseInt(getComputedStyle(el).zIndex||'0');"
          + "  if (z>100) el.style.display='none';"
          + "});"
          + "// clicca eventuali pulsanti 'Chiudi'"
          + "document.querySelectorAll('button,[role=button]').forEach(function(b){"
          + "  if(/chiudi/i.test(b.textContent||'')) { try{ b.click(); }catch(e){} }"
          + "});"
          + "// forza pointer events e scroll"
          + "var css='*{pointer-events:auto !important;}' + "
          + "'html,body{overflow:auto !important;overscroll-behavior:auto !important;}';"
          + "var st=document.createElement('style'); st.textContent=css; document.head.appendChild(st);"
          + "}catch(e){}})();";
        webView.evaluateJavascript(js, null);
    }

    @Override protected void onSaveInstanceState(Bundle out){ super.onSaveInstanceState(out); webView.saveState(out); }
    @Override public void onBackPressed(){ if (webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}

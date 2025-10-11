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

    // Punta al tuo sito
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
        s.setSupportMultipleWindows(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        // User-Agent mobile “pulito”
        String ua = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36";
        s.setUserAgentString(ua);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setOnTouchListener((v, e) -> { if (!v.hasFocus()) v.requestFocus(); return false; });
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return false; }
            @Override public void onReceivedSslError(WebView v, SslErrorHandler h, SslError e) { h.proceed(); } // solo debug
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectUnblocker(); // <— kill overlay e riattiva i tocchi
            }
        });

        if (savedInstanceState != null) webView.restoreState(savedInstanceState);
        else webView.loadUrl(START_URL);
    }

    /** NASCONDE qualsiasi overlay/modale e forza i tocchi/scroll.  */
    private void injectUnblocker() {
        String js =
            "(function(){try{"
          + " if(window.__plutooKiller){clearInterval(window.__plutooKiller);} "
          + " function kill(){try{"
          + "   var sel=['[class*=\"modal\"]','[id*=\"modal\"]','[class*=\"dialog\"]',"
          + "            '[class*=\"overlay\"]','[class*=\"backdrop\"]','[role=\"dialog\"]'].join(',');"
          + "   document.querySelectorAll(sel).forEach(function(el){"
          + "     var z=parseInt(getComputedStyle(el).zIndex||'0');"
          + "     if(z>100 || /modal|dialog|overlay|backdrop/i.test(el.className+\" \"+(el.id||''))){"
          + "       el.style.setProperty('display','none','important');"
          + "       el.style.setProperty('visibility','hidden','important');"
          + "     }"
          + "   });"
          + "   // Clicka eventuali pulsanti 'Chiudi/Close'"
          + "   document.querySelectorAll('button,[role=button],a').forEach(function(b){"
          + "     var t=(b.textContent||'').trim();"
          + "     if(/chiudi|close|ok/i.test(t)){ try{ b.click(); }catch(e){} }"
          + "   });"
          + "   // Forza tocchi e scroll"
          + "   var st=document.getElementById('plutoo-unblock');"
          + "   if(!st){ st=document.createElement('style'); st.id='plutoo-unblock';"
          + "     st.textContent='*{pointer-events:auto !important;}' + "
          + "       'html,body{overflow:auto !important;touch-action:auto !important;}' + "
          + "       '[class*=\"modal\"],[class*=\"overlay\"],[class*=\"backdrop\"],[role=\"dialog\"]{display:none !important;visibility:hidden !important;}';"
          + "     document.head.appendChild(st);"
          + "   }"
          + " }catch(e){}}"
          + " window.__plutooKiller=setInterval(kill,500); kill();"
          + "}catch(e){}})();";
        webView.evaluateJavascript(js, null);
    }

    @Override protected void onSaveInstanceState(Bundle out){ super.onSaveInstanceState(out); webView.saveState(out); }
    @Override public void onBackPressed(){ if (webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}

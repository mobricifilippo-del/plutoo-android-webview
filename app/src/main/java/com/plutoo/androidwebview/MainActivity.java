package com.plutoo.androidwebview;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private WebView webView;

    // HOME vera dell’app
    private static final String HOME = "https://plutoo-official.vercel.app/";

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setSupportMultipleWindows(true);
        ws.setJavaScriptCanOpenWindowsAutomatically(true);
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE); // niente cache

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new JsBridge(), "AndroidBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String url = r.getUrl().toString();
                // lascia al sistema le app esterne (tel/mail/whatsapp...)
                if (isExternalScheme(url)) return false;
                v.loadUrl(url);
                return true;
            }

            @Override public void onPageStarted(WebView v, String url, Bitmap favicon) {
                // se non è la root del dominio, rientra in HOME
                if (shouldForceHome(url)) {
                    v.stopLoading();
                    v.loadUrl(homeBuster());
                }
            }

            @Override public void onPageFinished(WebView v, String url) {
                // 1) Rimuovi *qualsiasi* SW/Storage che riapre il “Profilo”
                v.evaluateJavascript(
                    "(async function(){try{ " +
                        "if('serviceWorker' in navigator){ " +
                        " const regs=await navigator.serviceWorker.getRegistrations(); regs.forEach(r=>r.unregister()); }" +
                        "caches && caches.keys && caches.keys().then(keys=>keys.forEach(k=>caches.delete(k))); " +
                        "localStorage.clear(); sessionStorage.clear();" +
                        // chiudi overlay/modali che bloccano la UI
                        "[...document.querySelectorAll('button,[role=button],.btn,.button')]" +
                        ".forEach(b=>{const t=(b.innerText||'').trim().toLowerCase(); if(t==='chiudi'||t.includes('chiudi')){try{b.click();}catch(e){}}); " +
                        // nascondi banner col testo “Profilo”
                        "[...document.querySelectorAll('*')]" +
                        ".forEach(n=>{const t=(n.textContent||'').trim(); if(t==='Profilo'){n.style.display='none';}});" +
                    "}catch(e){}})();", null
                );

                // 2) se non è esattamente la HOME, rientra
                if (shouldForceHome(url)) v.loadUrl(homeBuster());
            }
        });

        // Pulizia pesante lato WebView
        webView.clearHistory();
        webView.clearCache(true);
        cm.removeAllCookies(null);
        cm.flush();
        WebStorage.getInstance().deleteAllData();

        if (savedInstanceState != null) webView.restoreState(savedInstanceState);
        else webView.loadUrl(homeBuster());

        // tasto Indietro
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack(); else finish();
            }
        });
    }

    private String homeBuster() {
        return HOME + (HOME.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis() + "#home";
    }
    private static boolean isExternalScheme(String url) {
        try {
            Uri u = Uri.parse(url); String s = u.getScheme(); if (s == null) return false;
            switch (s) { case "tel": case "mailto": case "sms": case "geo": case "intent": case "whatsapp": case "tg": return true; default: return false; }
        } catch (Exception e) { return false; }
    }
    private static boolean shouldForceHome(String url) {
        if (url == null) return false;
        if (!url.startsWith("https://plutoo-official.vercel.app/")) return false;
        String rest = url.replace("https://plutoo-official.vercel.app/", "");
        return !(rest.isEmpty() || rest.startsWith("?") || rest.startsWith("#"));
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        webView.saveState(out);
    }

    public static class JsBridge { @JavascriptInterface public void noop() {} }
}

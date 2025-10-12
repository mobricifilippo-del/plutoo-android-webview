package com.plutoo.androidwebview;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String START_URL = "https://plutoo-official.vercel.app/#home";
    private static final String APP_HOST = "plutoo-official.vercel.app";

    private WebView webView;
    private ProgressBar progress;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        webView  = findViewById(R.id.webview);
        progress = findViewById(R.id.progress);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() != null ? uri.getScheme() : "";

                if (scheme.equals("tel") || scheme.equals("mailto") || scheme.equals("geo")) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (ActivityNotFoundException ignored) {}
                    return true;
                }
                if (scheme.equals("intent")) {
                    try {
                        Intent i = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                        if (i != null) { startActivity(i); return true; }
                    } catch (Exception ignored) {}
                    return true;
                }

                if (scheme.startsWith("http")) {
                    if (APP_HOST.equalsIgnoreCase(uri.getHost())) return false; // resta in app
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (ActivityNotFoundException ignored) {}
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String js =
                        "(function(){try{"
                      + "document.body.style.overflow='auto';"
                      + "var btn=[].slice.call(document.querySelectorAll('button,[role=\"button\"],a'))"
                      + ".find(function(b){var t=(b.innerText||'').trim().toLowerCase();"
                      + "return t==='chiudi'||t==='close';});"
                      + "if(btn){btn.click();}"
                      + "var dialogs=document.querySelectorAll('[role=\"dialog\"],.modal,.ReactModal__Overlay');"
                      + "dialogs.forEach(function(d){d.remove();});"
                      + "var overlays=[].slice.call(document.querySelectorAll('*'))"
                      + ".filter(function(el){var s=getComputedStyle(el);"
                      + "return (s.position==='fixed'||s.position==='sticky') && parseInt(s.zIndex||'0',10)>=1000;});"
                      + "overlays.forEach(function(el){el.parentNode&&el.parentNode.removeChild(el);});"
                      + "}catch(e){}})();";
                view.evaluateJavascript(js, null);

                if (progress != null) progress.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }
        });

        webView.setVisibility(View.INVISIBLE);
        if (progress != null) progress.setVisibility(View.VISIBLE);

        if (savedInstanceState == null) {
            webView.loadUrl(START_URL);
        }
    }

    @Override
    protected void onSaveInstanceState(@Nullable Bundle outState) {
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
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}

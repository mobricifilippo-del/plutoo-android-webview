package com.plutoo.app;

import android.Manifest; import android.annotation.SuppressLint; import android.content.pm.PackageManager; import android.os.Bundle; import android.util.Log; import android.webkit.CookieManager; import android.webkit.GeolocationPermissions; import android.webkit.WebChromeClient; import android.webkit.WebSettings; import android.webkit.WebStorage; import android.webkit.WebView; import android.webkit.WebViewClient;

import androidx.annotation.NonNull; import androidx.appcompat.app.AppCompatActivity; import androidx.core.app.ActivityCompat; import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

private static final String TAG = "PlutooMain";
private WebView webView;
private static final String BASE_URL = "https://plutoo-official.vercel.app/";
private static final int REQUEST_LOCATION = 1001;

@SuppressLint("SetJavaScriptEnabled")
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    try {
        // Layout principale con WebView
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        if (webView == null) {
            throw new IllegalStateException("webView == null (controlla activity_main.xml)");
        }

        // Aiuta il debug via Chrome (puoi rimuoverlo prima del rilascio definitivo)
        WebView.setWebContentsDebuggingEnabled(true);

        setupWebView();
        loadUrl();
        requestLocationPermissionIfNeeded();

    } catch (Exception e) {
        // Qui intercettiamo QUALSIASI errore in onCreate che prima ti chiudeva l'app al volo
        Log.e(TAG, "Fatal error in onCreate", e);
        showFatalErrorPage(e);
    }
}

/**
 * Mostra una pagina HTML di errore dentro una WebView,
 * così invece del flash bianco vedi esattamente cosa è esploso.
 */
@SuppressLint("SetJavaScriptEnabled")
private void showFatalErrorPage(Throwable e) {
    try {
        WebView errorView = new WebView(this);
        WebSettings s = errorView.getSettings();
        s.setJavaScriptEnabled(true);

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta name='viewport' content='width=device-width,initial-scale=1' />");
        sb.append("<style>body{background:#050509;color:#f5f5f5;font-family:system-ui,Roboto,Arial;padding:16px;}" +
                "h1{font-size:20px;margin-bottom:10px;}" +
                "pre{white-space:pre-wrap;background:#111526;border-radius:8px;padding:10px;font-size:12px;}</style>");
        sb.append("</head><body>");
        sb.append("<h1>Plutoo - Errore applicazione</h1>");
        sb.append("<p>L'app ha incontrato un errore interno in <b>MainActivity</b>.</p>");
        sb.append("<p>Mostro il dettaglio tecnico per il debug:</p>");
        sb.append("<pre>");
        sb.append(Log.getStackTraceString(e).replace("<", "&lt;").replace(">", "&gt;"));
        sb.append("</pre>");
        sb.append("<p>Ricompila l'APK dopo il fix.</p>");
        sb.append("</body></html>");

        setContentView(errorView);
        errorView.loadDataWithBaseURL(null, sb.toString(), "text/html", "UTF-8", null);
    } catch (Exception inner) {
        Log.e(TAG, "Error while showing fatal error page", inner);
        // Se anche questo fallisce, non possiamo fare altro
        finish();
    }
}

private void setupWebView() {
    WebSettings settings = webView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setDatabaseEnabled(true);
    settings.setLoadWithOverviewMode(true);
    settings.setUseWideViewPort(true);
    settings.setSupportZoom(false);
    settings.setBuiltInZoomControls(false);
    settings.setDisplayZoomControls(false);
    settings.setMediaPlaybackRequiresUserGesture(false);
    settings.setAllowFileAccess(true);
    settings.setAllowContentAccess(true);
    settings.setCacheMode(WebSettings.LOAD_DEFAULT);
    settings.setGeolocationEnabled(true);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

    // Cookie & storage
    try {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        WebStorage.getInstance();
    } catch (Exception e) {
        Log.w(TAG, "Cookie/WebStorage setup failed", e);
    }

    // Gestione errori di navigazione (se il sito non carica, qui vediamo qualcosa)
    webView.setWebViewClient(new WebViewClient() {
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            Log.e(TAG, "WebView error: " + errorCode + " - " + description + " (" + failingUrl + ")");
        }
    });

    webView.setWebChromeClient(new WebChromeClient() {
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, hasLocationPermission(), false);
        }
    });
}

private void loadUrl() {
    try {
        webView.loadUrl(BASE_URL);
    } catch (Exception e) {
        Log.e(TAG, "loadUrl failed", e);
        showFatalErrorPage(e);
    }
}

private boolean hasLocationPermission() {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
}

private void requestLocationPermissionIfNeeded() {
    if (!hasLocationPermission()) {
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQUEST_LOCATION
        );
    }
}

@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_LOCATION && webView != null) {
        webView.reload();
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

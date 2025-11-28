# WebView / reflection safe
-keep class android.webkit.** { *; }
-dontwarn android.webkit.**

# AppCompat / Material
-keep class androidx.** { *; }
-dontwarn androidx.**

# Keep your Activity - CORRETTO
-keep class com.plutoo.app.MainActivity { *; }
-keep class com.plutoo.app.** { *; }

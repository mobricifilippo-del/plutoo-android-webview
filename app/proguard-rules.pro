# WebView / reflection safe
-keep class android.webkit.** { *; }
-dontwarn android.webkit.**

# AppCompat / Material
-keep class androidx.** { *; }
-dontwarn androidx.**

# Keep your Activity
-keep class com.plutoo.wrappertest.MainActivity { *; }

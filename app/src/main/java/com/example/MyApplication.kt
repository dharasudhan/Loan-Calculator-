package com.example

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            val webViewCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            if (!webViewCacheDir.exists()) {
                webViewCacheDir.mkdirs()
            }
            java.io.File(webViewCacheDir, "js").mkdirs()
            java.io.File(webViewCacheDir, "wasm").mkdirs()
        } catch (e: Exception) {
            // Safe to ignore
        }
    }

    override fun getAttributionTag(): String? {
        return "play-services-ads"
    }
}

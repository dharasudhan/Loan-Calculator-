with open("app/src/main/java/com/example/MyApplication.kt", "r") as f:
    text = f.read()

text = text.replace('java.io.File(webViewCacheDir, "js").mkdir()', 'java.io.File(webViewCacheDir, "js").mkdirs()')
text = text.replace('java.io.File(webViewCacheDir, "wasm").mkdir()', 'java.io.File(webViewCacheDir, "wasm").mkdirs()')

with open("app/src/main/java/com/example/MyApplication.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text2 = f.read()

import re

old_init = """            try {
                MobileAds.initialize(this) {}
                InterstitialAdHelper.loadAd(this)
            } catch (e: Exception) {
                Log.e("MainActivity", "AdMob initialization failed: ${e.message}")
            }"""

new_init = """            try {
                val webViewCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
                java.io.File(webViewCacheDir, "js").mkdirs()
                java.io.File(webViewCacheDir, "wasm").mkdirs()
                MobileAds.initialize(this) {}
                InterstitialAdHelper.loadAd(this)
            } catch (e: Exception) {
                Log.e("MainActivity", "AdMob initialization failed: ${e.message}")
            }"""

text2 = text2.replace(old_init, new_init)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text2)

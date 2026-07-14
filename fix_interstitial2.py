with open("app/src/main/java/com/example/InterstitialAdHelper.kt", "r") as f:
    text = f.read()

old_load = """        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load("""

new_load = """        try {
            val webViewCacheDir = java.io.File(context.cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            java.io.File(webViewCacheDir, "js").mkdirs()
            java.io.File(webViewCacheDir, "wasm").mkdirs()
        } catch (e: Exception) {}
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load("""

text = text.replace(old_load, new_load)

with open("app/src/main/java/com/example/InterstitialAdHelper.kt", "w") as f:
    f.write(text)

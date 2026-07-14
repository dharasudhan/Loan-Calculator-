with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

old_ad = """        androidx.compose.ui.viewinterop.AndroidView(
          modifier = Modifier.fillMaxWidth().height(50.dp),
          factory = { ctx ->
            com.google.android.gms.ads.AdView(ctx).apply {"""

new_ad = """        androidx.compose.ui.viewinterop.AndroidView(
          modifier = Modifier.fillMaxWidth().height(50.dp),
          factory = { ctx ->
            try {
                val webViewCacheDir = java.io.File(ctx.cacheDir, "WebView/Default/HTTP Cache/Code Cache")
                java.io.File(webViewCacheDir, "js").mkdirs()
                java.io.File(webViewCacheDir, "wasm").mkdirs()
            } catch (e: Exception) {}
            com.google.android.gms.ads.AdView(ctx).apply {"""

text = text.replace(old_ad, new_ad)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

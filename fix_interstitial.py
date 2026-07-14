import re

with open("app/src/main/java/com/example/InterstitialAdHelper.kt", "r") as f:
    text = f.read()

old_load = """    fun loadAd(context: Context) {
        if (mInterstitialAd != null || isAdLoading) {
            return
        }"""

new_load = """    fun loadAd(context: Context) {
        if (!AdConfig.canRequestAds.value || mInterstitialAd != null || isAdLoading) {
            return
        }"""

text = text.replace(old_load, new_load)

with open("app/src/main/java/com/example/InterstitialAdHelper.kt", "w") as f:
    f.write(text)

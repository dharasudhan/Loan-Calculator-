import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

old_init = """    consentManager.gatherConsent(this) {
        if (!isMobileAdsInitializeCalled) {
            isMobileAdsInitializeCalled = true
            try {
                MobileAds.initialize(this) {}
                InterstitialAdHelper.loadAd(this)
            } catch (e: Exception) {
                Log.e("MainActivity", "AdMob initialization failed: ${e.message}")
            }
        }
    }"""

new_init = """    consentManager.gatherConsent(this) {
        if (!isMobileAdsInitializeCalled) {
            isMobileAdsInitializeCalled = true
            AdConfig.canRequestAds.value = true
            try {
                MobileAds.initialize(this) {}
                InterstitialAdHelper.loadAd(this)
            } catch (e: Exception) {
                Log.e("MainActivity", "AdMob initialization failed: ${e.message}")
            }
        }
    }"""

text = text.replace(old_init, new_init)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

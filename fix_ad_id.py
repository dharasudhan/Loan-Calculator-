import re
with open("app/src/main/java/com/example/InterstitialAdHelper.kt", "r") as f:
    text = f.read()

text = text.replace('private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"', '')
text = text.replace('// Standard Google test interstitial ad unit ID', '')
text = text.replace('AD_UNIT_ID', 'context.getString(R.string.admob_interstitial_id)')

with open("app/src/main/java/com/example/InterstitialAdHelper.kt", "w") as f:
    f.write(text)

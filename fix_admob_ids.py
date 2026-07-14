with open("app/src/main/res/values/strings.xml", "r") as f:
    text = f.read()

text = text.replace('ca-app-pub-3940256099942544~3347511713', 'ca-app-pub-6141525725546605~5592730265')
text = text.replace('ca-app-pub-3940256099942544/6300978111', 'ca-app-pub-6141525725546605/1664525320')
text = text.replace('ca-app-pub-3940256099942544/1033173712', 'ca-app-pub-6141525725546605/8455651615')

# We can also clean up the comments so they don't say "Replace this" anymore
text = text.replace('<!-- Replace this with your actual AdMob App ID -->', '<!-- Actual AdMob App ID -->')
text = text.replace('<!-- Replace this with your actual Banner Ad Unit ID -->', '<!-- Actual Banner Ad Unit ID -->')
text = text.replace('<!-- Replace this with your actual Interstitial Ad Unit ID -->', '<!-- Actual Interstitial Ad Unit ID -->')

with open("app/src/main/res/values/strings.xml", "w") as f:
    f.write(text)

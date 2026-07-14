import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

old_banner = """fun BannerAdComponent(
  viewModel: LoanCalculatorViewModel,
  onRemoveAdsClick: () -> Unit
) {
  val isAdFree by viewModel.isAdFreeVersion.collectAsState()
  if (isAdFree) return"""

new_banner = """fun BannerAdComponent(
  viewModel: LoanCalculatorViewModel,
  onRemoveAdsClick: () -> Unit
) {
  val isAdFree by viewModel.isAdFreeVersion.collectAsState()
  val canRequestAds by AdConfig.canRequestAds.collectAsState()
  if (isAdFree || !canRequestAds) return"""

text = text.replace(old_banner, new_banner)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

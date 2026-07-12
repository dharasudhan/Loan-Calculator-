with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

import re
text = text.replace('    enableEdgeToEdge()', '    enableEdgeToEdge()\n    \n    AppRatingManager.trackAppOpen(this)\n    AppRatingManager.maybeRequestRating(this)')

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/LoanCalculatorViewModel.kt", "r") as f:
    text2 = f.read()

text2 = text2.replace('private fun recalculate() {', 'private fun recalculate() {\n        AppRatingManager.trackCalculation(getApplication())\n')

with open("app/src/main/java/com/example/LoanCalculatorViewModel.kt", "w") as f:
    f.write(text2)

with open("app/src/main/java/com/example/LoanCalculatorViewModel.kt", "r") as f:
    text = f.read()

import re

# 1. Add import
if "import kotlinx.coroutines.launch" not in text:
    text = text.replace("import kotlinx.coroutines.flow.asStateFlow", "import kotlinx.coroutines.flow.asStateFlow\nimport kotlinx.coroutines.launch")

# 2, 3, 4, 5
text = text.replace("variableRateFixedPeriod.value", "variablePeriodYears.value")
text = text.replace("variableRateAdjustment.value", "subsequentAdjustRate.value")
text = text.replace("debtsList.value = emptyList()", "_debtsList.value = emptyList()")
text = text.replace("saveDebtPlannerData()", "saveDebtsToPrefs(_debtsList.value)\n            saveInputsToPrefs()")

# 6
text = text.replace("saveToPrefs()", "saveInputsToPrefs()")

with open("app/src/main/java/com/example/LoanCalculatorViewModel.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

import re
# Remove all the bad LaunchedEffects
bad_effect_pattern = r'  LaunchedEffect\(Unit\) \{\n      viewModel\.clearDataEvent\.collect \{\n          homePriceInput = ""\n          downPaymentInput = ""\n          loanAmtInput = ""\n          intRateInput = ""\n          loanTermInput = ""\n          extraPaymentInput = ""\n          propTaxInput = ""\n          insInput = ""\n          pmiInput = ""\n          margTaxInput = ""\n      \}\n  \}'

text = re.sub(bad_effect_pattern, "", text)

# Remove DebtPlanner bad effect
debt_effect_pattern = r'  LaunchedEffect\(Unit\) \{\n      viewModel\.clearDataEvent\.collect \{\n          newDebtName = ""\n          newDebtBalance = ""\n          newDebtIntRate = ""\n          newDebtMinPay = ""\n      \}\n  \}'

text = re.sub(debt_effect_pattern, "", text)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

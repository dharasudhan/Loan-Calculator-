with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

import re
pattern = r'(var margTaxInput by remember \{ mutableStateOf\(viewModel\.marginalTaxRate\.value\) \}\n  var activeHelpType by remember \{ mutableStateOf<HelpType\?>\(null\) \})'

replacement = r'''\1

  androidx.compose.runtime.LaunchedEffect(Unit) {
      viewModel.clearDataEvent.collect {
          homePriceInput = ""
          downPaymentInput = ""
          loanAmtInput = ""
          intRateInput = ""
          loanTermInput = ""
          extraPaymentInput = ""
          propTaxInput = ""
          insInput = ""
          pmiInput = ""
          margTaxInput = ""
      }
  }'''
text = re.sub(pattern, replacement, text)

# Now DebtPlannerTab
debt_pattern = r'(var newDebtMinPay by remember \{ mutableStateOf\(""\) \})'
debt_replacement = r'''\1

  androidx.compose.runtime.LaunchedEffect(Unit) {
      viewModel.clearDataEvent.collect {
          newDebtName = ""
          newDebtBalance = ""
          newDebtIntRate = ""
          newDebtMinPay = ""
      }
  }'''
text = re.sub(debt_pattern, debt_replacement, text)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

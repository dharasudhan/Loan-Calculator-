import re
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

btn_pattern = r'context\.getSharedPreferences\("loan_prefs", android\.content\.Context\.MODE_PRIVATE\)\.edit\(\)\.clear\(\)\.apply\(\)\s+android\.widget\.Toast\.makeText\(context, "Data Cleared", android\.widget\.Toast\.LENGTH_SHORT\)\.show\(\)'
new_btn = '''viewModel.clearAllData()
                        android.widget.Toast.makeText(context, "Data Cleared", android.widget.Toast.LENGTH_SHORT).show()'''

text = re.sub(btn_pattern, new_btn, text)

# Add LaunchedEffect to MainContent
launched_effect = '''  val customCurrency by viewModel.customCurrencySymbol.collectAsState()

  LaunchedEffect(Unit) {
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

text = text.replace('  val customCurrency by viewModel.customCurrencySymbol.collectAsState()', launched_effect)

# DebtPlannerTab LaunchedEffect
debt_effect = '''  var newDebtMinPay by remember { mutableStateOf("") }

  LaunchedEffect(Unit) {
      viewModel.clearDataEvent.collect {
          newDebtName = ""
          newDebtBalance = ""
          newDebtIntRate = ""
          newDebtMinPay = ""
      }
  }'''
text = text.replace('  var newDebtMinPay by remember { mutableStateOf("") }', debt_effect)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

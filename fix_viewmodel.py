import re
with open("app/src/main/java/com/example/LoanCalculatorViewModel.kt", "r") as f:
    text = f.read()

# Add clearDataEvent and clearAllData
if "val clearDataEvent =" not in text:
    text = text.replace('class LoanCalculatorViewModel(application: Application) : AndroidViewModel(application) {', '''class LoanCalculatorViewModel(application: Application) : AndroidViewModel(application) {
    val clearDataEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    
    fun clearAllData() {
        viewModelScope.launch {
            prefs.edit().clear().apply()
            prefs.edit().putBoolean("isFirstTime", false).apply()
            
            loanType.value = "Mortgage"
            homePrice.value = ""
            downPayment.value = ""
            loanAmountInput.value = ""
            interestRate.value = ""
            loanTermYears.value = ""
            extraPayment.value = ""
            propertyTaxRate.value = ""
            homeInsurance.value = ""
            pmiRate.value = ""
            marginalTaxRate.value = ""
            
            isVariableRateEnabled.value = false
            variableRateFixedPeriod.value = ""
            variableRateAdjustment.value = ""
            
            debtsList.value = emptyList()
            debtPlannerBudget.value = ""
            saveDebtPlannerData()
            
            _currentLanguage.value = LanguageCode.EN
            _customCurrencySymbol.value = null
            colorTheme.value = "blue"

            clearDataEvent.emit(Unit)
            recalculate()
            recalculateComparison()
            recalculateDebtPlanner()
        }
    }
''')

# Modify loadFromPrefs
load_pattern = r'private fun loadFromPrefs\(\) \{[\s\S]*?val savedLangStr = prefs\.getString\("current_language", "EN"\) \?: "EN"'
new_load = '''private fun loadFromPrefs() {
        val isFirstTime = prefs.getBoolean("isFirstTime", true)
        if (isFirstTime) {
            prefs.edit().putBoolean("isFirstTime", false).apply()
            loanType.value = "Mortgage"
            homePrice.value = "350000"
            downPayment.value = "70000"
            loanAmountInput.value = "280000"
            interestRate.value = "6.5"
            loanTermYears.value = "30"
            extraPayment.value = "0"
            propertyTaxRate.value = "1.2"
            homeInsurance.value = "1200"
            pmiRate.value = "0.5"
            marginalTaxRate.value = "24"
            saveToPrefs()
        } else {
            loanType.value = prefs.getString("loanType", "Mortgage") ?: "Mortgage"
            homePrice.value = prefs.getString("homePrice", "") ?: ""
            downPayment.value = prefs.getString("downPayment", "") ?: ""
            loanAmountInput.value = prefs.getString("loanAmountInput", "") ?: ""
            interestRate.value = prefs.getString("interestRate", "") ?: ""
            loanTermYears.value = prefs.getString("loanTermYears", "") ?: ""
            extraPayment.value = prefs.getString("extraPayment", "") ?: ""
            propertyTaxRate.value = prefs.getString("propertyTaxRate", "") ?: ""
            homeInsurance.value = prefs.getString("homeInsurance", "") ?: ""
            pmiRate.value = prefs.getString("pmiRate", "") ?: ""
            marginalTaxRate.value = prefs.getString("marginalTaxRate", "") ?: ""
        }
        
        colorTheme.value = prefs.getString("color_theme", "blue") ?: "blue"
        
        val savedLangStr = prefs.getString("current_language", "EN") ?: "EN"'''

text = re.sub(load_pattern, new_load, text)

with open("app/src/main/java/com/example/LoanCalculatorViewModel.kt", "w") as f:
    f.write(text)

package com.example

import android.app.Application
import android.app.Activity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class AmortizationItem(
    val monthNumber: Int,
    val yearNumber: Int,
    val paymentAmount: Double,
    val principalPaid: Double,
    val interestPaid: Double,
    val extraPayment: Double,
    val remainingBalance: Double,
    val totalInterestToDate: Double
)

data class AmortizationYearlyItem(
    val yearNumber: Int,
    val paymentAmount: Double,
    val principalPaid: Double,
    val interestPaid: Double,
    val extraPayment: Double,
    val endingBalance: Double
)

data class AnnualTaxSavingsItem(
    val yearNumber: Int,
    val interestPaid: Double,
    val estimatedTaxSavings: Double
)

data class Debt(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val balance: Double,
    val interestRate: Double,
    val minimumPayment: Double
)

data class DebtPayoffMonth(
    val monthNumber: Int,
    val totalRemainingBalance: Double,
    val totalInterestPaidThisMonth: Double,
    val payments: Map<String, Double>, // debtId -> payment amount
    val balances: Map<String, Double>  // debtId -> remaining balance
)

data class DebtPlannerResult(
    val isValid: Boolean = false,
    val totalStartingDebt: Double = 0.0,
    val debtFreeMonths: Int = 0,
    val totalInterestPaid: Double = 0.0,
    val monthlyProjection: List<DebtPayoffMonth> = emptyList(),
    val timeSavedMonths: Int = 0,
    val interestSaved: Double = 0.0
)

class LoanCalculatorViewModel(application: Application) : AndroidViewModel(application) {
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
            variablePeriodYears.value = ""
            subsequentAdjustRate.value = ""
            
            _debtsList.value = emptyList()
            debtPlannerBudget.value = ""
            saveDebtsToPrefs(_debtsList.value)
            saveInputsToPrefs()
            
            _currentLanguage.value = LanguageCode.EN
            _customCurrencySymbol.value = null
            colorTheme.value = "blue"

            clearDataEvent.emit(Unit)
            recalculate()
            recalculateComparison()
            recalculateDebtPlanner()
        }
    }


    private val prefs = application.getSharedPreferences("loan_calculator_prefs", Application.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // Play Billing Manager
    val billingManager = PlayBillingManager(application, viewModelScope) { isPurchased ->
        isAdFreeVersion.value = isPurchased
        prefs.edit().putBoolean("isAdFreeVersion", isPurchased).apply()
    }

    // Localization
    private val _currentLanguage = MutableStateFlow(LanguageCode.EN)
    val currentLanguage: StateFlow<LanguageCode> = _currentLanguage.asStateFlow()

    fun setLanguage(lang: LanguageCode) {
        _currentLanguage.value = lang
        prefs.edit().putString("current_language", lang.name).apply()
    }

    // Custom Currency
    private val _customCurrencySymbol = MutableStateFlow<String?>(null)
    val customCurrencySymbol: StateFlow<String?> = _customCurrencySymbol.asStateFlow()

    fun setCustomCurrencySymbol(symbol: String?) {
        _customCurrencySymbol.value = symbol
        prefs.edit().putString("custom_currency_symbol", symbol).apply()
    }

    // Color Theme ("blue", "red", "green", "yellow")
    val colorTheme = MutableStateFlow("blue")

    fun setColorTheme(theme: String) {
        colorTheme.value = theme
        prefs.edit().putString("color_theme", theme).apply()
    }

    // Input States
    val loanType = MutableStateFlow("Mortgage") // "Mortgage", "Personal" -> Merged Personal Loan & Auto Loan
    val homePrice = MutableStateFlow("0")
    val downPayment = MutableStateFlow("0")
    val loanAmountInput = MutableStateFlow("0") // direct loan amount for general loans
    val interestRate = MutableStateFlow("0")
    val loanTermYears = MutableStateFlow("0")
    val extraPayment = MutableStateFlow("0")
    val propertyTaxRate = MutableStateFlow("0") // Annual % rate
    val homeInsurance = MutableStateFlow("0") // Annual total $
    val pmiRate = MutableStateFlow("0") // Annual % rate
    val marginalTaxRate = MutableStateFlow("0") // Kept for backwards compatibility but not user-visible

    // Variable / Adjustable Rate Scenarios (ARM)
    val isVariableRateEnabled = MutableStateFlow(false)
    val variablePeriodYears = MutableStateFlow("0") // Period after which rate changes (e.g., 5, 7, 10 years)
    val subsequentAdjustRate = MutableStateFlow("0") // e.g. +1.5%

    // Calculations Flow Outputs
    private val _calculationResult = MutableStateFlow(CalculationResult())
    val calculationResult: StateFlow<CalculationResult> = _calculationResult.asStateFlow()

    // Loan Comparison Settings
    val isComparisonActive = MutableStateFlow(false)
    val comparisonLoanAmount = MutableStateFlow("0")
    val comparisonDownPayment = MutableStateFlow("0")
    val comparisonInterestRate = MutableStateFlow("0")
    val comparisonLoanTermYears = MutableStateFlow("0")
    val comparisonExtraPayment = MutableStateFlow("0")

    private val _comparisonResult = MutableStateFlow(CalculationResult())
    val comparisonResult: StateFlow<CalculationResult> = _comparisonResult.asStateFlow()

    // Debt Planner Flow Outputs
    private val _debtsList = MutableStateFlow<List<Debt>>(emptyList())
    val debtsList: StateFlow<List<Debt>> = _debtsList.asStateFlow()

    val debtPlannerBudget = MutableStateFlow("0")
    val debtPayoffStrategy = MutableStateFlow("Snowball") // "Snowball", "Avalanche"

    private val _debtPlannerResult = MutableStateFlow(DebtPlannerResult())
    val debtPlannerResult: StateFlow<DebtPlannerResult> = _debtPlannerResult.asStateFlow()

    // Local Saved Debt Scenarios List
    private val _savedPlannerScenarios = MutableStateFlow<List<DebtPlannerScenario>>(emptyList())
    val savedPlannerScenarios: StateFlow<List<DebtPlannerScenario>> = _savedPlannerScenarios.asStateFlow()

    // Ad-based Unlock States
    val isComparisonUnlocked = MutableStateFlow(false)
    val isDebtPlannerUnlocked = MutableStateFlow(false)
    val isRentVsBuyUnlocked = MutableStateFlow(false)
    val isAdFreeVersion = MutableStateFlow(false)

    init {
        loadFromPrefs()
        recalculate()
        recalculateComparison()
        recalculateDebtPlanner()
    }

    private fun loadFromPrefs() {
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
            saveInputsToPrefs()
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
        
        val savedLangStr = prefs.getString("current_language", "EN") ?: "EN"
        _currentLanguage.value = try {
            LanguageCode.valueOf(savedLangStr)
        } catch (e: Exception) {
            LanguageCode.EN
        }
        _customCurrencySymbol.value = prefs.getString("custom_currency_symbol", null)

        isVariableRateEnabled.value = prefs.getBoolean("isVariableRateEnabled", false)
        variablePeriodYears.value = prefs.getString("variablePeriodYears", "0") ?: "0"
        subsequentAdjustRate.value = prefs.getString("subsequentAdjustRate", "0") ?: "0"

        isComparisonActive.value = prefs.getBoolean("isComparisonActive", false)
        comparisonLoanAmount.value = prefs.getString("comparisonLoanAmount", "0") ?: "0"
        comparisonDownPayment.value = prefs.getString("comparisonDownPayment", "0") ?: "0"
        comparisonInterestRate.value = prefs.getString("comparisonInterestRate", "0") ?: "0"
        comparisonLoanTermYears.value = prefs.getString("comparisonLoanTermYears", "0") ?: "0"
        comparisonExtraPayment.value = prefs.getString("comparisonExtraPayment", "0") ?: "0"

        // Load Debts
        val debtsJson = prefs.getString("debts_json", null)
        if (debtsJson != null) {
            try {
                val listType = Types.newParameterizedType(List::class.java, Debt::class.java)
                val adapter = moshi.adapter<List<Debt>>(listType)
                val decoded = adapter.fromJson(debtsJson)
                if (decoded != null) {
                    _debtsList.value = decoded
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Load Saved Debt Scenarios
        val scenariosJson = prefs.getString("saved_scenarios_json", null)
        if (scenariosJson != null) {
            try {
                val listType = Types.newParameterizedType(List::class.java, DebtPlannerScenario::class.java)
                val adapter = moshi.adapter<List<DebtPlannerScenario>>(listType)
                val decoded = adapter.fromJson(scenariosJson)
                if (decoded != null) {
                    _savedPlannerScenarios.value = decoded
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        debtPlannerBudget.value = prefs.getString("planner_budget", "0") ?: "0"
        debtPayoffStrategy.value = prefs.getString("payoff_strategy", "Snowball") ?: "Snowball"

        isComparisonUnlocked.value = false
        isDebtPlannerUnlocked.value = false
        isRentVsBuyUnlocked.value = false
        isAdFreeVersion.value = prefs.getBoolean("isAdFreeVersion", false)
    }

    fun purchaseAdFree() {
        isAdFreeVersion.value = true
        prefs.edit().putBoolean("isAdFreeVersion", true).apply()
    }

    fun purchaseAdFreeReal(activity: Activity, onFallbackSimulation: () -> Unit) {
        billingManager.launchPurchaseFlow(activity, onFallbackSimulation)
    }

    fun resetAdFree() {
        isAdFreeVersion.value = false
        prefs.edit().putBoolean("isAdFreeVersion", false).apply()
    }

    fun unlockComparisonFeature() {
        isComparisonUnlocked.value = true
        prefs.edit().putBoolean("isComparisonUnlocked", true).apply()
    }

    fun lockComparisonFeature() {
        isComparisonUnlocked.value = false
        prefs.edit().putBoolean("isComparisonUnlocked", false).apply()
    }

    fun unlockDebtPlannerFeature() {
        isDebtPlannerUnlocked.value = true
        prefs.edit().putBoolean("isDebtPlannerUnlocked", true).apply()
    }

    fun lockDebtPlannerFeature() {
        isDebtPlannerUnlocked.value = false
        prefs.edit().putBoolean("isDebtPlannerUnlocked", false).apply()
    }

    fun unlockRentVsBuyFeature() {
        isRentVsBuyUnlocked.value = true
        prefs.edit().putBoolean("isRentVsBuyUnlocked", true).apply()
    }

    fun lockRentVsBuyFeature() {
        isRentVsBuyUnlocked.value = false
        prefs.edit().putBoolean("isRentVsBuyUnlocked", false).apply()
    }

    private fun saveInputsToPrefs() {
        prefs.edit().apply {
            putString("loanType", loanType.value)
            putString("homePrice", homePrice.value)
            putString("downPayment", downPayment.value)
            putString("loanAmountInput", loanAmountInput.value)
            putString("interestRate", interestRate.value)
            putString("loanTermYears", loanTermYears.value)
            putString("extraPayment", extraPayment.value)
            putString("propertyTaxRate", propertyTaxRate.value)
            putString("homeInsurance", homeInsurance.value)
            putString("pmiRate", pmiRate.value)
            
            putBoolean("isVariableRateEnabled", isVariableRateEnabled.value)
            putString("variablePeriodYears", variablePeriodYears.value)
            putString("subsequentAdjustRate", subsequentAdjustRate.value)

            putBoolean("isComparisonActive", isComparisonActive.value)
            putString("comparisonLoanAmount", comparisonLoanAmount.value)
            putString("comparisonDownPayment", comparisonDownPayment.value)
            putString("comparisonInterestRate", comparisonInterestRate.value)
            putString("comparisonLoanTermYears", comparisonLoanTermYears.value)
            putString("comparisonExtraPayment", comparisonExtraPayment.value)

            putString("planner_budget", debtPlannerBudget.value)
            putString("payoff_strategy", debtPayoffStrategy.value)
            apply()
        }
    }

    private fun saveDebtsToPrefs(debts: List<Debt>) {
        try {
            val listType = Types.newParameterizedType(List::class.java, Debt::class.java)
            val adapter = moshi.adapter<List<Debt>>(listType)
            val json = adapter.toJson(debts)
            prefs.edit().putString("debts_json", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateInputs() {
        saveInputsToPrefs()
        recalculate()
        recalculateComparison()
    }

    fun updateVariableRate(enabled: Boolean, period: String, adjustment: String) {
        isVariableRateEnabled.value = enabled
        variablePeriodYears.value = period
        subsequentAdjustRate.value = adjustment
        saveInputsToPrefs()
        recalculate()
    }

    fun updateComparison(enabled: Boolean, amount: String, downPaymentVal: String, rate: String, term: String, extraAmt: String) {
        isComparisonActive.value = enabled
        comparisonLoanAmount.value = amount
        comparisonDownPayment.value = downPaymentVal
        comparisonInterestRate.value = rate
        comparisonLoanTermYears.value = term
        comparisonExtraPayment.value = extraAmt
        saveInputsToPrefs()
        recalculateComparison()
    }

    fun saveCurrentPlannerScenario(name: String) {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val newScenario = DebtPlannerScenario(
            id = java.util.UUID.randomUUID().toString(),
            name = name.ifEmpty { "Plan Draft $date" },
            dateCreated = date,
            debts = _debtsList.value,
            budget = debtPlannerBudget.value,
            strategy = debtPayoffStrategy.value
        )
        val list = _savedPlannerScenarios.value + newScenario
        _savedPlannerScenarios.value = list
        saveScenariosToPrefs(list)
    }

    fun restorePlannerScenario(scenario: DebtPlannerScenario) {
        _debtsList.value = scenario.debts
        debtPlannerBudget.value = scenario.budget
        debtPayoffStrategy.value = scenario.strategy
        saveDebtsToPrefs(scenario.debts)
        saveInputsToPrefs()
        recalculateDebtPlanner()
    }

    fun deletePlannerScenario(id: String) {
        val list = _savedPlannerScenarios.value.filter { it.id != id }
        _savedPlannerScenarios.value = list
        saveScenariosToPrefs(list)
    }

    private fun saveScenariosToPrefs(list: List<DebtPlannerScenario>) {
        try {
            val listType = Types.newParameterizedType(List::class.java, DebtPlannerScenario::class.java)
            val adapter = moshi.adapter<List<DebtPlannerScenario>>(listType)
            val json = adapter.toJson(list)
            prefs.edit().putString("saved_scenarios_json", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addDebt(name: String, balance: Double, interestRate: Double, minimumPayment: Double) {
        val newDebt = Debt(name = name, balance = balance, interestRate = interestRate, minimumPayment = minimumPayment)
        val updatedList = _debtsList.value + newDebt
        _debtsList.value = updatedList
        saveDebtsToPrefs(updatedList)
        recalculateDebtPlanner()
    }

    fun deleteDebt(id: String) {
        val updatedList = _debtsList.value.filter { it.id != id }
        _debtsList.value = updatedList
        saveDebtsToPrefs(updatedList)
        recalculateDebtPlanner()
    }

    fun updatePlannerBudget(budget: String) {
        debtPlannerBudget.value = budget
        saveInputsToPrefs()
        recalculateDebtPlanner()
    }

    fun updateStrategy(strategy: String) {
        debtPayoffStrategy.value = strategy
        saveInputsToPrefs()
        recalculateDebtPlanner()
    }

    private fun recalculateDebtPlanner() {
        val debts = _debtsList.value
        if (debts.isEmpty()) {
            _debtPlannerResult.value = DebtPlannerResult(isValid = false)
            return
        }

        val totalBudget = debtPlannerBudget.value.parseToDoubleOrNull() ?: 0.0
        val strategy = debtPayoffStrategy.value

        // 1. Run principal simulation inside selected strategy
        val strategySim = runSimulation(debts, totalBudget, strategy)

        // 2. Run baseline minimum payment simulation to calculate comparison analytics
        val minSum = debts.sumOf { it.minimumPayment }
        val baselineSim = runSimulation(debts, minSum, "Minimum")

        val totalStarting = debts.sumOf { it.balance }
        val monthsSaved = max(0, baselineSim.debtFreeMonths - strategySim.debtFreeMonths)
        val interestSaved = max(0.0, baselineSim.totalInterestPaid - strategySim.totalInterestPaid)

        _debtPlannerResult.value = DebtPlannerResult(
            isValid = true,
            totalStartingDebt = totalStarting,
            debtFreeMonths = strategySim.debtFreeMonths,
            totalInterestPaid = strategySim.totalInterestPaid,
            monthlyProjection = strategySim.monthlyProjection,
            timeSavedMonths = monthsSaved,
            interestSaved = interestSaved
        )
    }

    private data class SimulationDetails(
        val debtFreeMonths: Int,
        val totalInterestPaid: Double,
        val monthlyProjection: List<DebtPayoffMonth>
    )

    private fun runSimulation(
        startingDebts: List<Debt>,
        monthlyBudget: Double,
        strategy: String
    ): SimulationDetails {
        val activeDebts = startingDebts.map { it.copy() }.toMutableList()
        var totalInterestPaid = 0.0
        val monthlyProjection = mutableListOf<DebtPayoffMonth>()
        var monthCounter = 0
        
        val currentBalances = activeDebts.associate { it.id to it.balance }.toMutableMap()

        // Loop until paid off or safety cap at 50 years max (600 months)
        while (currentBalances.values.any { it > 0.0 } && monthCounter < 600) {
            monthCounter++
            
            var interestThisMonthTotal = 0.0
            val paymentsThisMonth = mutableMapOf<String, Double>()
            
            // 1. Calculate and add interest
            for (debt in activeDebts) {
                val bal = currentBalances[debt.id] ?: 0.0
                if (bal > 0.0) {
                    val interestCharge = bal * (debt.interestRate / 100.0 / 12.0)
                    currentBalances[debt.id] = bal + interestCharge
                    interestThisMonthTotal += interestCharge
                }
            }
            
            // 2. Setup minimum payments capping at remaining balances
            val minPaymentsToDebts = mutableMapOf<String, Double>()
            var sumOfAllMinsNeededOnActive = 0.0
            
            for (debt in activeDebts) {
                val bal = currentBalances[debt.id] ?: 0.0
                if (bal > 0.0) {
                    val requiredMin = min(debt.minimumPayment, bal)
                    minPaymentsToDebts[debt.id] = requiredMin
                    sumOfAllMinsNeededOnActive += requiredMin
                }
            }
            
            // Auto cover required minimums if user budget lies below
            val effectiveBudget = max(monthlyBudget, sumOfAllMinsNeededOnActive)
            
            // Apply minimum payment deductions
            for (debt in activeDebts) {
                val bal = currentBalances[debt.id] ?: 0.0
                if (bal > 0.0) {
                    val payment = minPaymentsToDebts[debt.id] ?: 0.0
                    currentBalances[debt.id] = bal - payment
                    paymentsThisMonth[debt.id] = payment
                }
            }
            
            // 3. Allocate extra funds according to elected Snowball or Avalanche strategy
            var extraMoney = effectiveBudget - sumOfAllMinsNeededOnActive
            if (extraMoney > 0.0) {
                val sortedDebts = when (strategy) {
                    "Snowball" -> activeDebts.filter { (currentBalances[it.id] ?: 0.0) > 0.0 }
                        .sortedBy { currentBalances[it.id] ?: 0.0 }
                    "Avalanche" -> activeDebts.filter { (currentBalances[it.id] ?: 0.0) > 0.0 }
                        .sortedByDescending { it.interestRate }
                    else -> activeDebts.filter { (currentBalances[it.id] ?: 0.0) > 0.0 }
                }
                
                for (debt in sortedDebts) {
                    val bal = currentBalances[debt.id] ?: 0.0
                    if (bal > 0.0 && extraMoney > 0.0) {
                        val additionalPayment = min(extraMoney, bal)
                        currentBalances[debt.id] = bal - additionalPayment
                        paymentsThisMonth[debt.id] = (paymentsThisMonth[debt.id] ?: 0.0) + additionalPayment
                        extraMoney -= additionalPayment
                    }
                }
            }
            
            totalInterestPaid += interestThisMonthTotal
            
            monthlyProjection.add(
                DebtPayoffMonth(
                    monthNumber = monthCounter,
                    totalRemainingBalance = currentBalances.values.sum(),
                    totalInterestPaidThisMonth = interestThisMonthTotal,
                    payments = paymentsThisMonth.toMap(),
                    balances = currentBalances.toMap()
                )
            )
        }

        return SimulationDetails(
            debtFreeMonths = monthCounter,
            totalInterestPaid = totalInterestPaid,
            monthlyProjection = monthlyProjection
        )
    }

    private fun recalculate() {
        AppRatingManager.trackCalculation(getApplication())

        val currentType = loanType.value
        val hp = homePrice.value.parseToDoubleOrNull() ?: 0.0
        val dp = downPayment.value.parseToDoubleOrNull() ?: 0.0
        
        // Principal Loan Amount
        val p = if (currentType == "Mortgage") {
            max(0.0, hp - dp)
        } else {
            loanAmountInput.value.parseToDoubleOrNull() ?: 0.0
        }

        val annualRate = interestRate.value.parseToDoubleOrNull() ?: 0.0
        val years = loanTermYears.value.toIntOrNull() ?: 0
        val extra = extraPayment.value.parseToDoubleOrNull() ?: 0.0

        val rTax = propertyTaxRate.value.parseToDoubleOrNull() ?: 0.0
        val insAnnual = homeInsurance.value.parseToDoubleOrNull() ?: 0.0
        val rPmi = pmiRate.value.parseToDoubleOrNull() ?: 0.0
        val taxRate = marginalTaxRate.value.parseToDoubleOrNull() ?: 0.0

        if (p <= 0.0 || annualRate < 0.0 || years <= 0) {
            _calculationResult.value = CalculationResult(isValid = false)
            return
        }

        val totalMonths = years * 12
        val monthlyRate = annualRate / 12.0 / 100.0

        // Calculate Base P&I Monthly Payment
        val monthlyPi = if (monthlyRate > 0.0) {
            p * (monthlyRate * (1.0 + monthlyRate).pow(totalMonths.toDouble())) /
                    ((1.0 + monthlyRate).pow(totalMonths.toDouble()) - 1.0)
        } else {
            p / totalMonths
        }

        // Additional Mortgage Costs
        val monthlyTax = if (currentType == "Mortgage") (hp * (rTax / 100.0)) / 12.0 else 0.0
        val monthlyIns = if (currentType == "Mortgage") insAnnual / 12.0 else 0.0
        
        // PMI generally applies if down payment is less than 20% of home value
        val needsPmi = currentType == "Mortgage" && dp < (hp * 0.20)
        val monthlyPmi = if (needsPmi) (p * (rPmi / 100.0)) / 12.0 else 0.0

        val otherMonthlyExpenses = monthlyTax + monthlyIns + monthlyPmi
        val totalMonthlyPaymentNoExtras = monthlyPi + otherMonthlyExpenses

        // Variable Rate Scenario Details
        val variableYrs = variablePeriodYears.value.toIntOrNull() ?: 5
        val rateAdjustment = subsequentAdjustRate.value.parseToDoubleOrNull() ?: 1.5
        val isVarEnabled = isVariableRateEnabled.value
        val varLimitMonths = variableYrs * 12
        var hasAdjusted = false

        var activeMonthlyRate = monthlyRate
        var activeMonthlyPi = monthlyPi

        // Generate Amortization Schedule
        val schedule = mutableListOf<AmortizationItem>()
        var balance = p
        var totalInterestToDate = 0.0
        var monthCounter = 0

        while (balance > 0.01 && monthCounter < 600) { // Limit to 50 years max (600 months)
            monthCounter++
            val currentYear = ((monthCounter - 1) / 12) + 1
            
            if (isVarEnabled && monthCounter > varLimitMonths && !hasAdjusted) {
                hasAdjusted = true
                val newRateVal = annualRate + rateAdjustment
                activeMonthlyRate = newRateVal / 12.0 / 100.0
                val remainingMonths = max(1, totalMonths - varLimitMonths)
                activeMonthlyPi = if (activeMonthlyRate > 0.0) {
                    balance * (activeMonthlyRate * (1.0 + activeMonthlyRate).pow(remainingMonths.toDouble())) /
                            ((1.0 + activeMonthlyRate).pow(remainingMonths.toDouble()) - 1.0)
                } else {
                    balance / remainingMonths
                }
            }

            val interestThisMonth = balance * activeMonthlyRate
            var principalThisMonth = activeMonthlyPi - interestThisMonth

            if (principalThisMonth + 1.0 >= balance || monthCounter >= totalMonths) {
                principalThisMonth = balance
            } else if (principalThisMonth < 0.0) {
                principalThisMonth = 0.0
            }

            // Cap principal path to actual remaining balance
            
            // Check Extra Payment
            val maxAllowedExtra = max(0.0, balance - principalThisMonth)
            val actualExtra = min(extra, maxAllowedExtra)

            balance -= (principalThisMonth + actualExtra)
            totalInterestToDate += interestThisMonth

            schedule.add(
                AmortizationItem(
                    monthNumber = monthCounter,
                    yearNumber = currentYear,
                    paymentAmount = (interestThisMonth + principalThisMonth) + actualExtra,
                    principalPaid = principalThisMonth,
                    interestPaid = interestThisMonth,
                    extraPayment = actualExtra,
                    remainingBalance = max(0.0, balance),
                    totalInterestToDate = totalInterestToDate
                )
            )

            if (balance <= 0.0) break
        }

        // Aggregate by Year
        val yearlySchedule = schedule.groupBy { it.yearNumber }.map { (year, months) ->
            AmortizationYearlyItem(
                yearNumber = year,
                paymentAmount = months.sumOf { it.paymentAmount },
                principalPaid = months.sumOf { it.principalPaid },
                interestPaid = months.sumOf { it.interestPaid },
                extraPayment = months.sumOf { it.extraPayment },
                endingBalance = months.last().remainingBalance
            )
        }

        val totalInterestPaid = schedule.sumOf { it.interestPaid }
        val totalPaidPrincipal = schedule.sumOf { it.principalPaid }
        val totalExtraPaid = schedule.sumOf { it.extraPayment }

        // Tax Savings calculation per year
        val taxSavingsList = yearlySchedule.map { yearlyItem ->
            val interest = yearlyItem.interestPaid
            val savings = if (currentType == "Mortgage") {
                interest * (taxRate / 100.0)
            } else {
                0.0
            }
            AnnualTaxSavingsItem(
                yearNumber = yearlyItem.yearNumber,
                interestPaid = interest,
                estimatedTaxSavings = savings
            )
        }

        val totalTaxSavings = taxSavingsList.sumOf { it.estimatedTaxSavings }
        val finalRepaymentDurationMonths = schedule.size

        // Calculate Rate Sensitivity rows
        val sensitivityRates = listOf(annualRate - 1.0, annualRate - 0.5, annualRate, annualRate + 0.5, annualRate + 1.0, annualRate + 2.0)
            .filter { it > 0.0 }
            .distinct()
            .sorted()

        val sensitiveAnalysisList = if (annualRate <= 0.0) {
            emptyList()
        } else {
            sensitivityRates.map { rVal ->
                val mRate = rVal / 12.0 / 100.0
                val mPi = if (mRate > 0.0) {
                    p * (mRate * (1.0 + mRate).pow(totalMonths.toDouble())) /
                            ((1.0 + mRate).pow(totalMonths.toDouble()) - 1.0)
                } else {
                    p / totalMonths
                }

                // Run simple simulate schedule
                var balSim = p
                var totalIntSim = 0.0
                var monthSim = 0
                while (balSim > 0.0 && monthSim < totalMonths) {
                    monthSim++
                    val intSimMonth = balSim * mRate
                    var prinSimMonth = mPi - intSimMonth
                    if (prinSimMonth > balSim) prinSimMonth = balSim
                    else if (prinSimMonth < 0.0) prinSimMonth = 0.0
                    
                    val allowedExtra = max(0.0, balSim - prinSimMonth)
                    val appliedExtra = min(extra, allowedExtra)
                    balSim -= (prinSimMonth + appliedExtra)
                    totalIntSim += intSimMonth
                }

                val curMonthlyTotal = mPi + otherMonthlyExpenses
                val baseMonthlyTotal = monthlyPi + otherMonthlyExpenses
                val mDelta = curMonthlyTotal - baseMonthlyTotal
                val iDelta = totalIntSim - totalInterestPaid

                SensitivityItem(
                    rate = rVal,
                    monthlyPayment = curMonthlyTotal + extra,
                    totalInterest = totalIntSim,
                    deltaMonthly = mDelta,
                    deltaInterest = iDelta
                )
            }
        }

        _calculationResult.value = CalculationResult(
            isValid = true,
            principalLoanAmount = p,
            baseMonthlyPayment = monthlyPi,
            monthlyPropertyTax = monthlyTax,
            monthlyHomeInsurance = monthlyIns,
            monthlyPmi = monthlyPmi,
            totalMonthlyPaymentWithFees = totalMonthlyPaymentNoExtras + extra,
            schedule = schedule,
            yearlySchedule = yearlySchedule,
            taxSavingsSchedule = taxSavingsList,
            totalInterestPaid = totalInterestPaid,
            totalExtraPaid = totalExtraPaid,
            totalTaxSavings = totalTaxSavings,
            totalPaidAmount = totalPaidPrincipal + totalInterestPaid + totalExtraPaid + (otherMonthlyExpenses * finalRepaymentDurationMonths),
            actualRepaymentMonths = finalRepaymentDurationMonths,
            savingYearsEarly = max(0.0, ((totalMonths - finalRepaymentDurationMonths) / 12.0)),
            sensitivityAnalysis = sensitiveAnalysisList
        )
    }

    fun recalculateComparison() {
        if (!isComparisonActive.value) return

        val amount = comparisonLoanAmount.value.parseToDoubleOrNull() ?: 350000.0
        val downPay = if (loanType.value == "Mortgage") {
            comparisonDownPayment.value.parseToDoubleOrNull() ?: 0.0
        } else {
            0.0
        }
        val p = if (loanType.value == "Mortgage") max(0.0, amount - downPay) else amount

        val annualRate = comparisonInterestRate.value.parseToDoubleOrNull() ?: 5.5
        val years = comparisonLoanTermYears.value.toIntOrNull() ?: 15
        val extra = comparisonExtraPayment.value.parseToDoubleOrNull() ?: 0.0

        if (p <= 0.0 || annualRate < 0.0 || years <= 0) {
            _comparisonResult.value = CalculationResult(isValid = false)
            return
        }

        val totalMonths = years * 12
        val monthlyRate = annualRate / 12.0 / 100.0

        // Calculate Base P&I Monthly Payment
        val monthlyPi = if (monthlyRate > 0.0) {
            p * (monthlyRate * (1.0 + monthlyRate).pow(totalMonths.toDouble())) /
                    ((1.0 + monthlyRate).pow(totalMonths.toDouble()) - 1.0)
        } else {
            p / totalMonths
        }

        // Generate Amortization Schedule
        val schedule = mutableListOf<AmortizationItem>()
        var balance = p
        var totalInterestToDate = 0.0
        var monthCounter = 0

        while (balance > 0.01 && monthCounter < 600) {
            monthCounter++
            val currentYear = ((monthCounter - 1) / 12) + 1
            val interestThisMonth = balance * monthlyRate
            var principalThisMonth = monthlyPi - interestThisMonth

            if (principalThisMonth + 1.0 >= balance || monthCounter >= totalMonths) {
                principalThisMonth = balance
            } else if (principalThisMonth < 0.0) {
                principalThisMonth = 0.0
            }

            val maxAllowedExtra = max(0.0, balance - principalThisMonth)
            val actualExtra = min(extra, maxAllowedExtra)

            balance -= (principalThisMonth + actualExtra)
            totalInterestToDate += interestThisMonth

            schedule.add(
                AmortizationItem(
                    monthNumber = monthCounter,
                    yearNumber = currentYear,
                    paymentAmount = (interestThisMonth + principalThisMonth) + actualExtra,
                    principalPaid = principalThisMonth,
                    interestPaid = interestThisMonth,
                    extraPayment = actualExtra,
                    remainingBalance = max(0.0, balance),
                    totalInterestToDate = totalInterestToDate
                )
            )

            if (balance <= 0.0) break
        }

        val totalInterestPaid = schedule.sumOf { it.interestPaid }
        val finalRepaymentDurationMonths = schedule.size

        _comparisonResult.value = CalculationResult(
            isValid = true,
            principalLoanAmount = p,
            baseMonthlyPayment = monthlyPi,
            totalMonthlyPaymentWithFees = monthlyPi + extra,
            schedule = schedule,
            totalInterestPaid = totalInterestPaid,
            totalExtraPaid = schedule.sumOf { it.extraPayment },
            totalPaidAmount = p + totalInterestPaid + schedule.sumOf { it.extraPayment },
            actualRepaymentMonths = finalRepaymentDurationMonths,
            savingYearsEarly = max(0.0, ((totalMonths - finalRepaymentDurationMonths) / 12.0))
        )
    }
}

data class SensitivityItem(
    val rate: Double,
    val monthlyPayment: Double,
    val totalInterest: Double,
    val deltaMonthly: Double,
    val deltaInterest: Double
)

data class DebtPlannerScenario(
    val id: String,
    val name: String,
    val dateCreated: String,
    val debts: List<Debt>,
    val budget: String,
    val strategy: String
)

data class CalculationResult(
    val isValid: Boolean = false,
    val principalLoanAmount: Double = 0.0,
    val baseMonthlyPayment: Double = 0.0, // P&I only
    val monthlyPropertyTax: Double = 0.0,
    val monthlyHomeInsurance: Double = 0.0,
    val monthlyPmi: Double = 0.0,
    val totalMonthlyPaymentWithFees: Double = 0.0, // with other fees + Extra payment
    val schedule: List<AmortizationItem> = emptyList(),
    val yearlySchedule: List<AmortizationYearlyItem> = emptyList(),
    val taxSavingsSchedule: List<AnnualTaxSavingsItem> = emptyList(),
    val totalInterestPaid: Double = 0.0,
    val totalExtraPaid: Double = 0.0,
    val totalTaxSavings: Double = 0.0,
    val totalPaidAmount: Double = 0.0,
    val actualRepaymentMonths: Int = 0,
    val savingYearsEarly: Double = 0.0,
    val sensitivityAnalysis: List<SensitivityItem> = emptyList()
)

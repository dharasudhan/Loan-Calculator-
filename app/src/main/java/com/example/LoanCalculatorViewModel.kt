package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val prefs = application.getSharedPreferences("loan_calculator_prefs", Application.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // Localization
    private val _currentLanguage = MutableStateFlow(LanguageCode.EN)
    val currentLanguage: StateFlow<LanguageCode> = _currentLanguage.asStateFlow()

    fun setLanguage(lang: LanguageCode) {
        _currentLanguage.value = lang
    }

    // Input States
    val loanType = MutableStateFlow("Mortgage") // "Mortgage", "Personal" -> Merged Personal Loan & Auto Loan
    val homePrice = MutableStateFlow("400000")
    val downPayment = MutableStateFlow("80000")
    val loanAmountInput = MutableStateFlow("50000") // direct loan amount for general loans
    val interestRate = MutableStateFlow("6.5")
    val loanTermYears = MutableStateFlow("30")
    val extraPayment = MutableStateFlow("100")
    val propertyTaxRate = MutableStateFlow("1.2") // Annual % rate
    val homeInsurance = MutableStateFlow("1200") // Annual total $
    val pmiRate = MutableStateFlow("0.7") // Annual % rate
    val marginalTaxRate = MutableStateFlow("24") // Kept for backwards compatibility but not user-visible

    // Calculations Flow Outputs
    private val _calculationResult = MutableStateFlow(CalculationResult())
    val calculationResult: StateFlow<CalculationResult> = _calculationResult.asStateFlow()

    // Debt Planner Flow Outputs
    private val _debtsList = MutableStateFlow<List<Debt>>(emptyList())
    val debtsList: StateFlow<List<Debt>> = _debtsList.asStateFlow()

    val debtPlannerBudget = MutableStateFlow("1000")
    val debtPayoffStrategy = MutableStateFlow("Snowball") // "Snowball", "Avalanche"

    private val _debtPlannerResult = MutableStateFlow(DebtPlannerResult())
    val debtPlannerResult: StateFlow<DebtPlannerResult> = _debtPlannerResult.asStateFlow()

    init {
        loadFromPrefs()
        recalculate()
        recalculateDebtPlanner()
    }

    private fun loadFromPrefs() {
        loanType.value = prefs.getString("loanType", "Mortgage") ?: "Mortgage"
        homePrice.value = prefs.getString("homePrice", "400000") ?: "400000"
        downPayment.value = prefs.getString("downPayment", "80000") ?: "80000"
        loanAmountInput.value = prefs.getString("loanAmountInput", "50000") ?: "50000"
        interestRate.value = prefs.getString("interestRate", "6.5") ?: "6.5"
        loanTermYears.value = prefs.getString("loanTermYears", "30") ?: "30"
        extraPayment.value = prefs.getString("extraPayment", "100") ?: "100"
        propertyTaxRate.value = prefs.getString("propertyTaxRate", "1.2") ?: "1.2"
        homeInsurance.value = prefs.getString("homeInsurance", "1200") ?: "1200"
        pmiRate.value = prefs.getString("pmiRate", "0.7") ?: "0.7"
        
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

        debtPlannerBudget.value = prefs.getString("planner_budget", "1000") ?: "1000"
        debtPayoffStrategy.value = prefs.getString("payoff_strategy", "Snowball") ?: "Snowball"
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

        val totalBudget = debtPlannerBudget.value.toDoubleOrNull() ?: 0.0
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
        val currentType = loanType.value
        val hp = homePrice.value.toDoubleOrNull() ?: 0.0
        val dp = downPayment.value.toDoubleOrNull() ?: 0.0
        
        // Principal Loan Amount
        val p = if (currentType == "Mortgage") {
            max(0.0, hp - dp)
        } else {
            loanAmountInput.value.toDoubleOrNull() ?: 0.0
        }

        val annualRate = interestRate.value.toDoubleOrNull() ?: 0.0
        val years = loanTermYears.value.toIntOrNull() ?: 0
        val extra = extraPayment.value.toDoubleOrNull() ?: 0.0

        val rTax = propertyTaxRate.value.toDoubleOrNull() ?: 0.0
        val insAnnual = homeInsurance.value.toDoubleOrNull() ?: 0.0
        val rPmi = pmiRate.value.toDoubleOrNull() ?: 0.0
        val taxRate = marginalTaxRate.value.toDoubleOrNull() ?: 0.0

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

        // Generate Amortization Schedule
        val schedule = mutableListOf<AmortizationItem>()
        var balance = p
        var totalInterestToDate = 0.0
        var monthCounter = 0

        while (balance > 0.0 && monthCounter < 600) { // Limit to 50 years max (600 months)
            monthCounter++
            val currentYear = ((monthCounter - 1) / 12) + 1
            
            val interestThisMonth = balance * monthlyRate
            var principalThisMonth = monthlyPi - interestThisMonth

            if (principalThisMonth > balance) {
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
            savingYearsEarly = max(0.0, ((totalMonths - finalRepaymentDurationMonths) / 12.0))
        )
    }
}

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
    val savingYearsEarly: Double = 0.0
)

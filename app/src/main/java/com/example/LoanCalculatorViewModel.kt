package com.example

import androidx.lifecycle.ViewModel
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

class LoanCalculatorViewModel : ViewModel() {

    // Localization
    private val _currentLanguage = MutableStateFlow(LanguageCode.EN)
    val currentLanguage: StateFlow<LanguageCode> = _currentLanguage.asStateFlow()

    fun setLanguage(lang: LanguageCode) {
        _currentLanguage.value = lang
    }

    // Input States
    val loanType = MutableStateFlow("Mortgage") // "Mortgage", "Personal", "Auto"
    val homePrice = MutableStateFlow("400000")
    val downPayment = MutableStateFlow("80000")
    val loanAmountInput = MutableStateFlow("50000") // direct loan amount for Personal/Auto
    val interestRate = MutableStateFlow("6.5")
    val loanTermYears = MutableStateFlow("30")
    val extraPayment = MutableStateFlow("100")
    val propertyTaxRate = MutableStateFlow("1.2") // Annual % rate
    val homeInsurance = MutableStateFlow("1200") // Annual total $
    val pmiRate = MutableStateFlow("0.7") // Annual % rate
    val marginalTaxRate = MutableStateFlow("24") // Marginal income tax %

    // Calculations Flow Outputs
    private val _calculationResult = MutableStateFlow(CalculationResult())
    val calculationResult: StateFlow<CalculationResult> = _calculationResult.asStateFlow()

    init {
        // Trigger initial calculation
        recalculate()
    }

    fun updateInputs() {
        recalculate()
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
            val actualPiPayment = interestThisMonth + principalThisMonth
            
            // Check Extra Payment
            val maxAllowedExtra = max(0.0, balance - principalThisMonth)
            val actualExtra = min(extra, maxAllowedExtra)

            balance -= (principalThisMonth + actualExtra)
            totalInterestToDate += interestThisMonth

            schedule.add(
                AmortizationItem(
                    monthNumber = monthCounter,
                    yearNumber = currentYear,
                    paymentAmount = actualPiPayment + actualExtra,
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

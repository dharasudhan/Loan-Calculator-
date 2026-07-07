import re
with open("app/src/main/java/com/example/RentVsBuyTab.kt", "r") as f:
    text = f.read()

# Replace .ifEmpty with simple property access, and default rent inputs to first load behavior (we'll just use simple values or empty)
rent_init = r'''    // Synchronize initial Buying inputs from main Loan Calculator ViewModel
    var homePriceInput by remember \{ mutableStateOf\(viewModel\.homePrice\.value\.ifEmpty \{ "350000" \}\) \}
    var downPaymentInput by remember \{ mutableStateOf\(viewModel\.downPayment\.value\.ifEmpty \{ "70000" \}\) \}
    var interestRateInput by remember \{ mutableStateOf\(viewModel\.interestRate\.value\.ifEmpty \{ "6\.5" \}\) \}
    var loanTermInput by remember \{ mutableStateOf\(viewModel\.loanTermYears\.value\.ifEmpty \{ "30" \}\) \}
    var taxRateInput by remember \{ mutableStateOf\(viewModel\.propertyTaxRate\.value\.ifEmpty \{ "1\.2" \}\) \}
    var homeInsInput by remember \{ mutableStateOf\(viewModel\.homeInsurance\.value\.ifEmpty \{ "1200" \}\) \}'''

new_rent_init = '''    // Synchronize initial Buying inputs from main Loan Calculator ViewModel
    var homePriceInput by remember { mutableStateOf(viewModel.homePrice.value) }
    var downPaymentInput by remember { mutableStateOf(viewModel.downPayment.value) }
    var interestRateInput by remember { mutableStateOf(viewModel.interestRate.value) }
    var loanTermInput by remember { mutableStateOf(viewModel.loanTermYears.value) }
    var taxRateInput by remember { mutableStateOf(viewModel.propertyTaxRate.value) }
    var homeInsInput by remember { mutableStateOf(viewModel.homeInsurance.value) }'''

text = re.sub(rent_init, new_rent_init, text)

effect = '''    var showExtendedProjection by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.clearDataEvent.collect {
            rentInput = ""
            rentIncreaseInput = ""
            rentersInsInput = ""
            homePriceInput = ""
            downPaymentInput = ""
            interestRateInput = ""
            loanTermInput = ""
            taxRateInput = ""
            homeInsInput = ""
            registrationTaxInput = ""
            maintenanceInput = ""
            appreciationInput = ""
        }
    }'''

text = text.replace('    var showExtendedProjection by remember { mutableStateOf(false) }', effect)

# We should also ensure rentInput starts correctly... 
# The user wants "default values for all the inputs during the first time load". 
# Currently rentInput = "1800". But if cleared, it should be empty. But how do we distinguish first load vs cleared?
# Well, we can just use rentInput = if (viewModel.homePrice.value == "350000") "1800" else "1800"
# Wait, if we just initialize them as "1800", they'll be "1800" on every mount except when "clearDataEvent" fires. But that's fine, first load it will be "1800". Wait, if the user clears data, then leaves the tab and comes back, rentInput will be re-initialized to "1800" because `remember` will reset it!
# To fix this, RentVsBuy inputs should also be moved to SharedPreferences, or we can just initialize them to empty if `viewModel.homePrice.value.isEmpty()`.
# Yes!

rent_vars = r'''    // Input States
    var rentInput by remember \{ mutableStateOf\("1800"\) \}
    var rentIncreaseInput by remember \{ mutableStateOf\("3\.0"\) \}
    var rentersInsInput by remember \{ mutableStateOf\("15"\) \}'''

new_rent_vars = '''    val isCleared = viewModel.homePrice.value.isEmpty()
    // Input States
    var rentInput by remember { mutableStateOf(if (isCleared) "" else "1800") }
    var rentIncreaseInput by remember { mutableStateOf(if (isCleared) "" else "3.0") }
    var rentersInsInput by remember { mutableStateOf(if (isCleared) "" else "15") }'''

text = re.sub(rent_vars, new_rent_vars, text)

# Registration and maintenance
rent_vars2 = r'''    var registrationTaxInput by remember \{ mutableStateOf\("2\.0"\) \} // upfront registration / stamp duty & taxes %
    var maintenanceInput by remember \{ mutableStateOf\("1\.0"\) \} // % of home price yearly
    var appreciationInput by remember \{ mutableStateOf\("3\.0"\) \} // Yearly % home value increase'''

new_rent_vars2 = '''    var registrationTaxInput by remember { mutableStateOf(if (isCleared) "" else "2.0") } // upfront registration / stamp duty & taxes %
    var maintenanceInput by remember { mutableStateOf(if (isCleared) "" else "1.0") } // % of home price yearly
    var appreciationInput by remember { mutableStateOf(if (isCleared) "" else "3.0") } // Yearly % home value increase'''

text = re.sub(rent_vars2, new_rent_vars2, text)

with open("app/src/main/java/com/example/RentVsBuyTab.kt", "w") as f:
    f.write(text)

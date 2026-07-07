package com.example
import androidx.compose.ui.draw.scale

import com.example.LoanCalculatorViewModel
import com.example.LanguageCode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class YearlyComparisonRow(
    val year: Int,
    val rentCumulativeSpend: Double,
    val buyOutPocketSpend: Double,
    val buyHomeValue: Double,
    val buyRemainingBalance: Double,
    val buyEquity: Double,
    val buyNetCost: Double,
    val isBreakEven: Boolean
)

@Composable
fun RentVsBuyTab(
    viewModel: LoanCalculatorViewModel,
    lang: LanguageCode
) {
    val customCurrency by viewModel.customCurrencySymbol.collectAsState()
    val cur = customCurrency ?: lang.currencySymbol
    val context = LocalContext.current

    val isCleared = viewModel.homePrice.value.isEmpty()
    // Input States
    var rentInput by remember { mutableStateOf(if (isCleared) "" else "1800") }
    var rentIncreaseInput by remember { mutableStateOf(if (isCleared) "" else "3.0") }
    var rentersInsInput by remember { mutableStateOf(if (isCleared) "" else "15") }

    // Synchronize initial Buying inputs from main Loan Calculator ViewModel
    var homePriceInput by remember { mutableStateOf(viewModel.homePrice.value) }
    var downPaymentInput by remember { mutableStateOf(viewModel.downPayment.value) }
    var interestRateInput by remember { mutableStateOf(viewModel.interestRate.value) }
    var loanTermInput by remember { mutableStateOf(viewModel.loanTermYears.value) }
    var taxRateInput by remember { mutableStateOf(viewModel.propertyTaxRate.value) }
    var homeInsInput by remember { mutableStateOf(viewModel.homeInsurance.value) }
    
    // Rent vs Buy Specific owner inputs
    var registrationTaxInput by remember { mutableStateOf("2.0") } // upfront registration / stamp duty & taxes %
    var maintenanceInput by remember { mutableStateOf("1.0") } // % of home price yearly
    var appreciationInput by remember { mutableStateOf("4.0") } // % home appreciation yearly

    // Strategy Parameters
    var plannedYearsInput by remember { mutableStateOf("7") }
    var activeGlossaryTerm by remember { mutableStateOf<String?>(null) }
    var showExtendedProjection by remember { mutableStateOf(false) }

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
    }

    // Recalculate everything dynamically
    val rentMonthly = rentInput.toDoubleOrNull() ?: 1800.0
    val rentIncrease = rentIncreaseInput.toDoubleOrNull() ?: 3.0
    val rentersIns = rentersInsInput.toDoubleOrNull() ?: 15.0

    val homePriceVal = homePriceInput.toDoubleOrNull() ?: 350000.0
    val downPaymentVal = downPaymentInput.toDoubleOrNull() ?: 70000.0
    val interestRateVal = interestRateInput.toDoubleOrNull() ?: 6.5
    val loanTermYears = loanTermInput.toIntOrNull() ?: 30
    val taxRateVal = taxRateInput.toDoubleOrNull() ?: 1.2
    val homeInsVal = homeInsInput.toDoubleOrNull() ?: 1200.0
    val registrationTaxVal = registrationTaxInput.toDoubleOrNull() ?: 2.0
    val maintenanceVal = maintenanceInput.toDoubleOrNull() ?: 1.0
    val appreciationVal = appreciationInput.toDoubleOrNull() ?: 4.0
    val plannedYears = Math.max(1, Math.min(30, plannedYearsInput.toIntOrNull() ?: 7))

    // Performing year-by-year 30-year projections
    val projectionList = remember(
        rentMonthly, rentIncrease, rentersIns,
        homePriceVal, downPaymentVal, interestRateVal, loanTermYears, taxRateVal, homeInsVal,
        registrationTaxVal, maintenanceVal, appreciationVal
    ) {
        val list = mutableListOf<YearlyComparisonRow>()
        
        // Setup buying variables
        val loanAmount = max(0.0, homePriceVal - downPaymentVal)
        val r = interestRateVal / 12.0 / 100.0
        val totalMonths = loanTermYears * 12
        val buyMonthlyPi = if (r > 0.0 && totalMonths > 0) {
            loanAmount * (r * (1.0 + r).pow(totalMonths.toDouble())) /
                    ((1.0 + r).pow(totalMonths.toDouble()) - 1.0)
        } else if (totalMonths > 0) {
            loanAmount / totalMonths
        } else {
            0.0
        }

        val upfrontClosingCosts = homePriceVal * (registrationTaxVal / 100.0)
        var cumulativeRentSpent = 0.0
        var cumulativeBuySpent = downPaymentVal + upfrontClosingCosts
        
        var currentBalance = loanAmount
        var currentHomeValue = homePriceVal

        // Amortize month-by-month for 30 years
        for (y in 1..30) {
            // RENT CALCULATIONS
            val currentYearRentMonthly = rentMonthly * (1.1 - 0.1 + rentIncrease / 100.0).pow(y - 1)
            val rentPaidThisYear = currentYearRentMonthly * 12
            val rentersInsPaidThisYear = rentersIns * 12
            cumulativeRentSpent += (rentPaidThisYear + rentersInsPaidThisYear)

            // BUY CALCULATIONS
            // Mortgage Principal and interest paid
            var principalPaidThisYear = 0.0
            var interestPaidThisYear = 0.0
            
            for (m in 1..12) {
                if (currentBalance > 0.01) {
                    val interestThisMonth = currentBalance * r
                    var principalThisMonth = buyMonthlyPi - interestThisMonth
                    if (principalThisMonth + 0.1 >= currentBalance) {
                        principalThisMonth = currentBalance
                    }
                    interestPaidThisYear += interestThisMonth
                    principalPaidThisYear += principalThisMonth
                    currentBalance -= principalThisMonth
                }
            }
            
            val taxPaidThisYear = homePriceVal * (taxRateVal / 100.0)
            val insPaidThisYear = homeInsVal
            val maintPaidThisYear = homePriceVal * (maintenanceVal / 100.0)
            
            val outOfPocketThisYear = principalPaidThisYear + interestPaidThisYear + taxPaidThisYear + insPaidThisYear + maintPaidThisYear
            cumulativeBuySpent += outOfPocketThisYear

            // Asset value buildup
            currentHomeValue *= (1.0 + appreciationVal / 100.0)
            val endingEquity = max(0.0, currentHomeValue - currentBalance)
            val buyNetCost = cumulativeBuySpent - endingEquity

            list.add(
                YearlyComparisonRow(
                    year = y,
                    rentCumulativeSpend = cumulativeRentSpent,
                    buyOutPocketSpend = cumulativeBuySpent,
                    buyHomeValue = currentHomeValue,
                    buyRemainingBalance = currentBalance,
                    buyEquity = endingEquity,
                    buyNetCost = buyNetCost,
                    isBreakEven = false
                )
            )
        }

        // Identify the first break-even year
        var breakEvenIndex = -1
        for (i in list.indices) {
            if (list[i].buyNetCost < list[i].rentCumulativeSpend) {
                breakEvenIndex = i
                break
            }
        }

        if (breakEvenIndex != -1) {
            list.replaceAll { row ->
                if (row.year >= (breakEvenIndex + 1)) {
                    row.copy(isBreakEven = true)
                } else row
            }
        }

        list
    }

    val breakEvenYearRow = projectionList.firstOrNull { it.isBreakEven }
    val breakEvenYearVal = breakEvenYearRow?.year

    val scrollState = rememberScrollState()

    // Interactive Glossary / Tooltip Dialog for Acronyms
    if (activeGlossaryTerm != null) {
        AlertDialog(
            onDismissRequest = { activeGlossaryTerm = null },
            confirmButton = {
                TextButton(onClick = { activeGlossaryTerm = null }) {
                    Text(
                        text = when(lang) {
                            LanguageCode.ES -> "Entendido"
                            LanguageCode.FR -> "D'accord"
                            LanguageCode.DE -> "Verstanden"
                            LanguageCode.HI -> "समझ गया"
                            LanguageCode.TA -> "சரி"
                            else -> "Dismiss"
                        }
                    )
                }
            },
            title = {
                Text(
                    text = when(activeGlossaryTerm) {
                        "HOA" -> when(lang) {
                            LanguageCode.ES -> "HOA (Asociación de Propietarios)"
                            LanguageCode.FR -> "HOA (Frais de Copropriété)"
                            LanguageCode.DE -> "HOA (Hausbesitzervereinigung)"
                            LanguageCode.HI -> "एचओए (गृहस्वामी संघ)"
                            LanguageCode.TA -> "HOA (வீட்டு உரிமையாளர் சங்கம்)"
                            else -> "HOA (Homeowners Association)"
                        }
                        "LTV" -> when(lang) {
                            LanguageCode.ES -> "LTV (Relación Préstamo-Valor)"
                            LanguageCode.FR -> "LTV (Ratio Prêt/Valeur)"
                            LanguageCode.DE -> "LTV (Beleihungsauslauf)"
                            LanguageCode.HI -> "एलटीवी (ऋण-से-मूल्य अनुपात)"
                            LanguageCode.TA -> "LTV (கடன்-மதிப்பு விகிதம்)"
                            else -> "LTV (Loan-To-Value Ratio)"
                        }
                        "BrokerFee" -> when(lang) {
                            LanguageCode.ES -> "Costos de Venta y Comisiones"
                            LanguageCode.FR -> "Frais de Courtage et Vente"
                            LanguageCode.DE -> "Maklergebühren und Verkaufskosten"
                            LanguageCode.HI -> "दलाली और बिक्री लागत"
                            LanguageCode.TA -> "தரகு மற்றும் விற்பனை செலவுகள்"
                            else -> "Selling Costs & Broker Fees"
                        }
                        "ROI" -> when(lang) {
                            LanguageCode.ES -> "ROI (Punto de Equilibrio de Inversión)"
                            LanguageCode.FR -> "ROI (Retour sur Investissement)"
                            LanguageCode.DE -> "ROI (Rentabilität / Gewinnschwelle)"
                            LanguageCode.HI -> "आरओआई (निवेश पर रिटर्न)"
                            LanguageCode.TA -> "ROI (முதலீட்டின் மீதான வட்டி/சமநிலை)"
                            else -> "ROI (Return on Investment)"
                        }
                        "PMI" -> when(lang) {
                            LanguageCode.ES -> "PMI (Seguro Hipotecario Privado)"
                            LanguageCode.FR -> "PMI (Assurance Emprunteur Privée)"
                            LanguageCode.DE -> "PMI (Private Hypothekenversicherung)"
                            LanguageCode.HI -> "पीएमआई (निजी बंधक बीमा)"
                            LanguageCode.TA -> "PMI (தனியார் அடமான காப்பீடு)"
                            else -> "PMI (Private Mortgage Insurance)"
                        }
                        "APR" -> when(lang) {
                            LanguageCode.ES -> "APR (Tasa de Interés Real / Anual)"
                            LanguageCode.FR -> "APR (Taux Annuel Effectif Global)"
                            LanguageCode.DE -> "APR (Effektiver Jahreszins)"
                            LanguageCode.HI -> "एपीआर (वास्तविक वार्षिक ब्याज दर)"
                            LanguageCode.TA -> "APR (வருடாந்திர வட்டி விகிதம்)"
                            else -> "APR / Interest Rates"
                        }
                        "REG_TAX" -> when(lang) {
                            LanguageCode.ES -> "Registro e Impuestos de Compra"
                            LanguageCode.FR -> "Enregistrement & Frais de Mutation"
                            LanguageCode.DE -> "Kaufnebenkosten & Steuern"
                            LanguageCode.HI -> "पंजीकरण और खरीद कर"
                            LanguageCode.TA -> "பதிவு மற்றும் கொள்முதல் வரிகள்"
                            else -> "Registration & Purchase Taxes"
                        }
                        "STAY" -> when(lang) {
                            LanguageCode.ES -> "Periodo de Estancia Planeado"
                            LanguageCode.FR -> "Durée de séjour planifiée"
                            LanguageCode.DE -> "Geplante Wohndauer"
                            LanguageCode.HI -> "रहने की योजनाबद्ध अवधि"
                            LanguageCode.TA -> "திட்டமிட்ட வசிக்கும் காலம்"
                            else -> "Planned Residence Stay"
                        }
                        else -> when(lang) {
                            LanguageCode.ES -> "Glosario Financiero"
                            LanguageCode.FR -> "Lexique Financier"
                            LanguageCode.DE -> "Finanzglossar"
                            LanguageCode.HI -> "वित्तीय शब्दावली"
                            LanguageCode.TA -> "நிதிச் சொற்களஞ்சியம்"
                            else -> "Dictionary Glossary"
                        }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = when(activeGlossaryTerm) {
                        "HOA" -> when(lang) {
                            LanguageCode.ES -> "Las cuotas de la Asociación de Propietarios (HOA) se pagan obligatoriamente para el mantenimiento de áreas comunes, seguridad y servicios compartidos. Suman costos fijos no recuperables a la propiedad."
                            LanguageCode.FR -> "Les frais d'Association des Propriétaires (HOA) couvrent l'entretien, l'assurance et les services des parties communes. Ils constituent une charge fixe non récupérable."
                            LanguageCode.DE -> "HOA (Hausbesitzervereinigung) bezieht sich auf obligatorische wiederkehrende Gebühren, die von Immobilieneigentümern gezahlt werden. Diese decken die Instandhaltung von Gemeinschaftsräumen, die Sicherheit oder Annehmlichkeiten der Nachbarschaft ab. Es stellt nicht erstattungsfähige Zusatzkosten dar."
                            LanguageCode.HI -> "एचओए (गृहस्वामी संघ) का तात्पर्य संपत्ति के मालिकों द्वारा भुगतान किए जाने वाले अनिवार्य आवर्ती शुल्क से है। इसमें साझा स्थानों का रखरखाव, सुरक्षा या सुविधाएं शामिल हैं। यह गैर-वसूलने योग्य अतिरिक्त स्वामित्व लागत को दर्शाता है।"
                            LanguageCode.TA -> "HOA (வீட்டு உரிமையாளர் சங்கம்) என்பது சொத்து உரிமையாளர்களால் செலுத்தப்படும் கட்டாயத் தொடர் கட்டணங்களைக் குறிக்கிறது. இது பொதுவான இடங்களைப் பராமரித்தல், பாதுகாப்பு அல்லது பிற பொதுவான வசதிகளுக்கான கட்டணம் ஆகும்."
                            else -> "HOA (Homeowners Association) refers to mandatory recurring dues paid by property owners. These covers maintenance of shared spaces, neighborhood security, or amenities. It represents a non-recoverable ownership cost."
                        }
                        "LTV" -> when(lang) {
                            LanguageCode.ES -> "La Relación Préstamo-Valor (LTV) mide el porcentaje de hipoteca respecto al precio total de venta. Enganches inferiores al 20% (LTV > 80%) suelen requerir pago obligatorio de seguro hipotecario (PMI)."
                            LanguageCode.FR -> "Le ratio Prêt/Valeur (LTV) indique la part d'emprunt restante par rapport à la valeur estimée du bien. Moins d'apport augmente le LTV."
                            LanguageCode.DE -> "Der LTV (Beleihungsauslauf) ist das prozentuale Verhältnis der Kreditsumme zum Wert der Immobilie. Ein Eigenkapital von weniger als 20 % (LTV > 80 %) führt meist zu zusätzlichen monatlichen Kosten für eine private Hypothekenversicherung (PMI)."
                            LanguageCode.HI -> "एलटीवी (ऋण-से-मूल्य) आपके ऋण राशि और घर की कुल कीमत के बीच के प्रतिशत अनुपात को दर्शाता है। 20% से कम डाउन पेमेंट (एलटीवी > 80%) के कारण अतिरिक्त मासिक पीएमआई शुल्क देना पड़ता है।"
                            LanguageCode.TA -> "LTV (கடன்-மதிப்பு விகிதம்) என்பது வீட்டின் விலையுடன் ஒப்பிடும்போது கடன் தொகையின் சதவீதத்தைக் குறிக்கிறது. 20%-க்கும் குறைவான முன்பணத்துடன் (LTV > 80%) கடன் வாங்கினால் கூடுதல் மாதாந்திர PMI காப்பீட்டுக் கட்டணத்தை செலுத்த வேண்டியிருக்கும்."
                            else -> "LTV (Loan-to-Value) represents the percentage ratio of your mortgage remaining compared to corporate value of the house. Entering with less than 20% down payment (LTV > 80%) triggers extra monthly PMI costs."
                        }
                        "ROI" -> when(lang) {
                            LanguageCode.ES -> "El Retorno de Inversión (en Rent vs Buy se traduce como el punto de equilibrio) marca el año exacto en el que comprar se vuelve más rentable que alquilar. Si planeas mudarte antes, alquilar es mejor."
                            LanguageCode.FR -> "Le Retour sur Investissement (ROI) correspond au point de bascule géographique et temporel où posséder un bien devient financièrement plus avantageux que de payer un loyer."
                            LanguageCode.DE -> "Die Kapitalrendite (ROI) oder der Crossover-Punkt misst das genaue Jahr, in dem verbleibende Pfandbriefzahlungen und steigendes Eigenkapital die kumulierten Mietkosten übersteigen. Wenn Sie planen, die Immobilie vor dieser Schwelle zu verkaufen, ist Mieten finanziell vorteilhafter."
                            LanguageCode.HI -> "आरओआई (निवेश पर रिटर्न) या क्रॉसओवर पॉइंट उस सटीक वर्ष को maapta है जहां कम होता हुआ ऋण और बढ़ता हुआ गृह मूल्य संचयी किराये के खर्चों से बेहतर परिणाम देता है। यदि आप इससे पहले घर बेचने की योजना बनाते हैं, तो किराए पर रहना बेहतर है।"
                            LanguageCode.TA -> "ROI (முதலீட்டின் மீதான லாபம்) அல்லது சமநிலை புள்ளி என்பது செலுத்தப்பட்ட மாதாந்திர தவணைகளும் வீட்டின் மதிப்பும் வாடகைச் செலவுகளைத் தாண்டி லாபம் தரும் ஆண்டைக் குறிக்கிறது. இதற்கு முன்னரே வீட்டை விற்க நேர்ந்தால் வாடகைக்கு இருப்பதே நல்லது."
                            else -> "ROI (Return on Investment) or Crossover Point measures the precise year where the paid-down mortgage and rising equity exceed all rental cumulative expenses. If you plan to sell before this threshold, renting preserves more wealth."
                        }
                        "PMI" -> when(lang) {
                            LanguageCode.ES -> "El Seguro Hipotecario Privado (PMI) protege al banco si aportas menos del 20% como enganche. Incrementa la cuota mensual sin abonar al capital de la deuda."
                            LanguageCode.FR -> "L'Assurance Emprunteur Privée (PMI) est imposée par l'organisme de crédit si votre apport personnel est inférieur à 20% du montant total du bien. Elle augmente les mensualités sans réduire el capital emprunté."
                            LanguageCode.DE -> "Eine private Hypothekenversicherung (PMI) ist eine vom Kreditgeber verlangte Absicherung, wenn Ihre Anzahlung unter 20 % liegt. Sie erhöht Ihre monatliche Rate, ist eine reine Zusatzgebühr und trägt nicht zur Reduzierung Ihrer Restschuld bei."
                            LanguageCode.HI -> "पीएमआई (निजी बंधक बीमा) ऋणदाताओं द्वारा आवश्यक एक अनिवार्य सुरक्षा प्रीमियम है यदि आपका डाउन पेमेंट 20% से कम है। यह बैंक के लिए सुरक्षा मात्र है और एक शुद्ध मासिक गैर-वसूलने योग्य अतिरिक्त खर्च का प्रतिनिधित्व करता है।"
                            LanguageCode.TA -> "PMI (தனியார் அடமானக் காப்பீடு) என்பது 20%-க்கும் குறைவான முன்பணத்துடன் கடன் வாங்கும்போது கடன் வழங்குநர்களால் கோரப்படும் அவசியமான காப்பீடு ஆகும். இது மாதாந்திர தவணையை அதிகரிக்கும் ஆனால் அசல் தொகையைக் குறைக்காது."
                            else -> "PMI (Private Mortgage Insurance) is a mandatory protection premium required by lenders if your down payment is less than 20%. It is helper security for the bank and represents a pure monthly non-recoverable expense."
                        }
                        "APR" -> when(lang) {
                            LanguageCode.ES -> "La Tasa de Porcentaje Anual (APR) refleja el costo real anual de su hipoteca, sumando el interés bruto, honorarios de corretaje, seguros y costos de cierre requeridos."
                            LanguageCode.FR -> "Le Taux Annuel Effectif Global (APR) intègre l'intérêt nominal combiné aux frais administratifs, de courtage, d'assurances et de dossier."
                            LanguageCode.DE -> "Der effektive Jahreszins (APR) stellt die tatsächlichen jährlichen Gesamtkosten Ihrer Finanzierung dar. Er kombiniert den Nominalzinssatz mit zusätzlichen Verwaltungsgebühren, Bearbeitungskosten und Abschlussgebühren."
                            LanguageCode.HI -> "वार्षिक प्रतिशत दर (एपीआर) आपके ऋण लेने की वास्तविक व्यापक वार्षिक लागत का प्रतिनिधित्व करती है। यह वार्षिक ब्याज दर को अन्य अतिरिक्त वित्त शुल्कों और विलेख पंजीकरण शुल्कों के साथ जोड़ती है।"
                            LanguageCode.TA -> "APR (ஆண்டு சதவீத வட்டி விகிதம்) என்பது உங்களது கடனுக்கான உண்மையான வருடாந்திர செலவை குறிக்கிறது. இது உங்களது வட்டி விகிதத்துடன் காப்பீடு மற்றும் இதர கோப்பு கட்டணங்களையும் இணைத்துக் காட்டுகிறது."
                            else -> "APR (Annual Percentage Rate) represents the real comprehensive yearly cost of your financing. It combines the raw interest coupon rate with extra points, setup, and closing amortizations."
                        }
                        "REG_TAX" -> when(lang) {
                            LanguageCode.ES -> "Tasas gubernamentales obligatorias, impuestos de transferencia y derechos de registro (como el impuesto sobre transmisiones patrimoniales o AJD) que se cobran por adelantado al comprar una propiedad."
                            LanguageCode.FR -> "Droits de mutation, frais d'enregistrement et émoluments de notaire perçus par l'État à l'achat du bien immobilier (frais d'acquisition)."
                            LanguageCode.DE -> "Einmalige Erwerbsnebenkosten, einschließlich Grunderwerbsteuer, Notargebühren und Grundbucheintragung beim Kauf einer Immobilie."
                            LanguageCode.HI -> "सरकारी पंजीकरण शुल्क, स्टांप शुल्क और हस्तांतरण कर जो संपत्ति खरीद और विलेख पंजीकरण के दौरान अग्रिम रूप से लिए जाते हैं।"
                            LanguageCode.TA -> "சொத்து வாங்கும் போதும், பத்திரப் பதிவின் போதும் செலுத்த வேண்டிய அரசுப் பதிவுச் கட்டணம், முத்திரைக் கட்டணம் மற்றும் பிற வரிகள்."
                            else -> "Government registration fees, stamp duty, and transfer taxes. These are mandatory upfront transactional costs charged on the home price during property purchase deeds registration."
                        }
                        "STAY" -> when(lang) {
                            LanguageCode.ES -> "Años que planeas conservar esta vivienda. Si tu periodo planeado de estancia es más corto que el año cruzado de equilibrio financiero, es preferible alquilar debido a los altos impuestos de compra y venta."
                            LanguageCode.FR -> "La durée d'occupation estimée de cette maison. Déménager avant d'atteindre le point de rentabilité entraîne des pertes nettes dues aux commissions de relocation."
                            LanguageCode.DE -> "Die geplante Wohndauer in diesem Haus. Der Kauf ist mit hohen einmaligen Erwerbsnebenkosten und späteren Verkaufskosten verbunden. Bei einer kürzeren Wohndauer als dem Break-even-Jahr ist Mieten finanziell klüger."
                            LanguageCode.HI -> "वह अवधि जब तक आप इस घर में रहने की योजना बना रहे हैं। घर खरीदने में उच्च प्रारंभिक और बाद में बिक्री लागत शामिल होती है। यदि आपकी योजना ब्रेक-ईवन वर्ष से कम अवधि की है, तो किराए पर रहना अधिक समझदारी है।"
                            LanguageCode.TA -> "நீங்கள் இந்த வீட்டில் வசிக்கத் திட்டமிட்டுள்ள ஆண்டுகள். வீடு வாங்குவது அதிக ஆரம்ப மற்றும் நிறைவு கட்டணங்களை உள்ளடக்கியது, எனவே இக்காலகட்டம் உங்களது சமநிலை ஆண்டிற்கு குறைவாக இருந்தால் வாடகைக்கு இருப்பதே சிறந்தது."
                            else -> "The duration you plan to live in this house. Buying involves large upfront and exit transaction costs; stays shorter than your break-even crossover year are financially inefficient, making renting wiser."
                        }
                        "BrokerFee" -> when(lang) {
                            LanguageCode.ES -> "El 'Costo Neto de Compra' supone que la propiedad se vende en el año objetivo. No incluye honorarios de corredores u otros costos de cierre que pueden variar según la región (generalmente entre 3% y 6%)."
                            LanguageCode.FR -> "Le «Coût Net d'Achat» suppose la vente de la propriété à l'année cible. Il n'inclut pas les frais de courtage ou autres frais de clôture qui varient selon la région (généralement 3% à 6%)."
                            LanguageCode.DE -> "Die 'Kauf Netto-Zahlung' geht davon aus, dass die Immobilie im Zieljahr verkauft wird. Maklergebühren oder andere Abschlusskosten, die je nach Region variieren (typischerweise 3% bis 6%), sind nicht enthalten."
                            LanguageCode.HI -> "'शुद्ध खरीद लागत' यह मानती है कि संपत्ति लक्ष्य वर्ष में बेची जाती है। इसमें दलाली शुल्क या अन्य समापन लागतें शामिल नहीं हैं जो क्षेत्र के अनुसार भिन्न हो सकती हैं (आमतौर पर 3% से 6%)।"
                            LanguageCode.TA -> "'வாங்குதலின் நிகர மதிப்பு' என்பது இலக்கு ஆண்டில் சொத்து விற்கப்படுவதாகக் கருதுகிறது. பிராந்தியத்தைப் பொறுத்து மாறுபடும் தரகர் கட்டணம் அல்லது பிற நிறைவுச் செலவுகளை (பொதுவாக 3% முதல் 6% வரை) இதில் சேர்க்கவில்லை."
                            else -> "The 'Buy Net Cost' assumes the property is sold at the target year. It does not include broker fees or other closing costs when selling, which can vary by region (typically 3% to 6% of the home's value)."
                        }
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth > 650.dp
        
        if (isTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Inputs Pane
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RentVsBuyInputs(
                        lang = lang,
                        cur = cur,
                        rentInput = rentInput,
                        onRentChange = { rentInput = it },
                        rentIncreaseInput = rentIncreaseInput,
                        onRentIncreaseChange = { rentIncreaseInput = it },
                        rentersInsInput = rentersInsInput,
                        onRentersInsChange = { rentersInsInput = it },
                        homePriceInput = homePriceInput,
                        onHomePriceChange = { homePriceInput = it },
                        downPaymentInput = downPaymentInput,
                        onDownPaymentChange = { downPaymentInput = it },
                        interestRateInput = interestRateInput,
                        onInterestRateChange = { interestRateInput = it },
                        loanTermInput = loanTermInput,
                        onLoanTermChange = { loanTermInput = it },
                        taxRateInput = taxRateInput,
                        onTaxRateChange = { taxRateInput = it },
                        homeInsInput = homeInsInput,
                        onHomeInsChange = { homeInsInput = it },
                        registrationTaxInput = registrationTaxInput,
                        onRegistrationTaxChange = { registrationTaxInput = it },
                        maintenanceInput = maintenanceInput,
                        onMaintenanceChange = { maintenanceInput = it },
                        appreciationInput = appreciationInput,
                        onAppreciationChange = { appreciationInput = it },
                        plannedYearsInput = plannedYearsInput,
                        onPlannedYearsChange = { plannedYearsInput = it },
                        onHelpClick = { activeGlossaryTerm = it }
                    )
                }

                // Results Pane
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RentVsBuyHeader(lang, breakEvenYearVal)
                    RentVsBuyDashboard(
                        lang = lang,
                        cur = cur,
                        projectionList = projectionList,
                        breakEvenYear = breakEvenYearVal,
                        plannedYears = plannedYears,
                        showExtendedProjection = showExtendedProjection,
                        onToggleExtendedProjection = { showExtendedProjection = it },
                        onHelpClick = { activeGlossaryTerm = it }
                    )
                    val targetRow = if (showExtendedProjection) projectionList.last() else projectionList.firstOrNull { it.year == plannedYears } ?: projectionList.last()
                    RentVsBuyExplanationCard(
                        lang = lang,
                        cur = cur,
                        breakEvenYear = breakEvenYearVal,
                        plannedYears = plannedYears,
                        rentCumulativeSpend = targetRow.rentCumulativeSpend,
                        buyEquity = targetRow.buyEquity,
                        homePrice = homePriceVal,
                        rentMonthly = rentMonthly,
                        appreciationVal = appreciationVal,
                        rentIncrease = rentIncrease
                    )
                    Button(
                        onClick = {
                            val activity = context.findActivity()
                            if (activity != null) {
                                InterstitialAdHelper.showAdIfReady(activity) {
                                    PdfReportExporter.generateAndShareRentVsBuyPdf(
                                        context = context,
                                        lang = lang,
                                        rentMonthly = rentMonthly,
                                        rentIncrease = rentIncrease,
                                        rentersIns = rentersIns,
                                        homePriceVal = homePriceVal,
                                        downPaymentVal = downPaymentVal,
                                        interestRateVal = interestRateVal,
                                        loanTermYears = loanTermYears,
                                        taxRateVal = taxRateVal,
                                        homeInsVal = homeInsVal,
                                        maintenanceVal = maintenanceVal,
                                        appreciationVal = appreciationVal,
                                        registrationTaxVal = registrationTaxVal,
                                        breakEvenYear = breakEvenYearVal,
                                        plannedYears = plannedYears,
                                        projectionList = projectionList
                                    )
                                }
                            } else {
                                PdfReportExporter.generateAndShareRentVsBuyPdf(
                                    context = context,
                                    lang = lang,
                                    rentMonthly = rentMonthly,
                                    rentIncrease = rentIncrease,
                                    rentersIns = rentersIns,
                                    homePriceVal = homePriceVal,
                                    downPaymentVal = downPaymentVal,
                                    interestRateVal = interestRateVal,
                                    loanTermYears = loanTermYears,
                                    taxRateVal = taxRateVal,
                                    homeInsVal = homeInsVal,
                                    maintenanceVal = maintenanceVal,
                                    appreciationVal = appreciationVal,
                                    registrationTaxVal = registrationTaxVal,
                                    breakEvenYear = breakEvenYearVal,
                                    plannedYears = plannedYears,
                                    projectionList = projectionList
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("download_pdf_button_tablet"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share PDF"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when(lang) {
                                LanguageCode.ES -> "Descargar Reporte PDF"
                                LanguageCode.FR -> "Télécharger le Rapport PDF"
                                LanguageCode.DE -> "PDF-Bericht herunterladen"
                                LanguageCode.HI -> "पीडीएफ रिपोर्ट डाउनलोड करें"
                                LanguageCode.TA -> "PDF அறிக்கை பதிவிறக்கு"
                                else -> "Download Analysis (PDF)"
                            }
                        )
                    }
                    RentVsBuyProjectionsList(lang, cur, projectionList, plannedYears, showExtendedProjection)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RentVsBuyHeader(lang, breakEvenYearVal)
                RentVsBuyInputs(
                    lang = lang,
                    cur = cur,
                    rentInput = rentInput,
                    onRentChange = { rentInput = it },
                    rentIncreaseInput = rentIncreaseInput,
                    onRentIncreaseChange = { rentIncreaseInput = it },
                    rentersInsInput = rentersInsInput,
                    onRentersInsChange = { rentersInsInput = it },
                    homePriceInput = homePriceInput,
                    onHomePriceChange = { homePriceInput = it },
                    downPaymentInput = downPaymentInput,
                    onDownPaymentChange = { downPaymentInput = it },
                    interestRateInput = interestRateInput,
                    onInterestRateChange = { interestRateInput = it },
                    loanTermInput = loanTermInput,
                    onLoanTermChange = { loanTermInput = it },
                    taxRateInput = taxRateInput,
                    onTaxRateChange = { taxRateInput = it },
                    homeInsInput = homeInsInput,
                    onHomeInsChange = { homeInsInput = it },
                    registrationTaxInput = registrationTaxInput,
                    onRegistrationTaxChange = { registrationTaxInput = it },
                    maintenanceInput = maintenanceInput,
                    onMaintenanceChange = { maintenanceInput = it },
                    appreciationInput = appreciationInput,
                    onAppreciationChange = { appreciationInput = it },
                    plannedYearsInput = plannedYearsInput,
                    onPlannedYearsChange = { plannedYearsInput = it },
                    onHelpClick = { activeGlossaryTerm = it }
                )
                RentVsBuyDashboard(
                    lang = lang,
                    cur = cur,
                    projectionList = projectionList,
                    breakEvenYear = breakEvenYearVal,
                    plannedYears = plannedYears,
                    showExtendedProjection = showExtendedProjection,
                    onToggleExtendedProjection = { showExtendedProjection = it },
                    onHelpClick = { activeGlossaryTerm = it }
                )
                val targetRow = if (showExtendedProjection) projectionList.last() else projectionList.firstOrNull { it.year == plannedYears } ?: projectionList.last()
                RentVsBuyExplanationCard(
                    lang = lang,
                    cur = cur,
                    breakEvenYear = breakEvenYearVal,
                    plannedYears = plannedYears,
                    rentCumulativeSpend = targetRow.rentCumulativeSpend,
                    buyEquity = targetRow.buyEquity,
                    homePrice = homePriceVal,
                    rentMonthly = rentMonthly,
                    appreciationVal = appreciationVal,
                    rentIncrease = rentIncrease
                )
                Button(
                    onClick = {
                        val activity = context.findActivity()
                        if (activity != null) {
                            InterstitialAdHelper.showAdIfReady(activity) {
                                PdfReportExporter.generateAndShareRentVsBuyPdf(
                                    context = context,
                                    lang = lang,
                                    rentMonthly = rentMonthly,
                                    rentIncrease = rentIncrease,
                                    rentersIns = rentersIns,
                                    homePriceVal = homePriceVal,
                                    downPaymentVal = downPaymentVal,
                                    interestRateVal = interestRateVal,
                                    loanTermYears = loanTermYears,
                                    taxRateVal = taxRateVal,
                                    homeInsVal = homeInsVal,
                                    maintenanceVal = maintenanceVal,
                                    appreciationVal = appreciationVal,
                                    registrationTaxVal = registrationTaxVal,
                                    breakEvenYear = breakEvenYearVal,
                                    plannedYears = plannedYears,
                                    projectionList = projectionList
                                )
                            }
                        } else {
                            PdfReportExporter.generateAndShareRentVsBuyPdf(
                                context = context,
                                lang = lang,
                                rentMonthly = rentMonthly,
                                rentIncrease = rentIncrease,
                                rentersIns = rentersIns,
                                homePriceVal = homePriceVal,
                                downPaymentVal = downPaymentVal,
                                interestRateVal = interestRateVal,
                                loanTermYears = loanTermYears,
                                taxRateVal = taxRateVal,
                                homeInsVal = homeInsVal,
                                maintenanceVal = maintenanceVal,
                                appreciationVal = appreciationVal,
                                registrationTaxVal = registrationTaxVal,
                                breakEvenYear = breakEvenYearVal,
                                plannedYears = plannedYears,
                                projectionList = projectionList
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("download_pdf_button_mobile"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share PDF"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when(lang) {
                            LanguageCode.ES -> "Descargar Reporte PDF"
                            LanguageCode.FR -> "Télécharger le Rapport PDF"
                            LanguageCode.DE -> "PDF-Bericht herunterladen"
                            LanguageCode.HI -> "पीडीएफ रिपोर्ट डाउनलोड करें"
                            LanguageCode.TA -> "PDF அறிக்கை பதிவிறக்கு"
                            else -> "Download Analysis (PDF)"
                        }
                    )
                }
                RentVsBuyProjectionsList(lang, cur, projectionList, plannedYears, showExtendedProjection)
            }
        }
    }
}

@Composable
fun RentVsBuyHeader(lang: LanguageCode, breakEvenYear: Int?) {
    val messageText = if (breakEvenYear != null) {
        when(lang) {
            LanguageCode.ES -> "👍 ¡Comprar se vuelve financieramente lucrativo después de $breakEvenYear años!"
            LanguageCode.FR -> "👍 L'achat devient avantageux après $breakEvenYear ans !"
            LanguageCode.DE -> "👍 Kaufen wird nach $breakEvenYear Jahren finanziell vorteilhaft!"
            LanguageCode.HI -> "👍 $breakEvenYear वर्षों के बाद खरीदना आर्थिक रूप से अधिक लाभदायक हो जाता है!"
            LanguageCode.TA -> "👍 $breakEvenYear ஆண்டுகளுக்குப் பிறகு வாங்குவது சிறந்த நிதி லாபத்தை அளிக்கிறது!"
            else -> "👍 Buying outperforms renting after $breakEvenYear years!"
        }
    } else {
        when(lang) {
            LanguageCode.ES -> "🏠 Alquilar sigue siendo financieramente óptimo debido a altas tasas o bajo crecimiento."
            LanguageCode.FR -> "🏠 Louer reste financièrement optimal en raison de taux élevés ou d'une faible croissance."
            LanguageCode.DE -> "🏠 Mieten bleibt aufgrund hoher Zinsen oder geringem Wachstum finanziell optimal."
            LanguageCode.HI -> "🏠 उच्च दरों या कम विकास के कारण किराए पर रहना वित्तीय रूप से सर्वोत्तम रहता है।"
            LanguageCode.TA -> "🏠 உயர் வட்டி வீதம் அல்லது குறைந்த வீட்டின் வளர்ச்சி காரணமாக தொடர்ந்து வாடகைக்கு இருப்பதே சிறந்தது."
            else -> "🏠 Renting remains more cost-effective over this 30-year span under these specifications."
        }
    }

    val bannerColor = if (breakEvenYear != null) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bannerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(if (breakEvenYear != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, CircleShape)
                    .padding(8.dp)
            ) {
                Text(if (breakEvenYear != null) "✨" else "🔑", fontSize = 20.sp)
            }
            Column {
                Text(
                    text = when(lang) {
                        LanguageCode.ES -> "Análisis de Alquiler vs Compra"
                        LanguageCode.FR -> "Analyse Louer vs Acheter"
                        LanguageCode.DE -> "Mieten vs. Kaufen Analyse"
                        LanguageCode.HI -> "किराया बनाम खरीद विश्लेषण"
                        LanguageCode.TA -> "வாடகை vs கொள்முதல் பகுப்பாய்வு"
                        else -> "Rent Vs Buy Analysis"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (breakEvenYear != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = messageText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun RentVsBuyInputs(
    lang: LanguageCode,
    cur: String,
    rentInput: String,
    onRentChange: (String) -> Unit,
    rentIncreaseInput: String,
    onRentIncreaseChange: (String) -> Unit,
    rentersInsInput: String,
    onRentersInsChange: (String) -> Unit,
    homePriceInput: String,
    onHomePriceChange: (String) -> Unit,
    downPaymentInput: String,
    onDownPaymentChange: (String) -> Unit,
    interestRateInput: String,
    onInterestRateChange: (String) -> Unit,
    loanTermInput: String,
    onLoanTermChange: (String) -> Unit,
    taxRateInput: String,
    onTaxRateChange: (String) -> Unit,
    homeInsInput: String,
    onHomeInsChange: (String) -> Unit,
    registrationTaxInput: String,
    onRegistrationTaxChange: (String) -> Unit,
    maintenanceInput: String,
    onMaintenanceChange: (String) -> Unit,
    appreciationInput: String,
    onAppreciationChange: (String) -> Unit,
    plannedYearsInput: String,
    onPlannedYearsChange: (String) -> Unit,
    onHelpClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Stay Planning
            Text(
                text = when(lang) {
                    LanguageCode.ES -> "Planificación de la Estancia"
                    LanguageCode.FR -> "Planification du Séjour"
                    LanguageCode.DE -> "Wohnzeit-Planung"
                    LanguageCode.HI -> "रहने की योजना"
                    LanguageCode.TA -> "வசிக்கும் காலம்"
                    else -> "Strategic Target Stay"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = plannedYearsInput,
                onValueChange = { onPlannedYearsChange(coerceInputString(it, 30.0, true)) },
                label = { Text(when(lang) {
                    LanguageCode.ES -> "Años que planea vivir allí (1-30)"
                    LanguageCode.FR -> "Années de résidence prévues (1-30)"
                    LanguageCode.DE -> "Geplante Wohndauer in Jahren (1-30)"
                    LanguageCode.HI -> "वहां रहने की योजनाबद्ध वर्ष (1-30)"
                    LanguageCode.TA -> "நீங்கள் அங்கு வசிக்கும் ஆண்டுகள் (1-30)"
                    else -> "Years Planning to Live There (1-30)"
                }) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("planned_years_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    IconButton(onClick = { onHelpClick("STAY") }) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Stay Info")
                    }
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))

            // Section RENT
            Text(
                text = when(lang) {
                    LanguageCode.ES -> "Escenario de Alquiler"
                    LanguageCode.FR -> "Option Location"
                    LanguageCode.DE -> "Miet-Szenario"
                    LanguageCode.HI -> "किराए का परिदृश्य"
                    LanguageCode.TA -> "வாடகை விவரம்"
                    else -> "Rent Scenario Specifications"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rentInput,
                    onValueChange = { onRentChange(coerceInputString(it, 50000.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Alquiler Mensual ($cur)"
                        LanguageCode.FR -> "Loyer Mensuel ($cur)"
                        LanguageCode.DE -> "Monatliche Miete ($cur)"
                        LanguageCode.HI -> "मासिक किराया ($cur)"
                        LanguageCode.TA -> "மாத வாடகை ($cur)"
                        else -> "Monthly Rent ($cur)"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f).testTag("rent_val_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = rentIncreaseInput,
                    onValueChange = { onRentIncreaseChange(coerceInputString(it, 20.0, false)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Crecimiento %"
                        LanguageCode.FR -> "Hausse %"
                        LanguageCode.DE -> "Steigerung %"
                        LanguageCode.HI -> "वृद्धि %"
                        LanguageCode.TA -> "வட்டி %"
                        else -> "Rent Inc %"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("rent_increase_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            OutlinedTextField(
                value = rentersInsInput,
                onValueChange = { onRentersInsChange(coerceInputString(it, 1000.0, true)) },
                label = { Text(when(lang) {
                    LanguageCode.ES -> "Seguro de Inquilino Mensual ($cur)"
                    LanguageCode.FR -> "Assurance Locataire Mensuel ($cur)"
                    LanguageCode.DE -> "Mieter-Versicherung /Monat ($cur)"
                    LanguageCode.HI -> "किरायेदार का मासिक बीमा ($cur)"
                    LanguageCode.TA -> "வாடகைதாரர் காப்பீடு /மாதம் ($cur)"
                    else -> "Renters Insurance ($cur/mo)"
                }) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("rent_ins_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    IconButton(onClick = { onHelpClick("PMI") }) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Renters Insurance Help")
                    }
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))

            // Section BUY
            Text(
                text = when(lang) {
                    LanguageCode.ES -> "Escenario de Compra"
                    LanguageCode.FR -> "Option Achat"
                    LanguageCode.DE -> "Kauf-Szenario"
                    LanguageCode.HI -> "खरीद का परिदृश्य"
                    LanguageCode.TA -> "கடன் விவரம்"
                    else -> "Buy Scenario Specifications"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = homePriceInput,
                    onValueChange = { onHomePriceChange(coerceInputString(it, 100000000.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Valor Vivienda ($cur)"
                        LanguageCode.FR -> "Prix du bien ($cur)"
                        LanguageCode.DE -> "Kaufpreis ($cur)"
                        LanguageCode.HI -> "घर का मूल्य ($cur)"
                        LanguageCode.TA -> "வீட்டின் விலை ($cur)"
                        else -> "Home Price ($cur)"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1.1f).testTag("buy_price_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = downPaymentInput,
                    onValueChange = { onDownPaymentChange(coerceInputString(it, 100000000.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Enganche ($cur)"
                        LanguageCode.FR -> "Apport ($cur)"
                        LanguageCode.DE -> "Anzahlung ($cur)"
                        LanguageCode.HI -> "डाउन पेमेंट ($cur)"
                        LanguageCode.TA -> "முன்பணம் ($cur)"
                        else -> "Down Payment ($cur)"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("buy_down_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = { onHelpClick("LTV") }) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "Down Payment Help")
                        }
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = interestRateInput,
                    onValueChange = { onInterestRateChange(coerceInputString(it, 30.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Tasa %"
                        LanguageCode.FR -> "Taux %"
                        LanguageCode.DE -> "Zins %"
                        LanguageCode.HI -> "ब्याज %"
                        LanguageCode.TA -> "வட்டி %"
                        else -> "Interest Rate %"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1.1f).testTag("buy_rate_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = loanTermInput,
                    onValueChange = { onLoanTermChange(coerceInputString(it, 50.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Plazo (Años)"
                        LanguageCode.FR -> "Durée (Ans)"
                        LanguageCode.DE -> "Laufzeit (Jahre)"
                        LanguageCode.HI -> "अवधि (वर्ष)"
                        LanguageCode.TA -> "காலம் (ஆண்டுகள்)"
                        else -> "Years"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("buy_term_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = taxRateInput,
                    onValueChange = { onTaxRateChange(coerceInputString(it, 10.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Impuesto Propiedad %"
                        LanguageCode.FR -> "Taxe Foncière %"
                        LanguageCode.DE -> "Grundsteuer %"
                        LanguageCode.HI -> "संपत्ति कर %"
                        LanguageCode.TA -> "சொத்து வரி %"
                        else -> "Property Tax %"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1.1f).testTag("buy_tax_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = homeInsInput,
                    onValueChange = { onHomeInsChange(coerceInputString(it, 50000.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Seguro ($cur/año)"
                        LanguageCode.FR -> "Assurance ($cur/an)"
                        LanguageCode.DE -> "Versicherung ($cur/J)"
                        LanguageCode.HI -> "बीमा ($cur/वर्ष)"
                        LanguageCode.TA -> "காப்பீடு ($cur/ஆண்டு)"
                        else -> "Home Ins. ($cur/yr)"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("buy_ins_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = registrationTaxInput,
                    onValueChange = { onRegistrationTaxChange(coerceInputString(it, 20.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Impuesto de Registro %"
                        LanguageCode.FR -> "Droits de Mutation %"
                        LanguageCode.DE -> "Grunderwerbsteuer %"
                        LanguageCode.HI -> "पंजीकरण कर %"
                        LanguageCode.TA -> "பதிவு வரி %"
                        else -> "Closing Cost %"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1.1f).testTag("buy_reg_tax_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = maintenanceInput,
                    onValueChange = { onMaintenanceChange(coerceInputString(it, 10.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Mantenimiento %"
                        LanguageCode.FR -> "Entretien %"
                        LanguageCode.DE -> "Instandhaltung %"
                        LanguageCode.HI -> "रखरखाव %"
                        LanguageCode.TA -> "பராமரிப்பு %"
                        else -> "Maintenance %"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("buy_maint_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = appreciationInput,
                    onValueChange = { onAppreciationChange(coerceInputString(it, 20.0, true)) },
                    label = { Text(when(lang) {
                        LanguageCode.ES -> "Apreciación %"
                        LanguageCode.FR -> "Appréciation %"
                        LanguageCode.DE -> "Wertsteigerung %"
                        LanguageCode.HI -> "वार्षिक प्रशंसा %"
                        LanguageCode.TA -> "மதிப்பு உயர்வு %"
                        else -> "Appreciation %"
                    }) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("buy_appr_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = { onHelpClick("ROI") }) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "ROI Help")
                        }
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun RentVsBuyDashboard(
    lang: LanguageCode,
    cur: String,
    projectionList: List<YearlyComparisonRow>,
    breakEvenYear: Int?,
    plannedYears: Int,
    showExtendedProjection: Boolean,
    onToggleExtendedProjection: (Boolean) -> Unit,
    onHelpClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when(lang) {
                        LanguageCode.ES -> if (showExtendedProjection) "Cuadro de mando (Plazo completo 30 Años)" else "Cuadro de mando ($plannedYears Años)"
                        LanguageCode.FR -> if (showExtendedProjection) "Tableau (Global 30 Ans)" else "Tableau ($plannedYears Ans)"
                        LanguageCode.DE -> if (showExtendedProjection) "Finanzübersicht (Komplett 30 Jahre)" else "Finanzübersicht ($plannedYears Jahre)"
                        LanguageCode.HI -> if (showExtendedProjection) "सारांश (पूर्ण 30 वर्ष)" else "सारांश ($plannedYears वर्ष)"
                        LanguageCode.TA -> if (showExtendedProjection) "சுருக்கம் (முழு 30 ஆண்டுகள்)" else "சுருக்கம் ($plannedYears ஆண்டுகள்)"
                        else -> if (showExtendedProjection) "Financial Overview (30-Year Complete)" else "Financial Overview (Year $plannedYears)"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "30-Yr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = showExtendedProjection,
                        onCheckedChange = onToggleExtendedProjection,
                        modifier = Modifier.scale(0.7f)
                    )
                }
            }

            val targetRow = if (showExtendedProjection) projectionList.last() else projectionList.firstOrNull { it.year == plannedYears } ?: projectionList.last()
            val targetYearLabel = if (showExtendedProjection) "30" else plannedYears.toString()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when(lang) {
                            LanguageCode.ES -> "Rent Cumulative (Rentabilidad)"
                            LanguageCode.FR -> "Location Cumulé"
                            LanguageCode.DE -> "Miete Kumuliert"
                            LanguageCode.HI -> "किराया संचयी व्यय"
                            LanguageCode.TA -> "வாடகை மொத்த செலவு"
                            else -> "Rent Total Spend"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$cur ${String.format("%,.0f2", targetRow.rentCumulativeSpend).replace(".0f2", "")}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when(lang) {
                                LanguageCode.ES -> "Buy Net Cost (Costo Neto)"
                                LanguageCode.FR -> "Achat Coût Net"
                                LanguageCode.DE -> "Kauf Netto-Zahlung"
                                LanguageCode.HI -> "खरीदने की शुद्ध लागत"
                                LanguageCode.TA -> "வாங்குதலின் நிகர மதிப்பு"
                                else -> "Buy Net Cost"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { onHelpClick("BrokerFee") }, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "Broker Fee Info", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = "$cur ${String.format("%,.0f2", targetRow.buyNetCost).replace(".0f2", "")}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (targetRow.buyNetCost < targetRow.rentCumulativeSpend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when(lang) {
                        LanguageCode.ES -> "Detalle de los Bienes Adquiridos"
                        LanguageCode.FR -> "Capitalisation de l'actif immobilier"
                        LanguageCode.DE -> "Investition- & Vermögensbildung"
                        LanguageCode.HI -> "निवेश मूल्य सृजन"
                        LanguageCode.TA -> "வீட்டின் சொத்து மதிப்பு விவரம்"
                        else -> "Home Value Assets Created"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when(lang) {
                            LanguageCode.ES -> "Valor Estimado (Año $targetYearLabel)"
                            LanguageCode.FR -> "Valorisation estimée (Ans $targetYearLabel)"
                            LanguageCode.DE -> "Geschätzter Wert (Jahr $targetYearLabel)"
                            LanguageCode.HI -> "अनुमानित मूल्य (वर्ष $targetYearLabel)"
                            LanguageCode.TA -> "மதிப்பு ($targetYearLabel-ஆம் ஆண்டு)"
                            else -> "Est. Property Value (Year $targetYearLabel)"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "$cur ${String.format("%,.0f2", targetRow.buyHomeValue).replace(".0f2", "")}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when(lang) {
                            LanguageCode.ES -> "Capital acumulado libre de deudas"
                            LanguageCode.FR -> "Capital acquis (Libre de dette)"
                            LanguageCode.DE -> "Gebildetes Eigenkapital (Schuldenfrei)"
                            LanguageCode.HI -> "निर्मित संचित इक्विटी"
                            LanguageCode.TA -> "உங்களது சொந்த பங்கு மதிப்பு"
                            else -> "Equity Acquired (Debt-Free)"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "$cur ${String.format("%,.0f2", targetRow.buyEquity).replace(".0f2", "")}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun RentVsBuyProjectionsList(
    lang: LanguageCode,
    cur: String,
    projectionList: List<YearlyComparisonRow>,
    plannedYears: Int,
    showExtendedProjection: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = when(lang) {
                    LanguageCode.ES -> "Tabla de Amortización Comparada / ROI"
                    LanguageCode.FR -> "Amortissement Comparatif / ROI"
                    LanguageCode.DE -> "Wertentwicklungstabelle / ROI"
                    LanguageCode.HI -> "तुलनात्मक परिशोधन / ROI तालिका"
                    LanguageCode.TA -> "வாடகை vs கொள்முதல் ஒப்பிடுதல் அட்டவணை"
                    else -> "Yearly Net Cost & ROI Projections"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when(lang) {
                        LanguageCode.ES -> "Año"
                        LanguageCode.FR -> "An"
                        LanguageCode.DE -> "Jahr"
                        LanguageCode.HI -> "वर्ष"
                        LanguageCode.TA -> "வ"
                        else -> "Year"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.5f)
                )
                Text(
                    text = when(lang) {
                        LanguageCode.ES -> "Rent Cum."
                        LanguageCode.FR -> "Loc. Cumul."
                        LanguageCode.DE -> "Miete Kum."
                        LanguageCode.HI -> "किराया सं."
                        LanguageCode.TA -> "வாடகை"
                        else -> "Rent Cum."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = when(lang) {
                        LanguageCode.ES -> "Comprar Costo"
                        LanguageCode.FR -> "Achat Coût"
                        LanguageCode.DE -> "Kauf Netto"
                        LanguageCode.HI -> "शुद्ध खरीद"
                        LanguageCode.TA -> "வாங்குதல்"
                        else -> "Buy Net Cost"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = when(lang) {
                        LanguageCode.ES -> "Capital Comp."
                        LanguageCode.FR -> "Achat Équité"
                        LanguageCode.DE -> "Kauf Eigenk."
                        LanguageCode.HI -> "इक्विटी"
                        LanguageCode.TA -> "பங்கு"
                        else -> "Buy Equity"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.2f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Show selected projection years representatively
            val standardYears = listOf(1, 2, 3, 5, 7, 10, 15, 20, 25, 30)
            val selectedYears = if (showExtendedProjection) {
                standardYears
            } else {
                (standardYears.filter { it < plannedYears } + plannedYears).distinct().sorted()
            }
            
            selectedYears.forEach { yr ->
                val row = projectionList.firstOrNull { it.year == yr }
                if (row != null) {
                    val isFavorable = row.isBreakEven
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isFavorable) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Yr $yr",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.5f)
                        )
                        Text(
                            text = "$cur ${String.format("%,.0f2", row.rentCumulativeSpend).replace(".0f2", "")}",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.1f)
                        )
                        Text(
                            text = "$cur ${String.format("%,.0f2", row.buyNetCost).replace(".0f2", "")}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isFavorable) FontWeight.Bold else FontWeight.Normal,
                            color = if (isFavorable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            text = "$cur ${String.format("%,.0f2", row.buyEquity).replace(".0f2", "")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RentVsBuyExplanationCard(
    lang: LanguageCode,
    cur: String,
    breakEvenYear: Int?,
    plannedYears: Int,
    rentCumulativeSpend: Double,
    buyEquity: Double,
    homePrice: Double,
    rentMonthly: Double,
    appreciationVal: Double,
    rentIncrease: Double
) {
    // Determine target stay outcome
    val recommendationType = if (breakEvenYear == null) {
        "RENT_STRICT"
    } else if (plannedYears >= breakEvenYear) {
        "BUY_WIN"
    } else {
        "RENT_WIN"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Recommendation Alert Banner Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (recommendationType) {
                        "BUY_WIN" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = if (recommendationType == "BUY_WIN") "🎉" else "🚪", fontSize = 24.sp)
                    Column {
                        Text(
                            text = when (recommendationType) {
                                "BUY_WIN" -> when(lang) {
                                    LanguageCode.ES -> "Recomendación: ¡Comprar es Mejor!"
                                    LanguageCode.FR -> "Recommandation : Achetez !"
                                    LanguageCode.DE -> "Empfehlung: Kaufen ist besser!"
                                    LanguageCode.HI -> "सिफारिश: खरीदना बेहतर है!"
                                    LanguageCode.TA -> "பரிந்துரை: வாங்குவதே சிறந்தது!"
                                    else -> "Recommendation: Buying is Favorable!"
                                }
                                "RENT_WIN" -> when(lang) {
                                    LanguageCode.ES -> "Recomendación: Alquilar es Mejor para su Estancia"
                                    LanguageCode.FR -> "Recommandation : Louez pour votre durée"
                                    LanguageCode.DE -> "Empfehlung: Mieten lohnt sich eher"
                                    LanguageCode.HI -> "सिफारिश: कम रहने के लिए किराया बेहतर है"
                                    LanguageCode.TA -> "பரிந்துரை: உங்களது காலத்திற்கு வாடகையே சிறந்தது"
                                    else -> "Recommendation: Renting is Favorable for Short Stay"
                                }
                                else -> when(lang) {
                                    LanguageCode.ES -> "Recomendación: Alquilar es Óptimo"
                                    LanguageCode.FR -> "Recommandation : La Location est Optimale"
                                    LanguageCode.DE -> "Empfehlung: Mieten ist optimal"
                                    LanguageCode.HI -> "सिफारिश: किराया सर्वोत्तम है"
                                    LanguageCode.TA -> "பரிந்துரை: வாடகையே சிறந்தது"
                                    else -> "Recommendation: Renting is Highly Favorable"
                                }
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (recommendationType == "BUY_WIN") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )

                        Text(
                            text = when (recommendationType) {
                                "BUY_WIN" -> when(lang) {
                                    LanguageCode.ES -> "Su estancia planeada ($plannedYears años) es superior o igual al punto de equilibrio ($breakEvenYear años). Conservar la casa creará un patrimonio sustancial."
                                    LanguageCode.FR -> "Votre séjour prévu ($plannedYears ans) dépasse le point de bascule ($breakEvenYear ans). Conserver le bien créera de la richesse brute."
                                    LanguageCode.DE -> "Ihre geplante Wohndauer ($plannedYears Jahre) liegt über oder auf der Gewinnschwelle ($breakEvenYear Jahre). Wohneigentum lohnt sich hier!"
                                    LanguageCode.HI -> "आपकी नियोजित रहने की अवधि ($plannedYears वर्ष) ब्रेक-ईवन बिंदु ($breakEvenYear वर्ष) के बराबर या उससे अधिक है। इसलिए खरीदना बुद्धिमानी है।"
                                    LanguageCode.TA -> "நீங்கள் திட்டமிட்டுள்ள காலம் ($plannedYears ஆண்டுகள்) உங்களது சமநிலை ஆண்டைவிட ($breakEvenYear ஆண்டுகள்) அதிகமாகவோ அல்லது சமமாகவோ இருப்பதால் வீட்டை வாங்குவது பெரும் சொத்தை உருவாக்கும்."
                                    else -> "Your planned stay ($plannedYears years) is longer than or equal to the financial crossover point ($breakEvenYear years). Keeping this home builds substantial long term wealth."
                                }
                                "RENT_WIN" -> when(lang) {
                                    LanguageCode.ES -> "Su estancia planeada ($plannedYears años) es inferior al punto de equilibrio ($breakEvenYear años). Alquilar le ahorrará costos de cierre no recuperables."
                                    LanguageCode.FR -> "Votre séjour prévu ($plannedYears ans) est inférieur au point d'équilibre de l'achat ($breakEvenYear ans). Préférer la location évite les pertes d'entrée."
                                    LanguageCode.DE -> "Ihre geplante Wohndauer ($plannedYears Jahre) liegt unter der Gewinnschwelle ($breakEvenYear Jahre). Mieten spart Ihnen nicht erstattungsfähige Kauf- und Verkaufskosten."
                                    LanguageCode.HI -> "आपकी नियोजित रहने की अवधि ($plannedYears वर्ष) ब्रेक-ईवन बिंदु ($breakEvenYear वर्ष) से कम है। किराए पर रहने से गैर-वसूलने योग्य अतिरिक्त खर्चों की बचत होगी।"
                                    LanguageCode.TA -> "நீங்கள் திட்டமிட்டுள்ள காலம் ($plannedYears ஆண்டுகள்) உங்களது சமநிலை ஆண்டைவிட ($breakEvenYear ஆண்டுகள்) குறைவாக இருப்பதால் வாடகைக்கு இருப்பதே சரி, இது கூடுதல் ஆரம்ப கட்டணங்களை தவிர்க்க உதவும்."
                                    else -> "Your planned stay ($plannedYears years) is shorter than the financial break-even point ($breakEvenYear years). Moving out early triggers heavy transaction costs, making renting the safer strategy."
                                }
                                else -> when(lang) {
                                    LanguageCode.ES -> "Alquilar sigue siendo superior durante los 30 años debido a tasas de interés altas o crecimiento inmobiliario moderado."
                                    LanguageCode.FR -> "Louer reste préférable sur toute la période de 30 ans en raison des taux élevés ou d'une revalorisation immobilière limitée."
                                    LanguageCode.DE -> "Mieten bleibt über die gesamten 30 Jahre hinweg die bessere Option, da die Zinsen hoch oder die Immobilienwertsteigerung moderat sind."
                                    LanguageCode.HI -> "उच्च ब्याज दरों या कम संपत्ति विकास के कारण पूरे 30 वर्षों की अवधि में किराए पर रहना ही सर्वोत्तम है।"
                                    LanguageCode.TA -> "உயர் வட்டி விகிதம் அல்லது குறைந்த வீட்டின் வளர்ச்சி காரணமாக தொடர்ந்து வாடகைக்கு இருப்பதே சிறந்தது."
                                    else -> "Under these specifications, renting remains strictly superior across the full 30-year span. Buying fails to amortize upfront transactional costs."
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = when(lang) {
                    LanguageCode.ES -> "💡 Análisis Estratégico: Alquilar vs Comprar"
                    LanguageCode.FR -> "💡 Analyse Stratégique : Louer vs Acheter"
                    LanguageCode.DE -> "💡 Strategische Analyse: Mieten vs. Kaufen"
                    LanguageCode.HI -> "💡 रणनीतिक विश्लेषण: किराया बनाम खरीद"
                    LanguageCode.TA -> "💡 Viewpoint பகுப்பாய்வு: வாடகை vs கொள்முதல்"
                    else -> "💡 Strategic Analysis: Rent vs Buy"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            if (breakEvenYear != null) {
                // Renting Advantage Short Term
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = when(lang) {
                            LanguageCode.ES -> "🏁 Corto Plazo: El beneficio de Alquilar (Años 1 a ${breakEvenYear - 1})"
                            LanguageCode.FR -> "🏁 Court Terme : L'avantage de Louer (Ans 1 à ${breakEvenYear - 1})"
                            LanguageCode.DE -> "🏁 Kurzfristig: Der Vorteil von Mieten (Jahre 1 bis ${breakEvenYear - 1})"
                            LanguageCode.HI -> "🏁 लघु अवधि: किराए का लाभ (वर्ष 1 से ${breakEvenYear - 1})"
                            LanguageCode.TA -> "🏁 குறுகிய காலம்: வாடகையின் நன்மை (ஆண்டு 1 முதல் ${breakEvenYear - 1} வரை)"
                            else -> "🏁 Short Term Strategy: Renting Advantage (Years 1 to ${breakEvenYear - 1})"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = when(lang) {
                            LanguageCode.ES -> "Alquilar es mejor inicialmente porque evita los altos costos de compra (como el enganche de $cur ${String.format("%,.0f", homePrice * 0.20)} y costos de cierre). Al principio, los pagos mensuales de la hipoteca se destinan principalmente a intereses no recuperables en lugar de construir capital."
                            LanguageCode.FR -> "Louer est préférable au début car cela évite d'importants frais d'achat (comme l'apport de $cur ${String.format("%,.0f", homePrice * 0.20)} et les frais de notaire). Au départ, les mensualités du prêt servent principalement à payer des intérêts non récupérables."
                            LanguageCode.DE -> "Mieten ist anfangs vorteilhafter, da Sie hohe Kaufnebenkosten und die Anzahlung von $cur ${String.format("%,.0f", homePrice * 0.20)} vermeiden. In den ersten Jahren bestehen die Hypothekenraten fast nur aus Zinsen und tragen kaum zur Tilgung bei."
                            LanguageCode.HI -> "शुरुआत में किराया बेहतर है क्योंकि यह खरीद के उच्च खर्चों (जैसे डाउन पेमेंट $cur ${String.format("%,.0f", homePrice * 0.20)} और पंजीकरण लागत) से बचाता है। शुरुआत में मासिक किश्त का बड़ा हिस्सा ब्याज में जाता है।"
                            LanguageCode.TA -> "ஆரம்ப காலத்தில் வாடகைக்கு இருப்பதே சிறந்தது. ஏனெனில் முன்பணம் ($cur ${String.format("%,.0f", homePrice * 0.20)}) மற்றும் பிற பதிவு கட்டணங்களை தவிர்க்கலாம். ஆரம்பத்தில் கடன் வட்டியே அதிகமாக இருக்கும்."
                            else -> "Renting is financially superior initially because it avoids high transactional friction (such as a down payment and closing costs). In the early years, buying carries heavy front-loaded mortgage interest payments that do not build asset equity."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f))

                // Buying Advantage Long Term
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = when(lang) {
                            LanguageCode.ES -> "📈 Largo Plazo: El beneficio de Comprar (Años $breakEvenYear a 30)"
                            LanguageCode.FR -> "📈 Long Terme : L'avantage d'Acheter (Ans $breakEvenYear à 30)"
                            LanguageCode.DE -> "📈 Langfristig: Der Vorteil von Kaufen (Jahre $breakEvenYear bis 30)"
                            LanguageCode.HI -> "📈 दीर्घकालिक रणनीति: खरीदने का लाभ (वर्ष $breakEvenYear से 30)"
                            LanguageCode.TA -> "📈 நீண்ட காலம்: வாங்குதலின் நன்மை (ஆண்டு $breakEvenYear முதல் 30 வரை)"
                            else -> "📈 Long Term Strategy: Buying Advantage (Years $breakEvenYear to 30)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = when(lang) {
                            LanguageCode.ES -> "A partir del año $breakEvenYear, comprar se vuelve superior. El crecimiento anual estimado de la vivienda y la amortización del saldo de su préstamo construyen un patrimonio real. Al año 30, usted habrá acumulado un capital libre de deuda de $cur ${String.format("%,.0f", buyEquity)}, mientras que alquilar representaría un gasto tirado a la basura de $cur ${String.format("%,.0f", rentCumulativeSpend)}."
                            LanguageCode.FR -> "Dès l'année $breakEvenYear, l'achat devient rentable. La revalorisation de la maison et le remboursement du prêt vous créent un capital solide. À la 30ème année, vous possédez un patrimoine net de $cur ${String.format("%,.0f", buyEquity)} libéré de tout crédit, alors que louer vous aurait coûté $cur ${String.format("%,.0f", rentCumulativeSpend)} à fonds perdus."
                            LanguageCode.DE -> "Ab dem Jahr $breakEvenYear übertrifft das Kaufen das Mieten. Die Wertsteigerung der Immobilie und die kontinuierliche Tilgung schaffen echtes Vermögen. Nach 30 Jahren besitzen Sie ein schuldenfreies Eigenkapital von $cur ${String.format("%,.0f", buyEquity)}, während Mieten reine Ausgaben von $cur ${String.format("%,.0f", rentCumulativeSpend)} ohne Gegenwert wären."
                            LanguageCode.HI -> "वर्ष $breakEvenYear से, खरीदना अधिक फायदेमंद हो जाता है। संपत्ति के मूल्य में वृद्धि और धीरे-धीरे ऋण का भुगतान वास्तविक संपत्ति का निर्माण करता है। 30वें वर्ष में, आपके पास $cur ${String.format("%,.0f", buyEquity)} की ऋण-मुक्त संपत्ति होगी, जबकि संचयी किराया मूल्य $cur ${String.format("%,.0f", rentCumulativeSpend)} शून्य रिटर्न के साथ खर्च होगा।"
                            LanguageCode.TA -> "$breakEvenYear ஆம் ஆண்டு Visual வாங்குவது நன்மை தரும். வீட்டின் மதிப்பு உயர்வும், கடன் தொகையை அடைப்பதும் உங்களின் சொந்த சொத்து மதிப்பை உயர்த்தும். 30 வது முடிவில் உங்களிடம் $cur ${String.format("%,.0f", buyEquity)} மதிப்புள்ள சொந்த வீடு இருக்கும். ஆனால் வாடகைக்கு செலவிட்ட $cur ${String.format("%,.0f", rentCumulativeSpend)} திரும்ப வராது."
                            else -> "Starting in Year $breakEvenYear, buying outpaces renting. The combined force of home value appreciation ($appreciationVal%) and mortgage amortization creates solid wealth. By Year 30, you acquire a debt-free home asset worth $cur ${String.format("%,.0f", buyEquity)} in pure equity, instead of a pure unrecoverable rental cost of $cur ${String.format("%,.0f", rentCumulativeSpend)}."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                // Renting is strictly better
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = when(lang) {
                            LanguageCode.ES -> "❌ Rentabilidad del Alquiler"
                            LanguageCode.FR -> "❌ La Location reste Supérieure"
                            LanguageCode.DE -> "❌ Mieten bleibt wirtschaftlicher"
                            LanguageCode.HI -> "❌ किराए पर रहना आर्थिक रूप से बेहतर है"
                            LanguageCode.TA -> "❌ வாடகைக்கு இருப்பதே தொடர்ந்து சிறந்தது"
                            else -> "❌ Renting Preserves Capital"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = when(lang) {
                            LanguageCode.ES -> "Debido a las tasas actuales, comprar no logra alcanzar el punto de equilibrio en 30 años. Alquilar le permite ahorrar y destinar el dinero inicial (como el enganche y el costo de mantenimiento) a inversiones líquidas con mayor rendimiento, en lugar de hundirlo en intereses bancarios."
                            LanguageCode.FR -> "Compte tenu des taux actuels, l'achat n'atteint pas son point d'équilibre en 30 ans. Louer vous permet d'économiser votre capital de départ pour le placer sur des supports plus rentables, plutôt que de payer des intérêts bancaires élevés."
                            LanguageCode.DE -> "Aufgrund des ungünstigen Verhältnisses von Zinsen zu Wertsteigerung lohnt sich ein Kauf über 30 Jahre hinweg wirtschaftlich nicht. Mieten schützt Ihr Kapital, da Sie die gesparte Anzahlung und Instandhaltungskosten rentabler am Finanzmarkt anlegen können."
                            LanguageCode.HI -> "वर्तमान ब्याज दरों या कम संपत्ति विकास के कारण, 30 वर्षों में खरीद का लाभ नहीं मिलता। किराए पर रहने से आप अपना प्रारंभिक पैसा बचाकर अन्य लाभदायक निवेशों में लगाकर बेहतर रिटर्न प्राप्त कर सकते हैं।"
                            LanguageCode.TA -> "உயர் வட்டி விகிதம் காரணமாக 30 ஆண்டுகளில் சொந்த வீடு வாங்குவது லாபகரமானதாக இல்லை. தொடர்ந்து வாடகைக்கு இருந்து உங்களது சேமிப்பை வேறு முதலீடுகளில் இடுவதன் மூலம் அதிக லாபம் பெறலாம்."
                            else -> "Under these specifications (low home value growth, high interest rate, or property expenses), renting is the optimal decision. It protects you from massive non-recoverable mortgage interest and upkeep fees, keeping your starting capital liquid for high-yielding deployments elsewhere."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

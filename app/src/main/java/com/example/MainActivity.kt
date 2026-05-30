package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlin.math.max
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainScreen()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: LoanCalculatorViewModel = viewModel()) {
  val context = LocalContext.current
  val lang by viewModel.currentLanguage.collectAsState()
  val result by viewModel.calculationResult.collectAsState()
  val loanTypeVal by viewModel.loanType.collectAsState()
  
  var activeTab by remember { mutableStateOf(0) }
  var langMenuExpanded by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            Text(
              text = Translations.get(TranslationKey.APP_TITLE, lang),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              modifier = Modifier.weight(1f)
            )
          }
        },
        actions = {
          // Language selector button
          Box(modifier = Modifier.padding(end = 8.dp)) {
            Row(
              modifier = Modifier
                .clickable { langMenuExpanded = true }
                .background(
                  MaterialTheme.colorScheme.surface,
                  shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("language_selector_btn"),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "${lang.flagEmoji} ${lang.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            
            DropdownMenu(
              expanded = langMenuExpanded,
              onDismissRequest = { langMenuExpanded = false },
              modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
              LanguageCode.values().forEach { option ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = "${option.flagEmoji} ${option.displayName}",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  },
                  onClick = {
                    viewModel.setLanguage(option)
                    langMenuExpanded = false
                  }
                )
              }
            }
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(innerPadding)
    ) {
      
      // Tabs Navigation (Material 3 look)
      TabRow(
        selectedTabIndex = activeTab,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
            color = MaterialTheme.colorScheme.primary
          )
        },
        divider = {
          HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        }
      ) {
        val tabTitles = listOf(
          Translations.get(TranslationKey.CALCULATOR_TAB, lang),
          Translations.get(TranslationKey.SCHEDULE_TAB, lang),
          Translations.get(TranslationKey.TAX_TAB, lang)
        )
        
        tabTitles.forEachIndexed { index, title ->
          Tab(
            selected = activeTab == index,
            onClick = { activeTab = index },
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("tab_${index}"),
            text = {
              Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium
              )
            }
          )
        }
      }

      // Responsive contents container
      Box(modifier = Modifier.weight(1f)) {
        when (activeTab) {
          0 -> CalculatorTab(viewModel, result, lang, loanTypeVal)
          1 -> AmortizationTab(viewModel, result, lang, loanTypeVal)
          2 -> TaxSavingsTab(viewModel, result, lang)
        }
      }
    }
  }
}

@Composable
fun CalculatorTab(
  viewModel: LoanCalculatorViewModel,
  result: CalculationResult,
  lang: LanguageCode,
  loanTypeVal: String
) {
  val curSymbol = lang.currencySymbol
  val scrollState = rememberScrollState()

  BoxWithConstraints {
    val isTablet = maxWidth > 600.dp
    
    if (isTablet) {
      // Tablet dual pane layout side-by-side
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Left Column Inputs
        Column(
          modifier = Modifier
            .weight(1.1f)
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
        ) {
          InputsCard(viewModel, lang, loanTypeVal)
        }
        
        // Right Column Dashboard
        Column(
          modifier = Modifier
            .weight(0.9f)
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
        ) {
          DashboardResultsCard(result, lang, curSymbol, loanTypeVal, viewModel)
        }
      }
    } else {
      // Phone layout vertical scroll
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(scrollState)
          .padding(16.dp)
      ) {
        InputsCard(viewModel, lang, loanTypeVal)
        Spacer(modifier = Modifier.height(16.dp))
        DashboardResultsCard(result, lang, curSymbol, loanTypeVal, viewModel)
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
fun InputsCard(
  viewModel: LoanCalculatorViewModel,
  lang: LanguageCode,
  loanTypeVal: String
) {
  var homePriceInput by remember { mutableStateOf(viewModel.homePrice.value) }
  var downPaymentInput by remember { mutableStateOf(viewModel.downPayment.value) }
  var loanAmtInput by remember { mutableStateOf(viewModel.loanAmountInput.value) }
  var intRateInput by remember { mutableStateOf(viewModel.interestRate.value) }
  var loanTermInput by remember { mutableStateOf(viewModel.loanTermYears.value) }
  var extraPaymentInput by remember { mutableStateOf(viewModel.extraPayment.value) }

  var showAdvancedCosts by remember { mutableStateOf(true) }
  var propTaxInput by remember { mutableStateOf(viewModel.propertyTaxRate.value) }
  var insInput by remember { mutableStateOf(viewModel.homeInsurance.value) }
  var pmiInput by remember { mutableStateOf(viewModel.pmiRate.value) }
  var margTaxInput by remember { mutableStateOf(viewModel.marginalTaxRate.value) }

  val cur = lang.currencySymbol

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("inputs_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = Translations.get(TranslationKey.LOAN_TYPE, lang),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      
      Spacer(modifier = Modifier.height(8.dp))
      
      // Category Badges
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val categories = listOf("Mortgage", "Personal", "Auto")
        categories.forEach { category ->
          val isSelected = loanTypeVal == category
          val label = when (category) {
            "Mortgage" -> Translations.get(TranslationKey.LOAN_TYPE_MORTGAGE, lang)
            "Personal" -> Translations.get(TranslationKey.LOAN_TYPE_PERSONAL, lang)
            else -> Translations.get(TranslationKey.LOAN_TYPE_AUTO, lang)
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 40.dp)
              .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(20.dp)
              )
              .clickable {
                viewModel.loanType.value = category
                viewModel.updateInputs()
              }
              .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Direct Loan Amount vs Mortgage Home Price
      if (loanTypeVal == "Mortgage") {
        OutlinedTextField(
          value = homePriceInput,
          onValueChange = {
            homePriceInput = it
            viewModel.homePrice.value = it
            viewModel.updateInputs()
          },
          label = { Text("Home Price (${cur})") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("home_price_input"),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
          )
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // Down Payment
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = downPaymentInput,
            onValueChange = {
              downPaymentInput = it
              viewModel.downPayment.value = it
              viewModel.updateInputs()
            },
            label = { Text("Down Payment (${cur})") },
            singleLine = true,
            modifier = Modifier
              .weight(1.5f)
              .testTag("down_payment_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
            )
          )

          // Down Payment responsive quick stats
          val limitHp = homePriceInput.toDoubleOrNull() ?: 0.0
          val limitDp = downPaymentInput.toDoubleOrNull() ?: 0.0
          val pct = if (limitHp > 0) (limitDp / limitHp * 100.0) else 0.0
          
          Card(
            modifier = Modifier
              .weight(1f)
              .height(56.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            shape = RoundedCornerShape(4.dp)
          ) {
            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "${pct.format(1)} %",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (pct >= 20.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
              )
              Text(
                text = "DP Equity",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      } else {
        OutlinedTextField(
          value = loanAmtInput,
          onValueChange = {
            loanAmtInput = it
            viewModel.loanAmountInput.value = it
            viewModel.updateInputs()
          },
          label = { Text("${Translations.get(TranslationKey.LOAN_AMOUNT, lang)} (${cur})") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("loan_amount_input"),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
          )
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Interest Rate
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = intRateInput,
          onValueChange = {
            intRateInput = it
            viewModel.interestRate.value = it
            viewModel.updateInputs()
          },
          label = { Text("${Translations.get(TranslationKey.ANNUAL_INTEREST, lang)} (%)") },
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("interest_rate_input"),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
          )
        )
        
        Spacer(modifier = Modifier.width(16.dp))

        // Simple interactive speed adjustments
        IconButton(
          onClick = {
            val curVal = intRateInput.toDoubleOrNull() ?: 0.0
            val newVal = max(0.0, curVal - 0.25).format(2)
            intRateInput = newVal
            viewModel.interestRate.value = newVal
            viewModel.updateInputs()
          },
          modifier = Modifier
            .size(36.dp)
            .background(
              MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
              CircleShape
            )
        ) {
          Text("-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
          onClick = {
            val curVal = intRateInput.toDoubleOrNull() ?: 0.0
            val newVal = (curVal + 0.25).format(2)
            intRateInput = newVal
            viewModel.interestRate.value = newVal
            viewModel.updateInputs()
          },
          modifier = Modifier
            .size(36.dp)
            .background(
              MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
              CircleShape
            )
        ) {
          Text("+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Loan Term
      OutlinedTextField(
        value = loanTermInput,
        onValueChange = {
          loanTermInput = it
          viewModel.loanTermYears.value = it
          viewModel.updateInputs()
        },
        label = { Text(Translations.get(TranslationKey.LOAN_TERM, lang)) },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("loan_term_input"),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Extra Payments slider & input
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = extraPaymentInput,
          onValueChange = {
            extraPaymentInput = it
            viewModel.extraPayment.value = it
            viewModel.updateInputs()
          },
          label = { Text("${Translations.get(TranslationKey.EXTRA_PAYMENT, lang)} (${cur})") },
          singleLine = true,
          modifier = Modifier
            .weight(1.2f)
            .testTag("extra_payment_input"),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
          )
        )
        
        Spacer(modifier = Modifier.width(12.dp))

        // Quick set sliders for extras
        Column(modifier = Modifier.weight(1.8f)) {
          Text(text = "Quick Boost: $cur $extraPaymentInput", style = MaterialTheme.typography.labelSmall)
          Slider(
            value = (extraPaymentInput.toFloatOrNull() ?: 0f).coerceIn(0f, 2000f),
            onValueChange = {
              val formatted = it.toInt().toString()
              extraPaymentInput = formatted
              viewModel.extraPayment.value = formatted
              viewModel.updateInputs()
            },
            valueRange = 0f..2000f,
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.height(24.dp)
          )
        }
      }

      // Mortgage Advanced Taxes/Insurance Collapse Section
      if (loanTypeVal == "Mortgage") {
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showAdvancedCosts = !showAdvancedCosts }
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Taxes, Insurance, and Deductions",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = if (showAdvancedCosts) "▲ Hide" else "▼ Expand",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
          )
        }

        AnimatedVisibility(
          visible = showAdvancedCosts,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically()
        ) {
          Column(modifier = Modifier.padding(top = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = propTaxInput,
                onValueChange = {
                  propTaxInput = it
                  viewModel.propertyTaxRate.value = it
                  viewModel.updateInputs()
                },
                label = { Text("Prop. Tax %") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
              )

              OutlinedTextField(
                value = insInput,
                onValueChange = {
                  insInput = it
                  viewModel.homeInsurance.value = it
                  viewModel.updateInputs()
                },
                label = { Text("Insurance $/yr") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = pmiInput,
                onValueChange = {
                  pmiInput = it
                  viewModel.pmiRate.value = it
                  viewModel.updateInputs()
                },
                label = { Text("PMI %") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
              )

              OutlinedTextField(
                value = margTaxInput,
                onValueChange = {
                  margTaxInput = it
                  viewModel.marginalTaxRate.value = it
                  viewModel.updateInputs()
                },
                label = { Text("Border Tax %") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun DashboardResultsCard(
  result: CalculationResult,
  lang: LanguageCode,
  currency: String,
  loanTypeVal: String,
  viewModel: LoanCalculatorViewModel
) {
  if (!result.isValid) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Text(
        text = Translations.get(TranslationKey.ENTER_VALUES, lang),
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    return
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = Translations.get(TranslationKey.SUMMARY, lang),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Donut Breakdown Visualizer
      val totalFe = result.monthlyPropertyTax + result.monthlyHomeInsurance + result.monthlyPmi
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        BreakdownDonutChart(
          principal = result.baseMonthlyPayment * 0.5, // approximate representation
          interest = result.baseMonthlyPayment * 0.5,
          fees = totalFe,
          totalMonthly = result.totalMonthlyPaymentWithFees,
          currencySymbol = currency,
          modifier = Modifier.padding(8.dp)
        )

        // Explainer Legend Box
        Column(
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.padding(end = 8.dp)
        ) {
          LegendRow(color = MaterialTheme.colorScheme.primary, label = Translations.get(TranslationKey.PRINCIPAL_AND_INTEREST, lang).take(13) + "..")
          if (loanTypeVal == "Mortgage") {
            LegendRow(color = MaterialTheme.colorScheme.secondary, label = Translations.get(TranslationKey.OTHER_FEES, lang))
          }
          LegendRow(color = MaterialTheme.colorScheme.tertiary, label = Translations.get(TranslationKey.EXTRA_PAYMENT, lang))
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.background)
      Spacer(modifier = Modifier.height(12.dp))

      // List key facts
      ResultsRowLabel(
        label = Translations.get(TranslationKey.TOTAL_INTEREST, lang),
        value = "$currency ${String.format("%,.2f", result.totalInterestPaid)}"
      )
      
      if (loanTypeVal == "Mortgage") {
        ResultsRowLabel(
          label = Translations.get(TranslationKey.ESTIMATED_TAX_SAVINGS, lang),
          value = "$currency ${String.format("%,.2f", result.totalTaxSavings)}"
        )
      }

      if (result.savingYearsEarly > 0.0) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
              RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "info",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Paying off ${result.savingYearsEarly.format(1)} Years Early!",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      ResultsRowLabel(
        label = Translations.get(TranslationKey.TOTAL_LOAN_COST, lang),
        value = "$currency ${String.format("%,.2f", result.totalPaidAmount)}",
        isBold = true
      )
    }
  }
}

@Composable
fun LegendRow(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(10.dp)
        .background(color, CircleShape)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun ResultsRowLabel(label: String, value: String, isBold: Boolean = false) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      style = if (isBold) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyMedium,
      fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = if (isBold) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyMedium,
      fontWeight = if (isBold) FontWeight.Bold else FontWeight.Bold,
      color = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
fun AmortizationTab(
  viewModel: LoanCalculatorViewModel,
  result: CalculationResult,
  lang: LanguageCode,
  loanTypeVal: String
) {
  val context = LocalContext.current
  var viewByMonthly by remember { mutableStateOf(false) }

  if (!result.isValid) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(text = Translations.get(TranslationKey.ENTER_VALUES, lang))
    }
    return
  }

  Column(modifier = Modifier.fillMaxSize()) {
    
    // Switch parameters row & PDF button
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Toggle schedule representation
      Row(
        modifier = Modifier
          .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
          .padding(4.dp)
      ) {
        val options = listOf(falseToText(lang), trueToText(lang))
        listOf(false, true).forEachIndexed { index, optionVal ->
          val isSelected = viewByMonthly == optionVal
          Box(
            modifier = Modifier
              .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
              )
              .clickable { viewByMonthly = optionVal }
              .padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(
              text = options[index],
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }

      // PDF Share Button
      Button(
        onClick = {
          val success = PdfReportExporter.generateAndSharePdf(context, result, lang, loanTypeVal)
          if (success) {
            Toast.makeText(context, Translations.get(TranslationKey.PDF_SUCCESS, lang), Toast.LENGTH_LONG).show()
          } else {
            Toast.makeText(context, Translations.get(TranslationKey.PDF_ERROR, lang), Toast.LENGTH_SHORT).show()
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
          .heightIn(min = 40.dp)
          .testTag("export_pdf_button")
      ) {
        Icon(
          imageVector = Icons.Default.Share,
          contentDescription = "export pdf",
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = Translations.get(TranslationKey.EXPORT_PDF, lang), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
      }
    }

    // Scrollable Schedule Grid
    Box(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 16.dp)
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        
        // Table columns header row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = if (viewByMonthly) Translations.get(TranslationKey.MONTH, lang) else Translations.get(TranslationKey.YEAR, lang), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
          Text(text = "Paid", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.End)
          Text(text = "Principal", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.End)
          Text(text = "Interest", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.End)
          Text(text = "Balance", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.End)
        }

        if (viewByMonthly) {
          LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(result.schedule) { item ->
              MonthlyAmortizationRow(item, lang.currencySymbol)
            }
          }
        } else {
          LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(result.yearlySchedule) { item ->
              YearlyAmortizationRow(item, lang.currencySymbol)
            }
          }
        }
      }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
fun MonthlyAmortizationRow(item: AmortizationItem, currentSymbol: String) {
  Column {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(text = "M ${item.monthNumber}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
      Text(text = "$currentSymbol${String.format("%,.0f", item.paymentAmount)}", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
      Text(text = "$currentSymbol${String.format("%,.0f", item.principalPaid)}", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
      Text(text = "$currentSymbol${String.format("%,.0f", item.interestPaid)}", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary, textAlign = TextAlign.End)
      Text(text = "$currentSymbol${String.format("%,.0f", item.remainingBalance)}", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.background)
  }
}

@Composable
fun YearlyAmortizationRow(item: AmortizationYearlyItem, currentSymbol: String) {
  Column {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(text = "Yr ${item.yearNumber}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
      Text(text = "$currentSymbol${String.format("%,.0f", item.paymentAmount)}", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
      Text(text = "$currentSymbol${String.format("%,.0f", item.principalPaid)}", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
      Text(text = "$currentSymbol${String.format("%,.0f", item.interestPaid)}", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary, textAlign = TextAlign.End)
      Text(text = "$currentSymbol${String.format("%,.0f", item.endingBalance)}", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.background)
  }
}

@Composable
fun TaxSavingsTab(
  viewModel: LoanCalculatorViewModel,
  result: CalculationResult,
  lang: LanguageCode
) {
  if (!result.isValid) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(text = Translations.get(TranslationKey.ENTER_VALUES, lang))
    }
    return
  }

  val cur = lang.currencySymbol

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    
    // Overview explaining card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "info tax",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = Translations.get(TranslationKey.TAX_TAB, lang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = Translations.get(TranslationKey.TAX_SAVINGS_EXPLAIN, lang),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Dynamic graphic bar representation of Tax savings per year
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Annual Interest Tax Deduction Advantage ($cur)",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Simple custom simulated graph for vertical bar values of first 10 years
        val slice = result.taxSavingsSchedule.take(10)
        val maxSavings = result.taxSavingsSchedule.maxOfOrNull { it.estimatedTaxSavings } ?: 1.0

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(vertical = 4.dp),
          verticalArrangement = Arrangement.Bottom
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(120.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
          ) {
            slice.forEach { item ->
              val pct = (item.estimatedTaxSavings / maxSavings).toFloat().coerceIn(0.1f, 1f)
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight()
              ) {
                Text(
                  text = "$cur${(item.estimatedTaxSavings / 100).toInt() * 100}",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 8.sp,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Box(
                  modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight(pct)
                    .background(
                      color = MaterialTheme.colorScheme.primary,
                      shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                  text = "Yr ${item.yearNumber}",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 8.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Yearly tax savings table list
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Full Yearly Deduction Schedule",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        result.taxSavingsSchedule.forEach { item ->
          Column {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "${Translations.get(TranslationKey.YEAR, lang)} ${item.yearNumber}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "Saved: $cur${String.format("%,.2f", item.estimatedTaxSavings)}",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = "Interest Paid: $cur${String.format("%,.0f", item.interestPaid)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.background)
          }
        }
      }
    }
  }
}

// Helpers
private fun Double.format(digits: Int) = String.format("%.${digits}f", this)

private fun falseToText(lang: LanguageCode): String {
  return when (lang) {
    LanguageCode.HI -> "वार्षिक"
    LanguageCode.ES -> "Anual"
    LanguageCode.FR -> "Annuel"
    LanguageCode.DE -> "Jährlich"
    else -> "Yearly"
  }
}

private fun trueToText(lang: LanguageCode): String {
  return when (lang) {
    LanguageCode.HI -> "मासिक"
    LanguageCode.ES -> "Mensual"
    LanguageCode.FR -> "Mensuel"
    LanguageCode.DE -> "Monatlich"
    else -> "Monthly"
  }
}

@Composable
fun BreakdownDonutChart(
  principal: Double,
  interest: Double,
  fees: Double,
  totalMonthly: Double,
  currencySymbol: String,
  modifier: Modifier = Modifier
) {
  val total = principal + interest + fees
  if (total <= 0) return

  val principalSweep = (principal / total * 360f).toFloat()
  val interestSweep = (interest / total * 360f).toFloat()
  val feesSweep = (fees / total * 360f).toFloat()

  val colors = listOf(
    MaterialTheme.colorScheme.primary,      // Principal (Emerald)
    MaterialTheme.colorScheme.tertiary,     // Interest (Coral pink)
    MaterialTheme.colorScheme.secondary     // Fees/Insurance (Sage/Mint)
  )

  Box(
    modifier = modifier.size(160.dp),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val strokeWidth = 14.dp.toPx()
      val radius = (size.minDimension - strokeWidth) / 2
      val center = Offset(size.width / 2, size.height / 2)
      val rect = Rect(
        center.x - radius,
        center.y - radius,
        center.x + radius,
        center.y + radius
      )

      var startAngle = -90f

      // Principal arc
      drawArc(
        color = colors[0],
        startAngle = startAngle,
        sweepAngle = principalSweep,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        size = size / 1.15f,
        topLeft = Offset((size.width - size.width/1.15f)/2, (size.height - size.height/1.15f)/2)
      )
      startAngle += principalSweep

      // Interest arc
      drawArc(
        color = colors[1],
        startAngle = startAngle,
        sweepAngle = interestSweep,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        size = size / 1.15f,
        topLeft = Offset((size.width - size.width/1.15f)/2, (size.height - size.height/1.15f)/2)
      )
      startAngle += interestSweep

      // Fees arc
      if (feesSweep > 0) {
        drawArc(
          color = colors[2],
          startAngle = startAngle,
          sweepAngle = feesSweep,
          useCenter = false,
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
          size = size / 1.15f,
          topLeft = Offset((size.width - size.width / 1.15f) / 2, (size.height - size.height / 1.15f) / 2)
        )
      }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "$currencySymbol${String.format("%,.0f", totalMonthly)}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Total/Mo",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

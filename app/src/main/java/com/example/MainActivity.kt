package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlin.math.max
import kotlin.math.roundToInt
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

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
          Translations.get(TranslationKey.DEBT_PLANNER_TAB, lang)
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
          2 -> DebtPlannerTab(viewModel, lang)
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
        val categories = listOf("Mortgage", "Personal")
        categories.forEach { category ->
          val isSelected = loanTypeVal == category
          val label = when (category) {
            "Mortgage" -> Translations.get(TranslationKey.LOAN_TYPE_MORTGAGE, lang)
            else -> Translations.get(TranslationKey.LOAN_TYPE_GENERAL, lang)
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
fun DebtPlannerTab(
  viewModel: LoanCalculatorViewModel,
  lang: LanguageCode
) {
  val context = LocalContext.current
  val cur = lang.currencySymbol
  val debts by viewModel.debtsList.collectAsState()
  val budgetStr by viewModel.debtPlannerBudget.collectAsState()
  val strategy by viewModel.debtPayoffStrategy.collectAsState()
  val result by viewModel.debtPlannerResult.collectAsState()

  var newDebtName by remember { mutableStateOf("") }
  var newDebtBalance by remember { mutableStateOf("") }
  var newDebtIntRate by remember { mutableStateOf("") }
  var newDebtMinPay by remember { mutableStateOf("") }

  val sumMinPayments = debts.sumOf { it.minimumPayment }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    // Header Intro Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "info planner",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = Translations.get(TranslationKey.DEBT_PLANNER_HEADER, lang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Input settings card: Budget and strategy Choice
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Payoff Settings",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = budgetStr,
          onValueChange = { viewModel.updatePlannerBudget(it) },
          label = { Text(Translations.get(TranslationKey.DEBT_PLANNER_BUDGET, lang) + " (${cur})") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_budget_input"),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
          )
        )

        // Show warning if budget is less than required mins
        if (debts.isNotEmpty() && (budgetStr.toDoubleOrNull() ?: 0.0) < sumMinPayments) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "${Translations.get(TranslationKey.DEBT_PLANNER_MIN_BUDGET_WARN, lang)} $cur${String.format("%,.2f", sumMinPayments)}",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = Translations.get(TranslationKey.DEBT_PLANNER_STRATEGY, lang),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Strategy Selector Toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("Snowball", "Avalanche").forEach { option ->
            val isSelected = strategy == option
            val label = if (option == "Snowball") {
              Translations.get(TranslationKey.DEBT_PLANNER_SNOWBALL, lang)
            } else {
              Translations.get(TranslationKey.DEBT_PLANNER_AVALANCHE, lang)
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .background(
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                  shape = RoundedCornerShape(20.dp)
                )
                .clickable { viewModel.updateStrategy(option) }
                .padding(vertical = 10.dp, horizontal = 8.dp),
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
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Form: Create a new debt card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = Translations.get(TranslationKey.DEBT_PLANNER_ADD_DEBT, lang),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = newDebtName,
          onValueChange = { newDebtName = it },
          label = { Text(Translations.get(TranslationKey.DEBT_PLANNER_DEBT_NAME, lang)) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_name_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = newDebtBalance,
            onValueChange = { newDebtBalance = it },
            label = { Text(Translations.get(TranslationKey.DEBT_PLANNER_BALANCE, lang) + " (${cur})") },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("debt_balance_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
            )
          )

          OutlinedTextField(
            value = newDebtIntRate,
            onValueChange = { newDebtIntRate = it },
            label = { Text(Translations.get(TranslationKey.DEBT_PLANNER_INT_RATE, lang)) },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("debt_rate_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
            )
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = newDebtMinPay,
          onValueChange = { newDebtMinPay = it },
          label = { Text(Translations.get(TranslationKey.DEBT_PLANNER_MIN_PAY, lang) + " (${cur})") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_min_payment_input"),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = {
            val name = newDebtName.trim()
            val balance = newDebtBalance.toDoubleOrNull() ?: 0.0
            val rate = newDebtIntRate.toDoubleOrNull() ?: 0.0
            val minPay = newDebtMinPay.toDoubleOrNull() ?: 0.0
            if (name.isNotEmpty() && balance > 0.0 && rate >= 0.0 && minPay >= 0.0) {
              viewModel.addDebt(name, balance, rate, minPay)
              newDebtName = ""
              newDebtBalance = ""
              newDebtIntRate = ""
              newDebtMinPay = ""
            } else {
              Toast.makeText(context, "Please enter correct debt fields", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("add_debt_button"),
          shape = RoundedCornerShape(20.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Text(text = "Add Debt", fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // List of existing registered debts
    if (debts.isEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
              text = Translations.get(TranslationKey.DEBT_PLANNER_NO_DEBTS, lang),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
        }
      }
    } else {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "${Translations.get(TranslationKey.DEBT_PLANNER_TOTAL_DEBTS, lang)} (${debts.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(12.dp))

          debts.forEach { debt ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = debt.name,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Rate: ${debt.interestRate}% | Min Pay: $cur${debt.minimumPayment}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Text(
                text = "$cur${String.format("%,.2f", debt.balance)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp)
              )
              Button(
                onClick = { viewModel.deleteDebt(debt.id) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("delete_debt_${debt.name}")
              ) {
                Text(
                    text = Translations.get(TranslationKey.DEBT_PLANNER_DELETE, lang),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelSmall
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.background)
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Results overview panel
      if (result.isValid) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Payoff Results Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )

              // Share / Export PDF Plan button
              Button(
                onClick = {
                  val budget = budgetStr.toDoubleOrNull() ?: 0.0
                  val success = PdfReportExporter.generateAndShareDebtPlanPdf(
                    context = context,
                    debts = debts,
                    budget = budget,
                    strategy = strategy,
                    result = result,
                    lang = lang
                  )
                  if (success) {
                    Toast.makeText(context, Translations.get(TranslationKey.PDF_SUCCESS, lang), Toast.LENGTH_LONG).show()
                  } else {
                    Toast.makeText(context, Translations.get(TranslationKey.PDF_ERROR, lang), Toast.LENGTH_SHORT).show()
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("export_debt_pdf_button")
              ) {
                Icon(Icons.Default.Share, contentDescription = "share", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Translations.get(TranslationKey.EXPORT_PDF, lang), style = MaterialTheme.typography.labelSmall)
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ResultsRowLabel(
              label = Translations.get(TranslationKey.DEBT_PLANNER_DEBT_FREE, lang),
              value = "${result.debtFreeMonths} months",
              isBold = true
            )
            ResultsRowLabel(
              label = "Total Interest Accrued",
              value = "$cur${String.format("%,.2f", result.totalInterestPaid)}"
            )

            if (result.timeSavedMonths > 0) {
              Spacer(modifier = Modifier.height(8.dp))
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
                  contentDescription = "savings highlight",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Saved ${result.timeSavedMonths} Months of Debt & $cur${String.format("%,.2f", result.interestSaved)} in Interests!",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Debt portfolio breakdown visualization
        DebtPortfolioBreakdown(debts = debts, currency = cur)

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive debt payoff progress chart
        DebtPayoffChart(result = result, currency = cur, lang = lang)

        Spacer(modifier = Modifier.height(16.dp))

        // Projected Months timeline
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = Translations.get(TranslationKey.DEBT_PLANNER_TIMELINE, lang),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Keep list length manageable by sampling every 3 months if it's longer than 24 months, showing final month
            val sampledProjection = if (result.monthlyProjection.size > 24) {
              val sampled = result.monthlyProjection.filterIndexed { index, _ -> index % 3 == 0 }.toMutableList()
              if (result.monthlyProjection.lastOrNull() != null && sampled.lastOrNull()?.monthNumber != result.monthlyProjection.last().monthNumber) {
                sampled.add(result.monthlyProjection.last())
              }
              sampled
            } else {
              result.monthlyProjection
            }

            sampledProjection.forEach { step ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Month ${step.monthNumber}",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = "Bal: $cur${String.format("%,.0f", step.totalRemainingBalance)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Text(
                    text = "Int charged: $cur${String.format("%,.1f", step.totalInterestPaidThisMonth)}",
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

    Spacer(modifier = Modifier.height(30.dp))
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

@Composable
fun DebtPortfolioBreakdown(
  debts: List<Debt>,
  currency: String,
  modifier: Modifier = Modifier
) {
  if (debts.isEmpty()) return
  val total = debts.sumOf { it.balance }
  if (total <= 0.0) return

  val colors = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.secondary,
    MaterialTheme.colorScheme.error,
    Color(0xFF8B5CF6), // Custom Purple
    Color(0xFFF59E0B), // Custom Amber
    Color(0xFFEC4899), // Custom Pink
    Color(0xFF06B6D4)  // Custom Cyan
  )

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "Starting Debt Portfolio Mix",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Distribution of your registered starting debts of $currency${String.format("%,.0f", total)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Segmented horizontal progress bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(18.dp)
          .background(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(9.dp)
          ),
        horizontalArrangement = Arrangement.Start
      ) {
        debts.forEachIndexed { index, debt ->
          val ratio = (debt.balance / total).toFloat()
          if (ratio > 0.005f) {
            val color = colors[index % colors.size]
            Box(
              modifier = Modifier
                .weight(ratio)
                .fillMaxHeight()
                .background(
                  color = color,
                  shape = RoundedCornerShape(
                    topStart = if (index == 0) 9.dp else 0.dp,
                    bottomStart = if (index == 0) 9.dp else 0.dp,
                    topEnd = if (index == debts.size - 1) 9.dp else 0.dp,
                    bottomEnd = if (index == debts.size - 1) 9.dp else 0.dp
                  )
                )
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Legend of debt distribution ratios
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        debts.forEachIndexed { index, debt ->
          val color = colors[index % colors.size]
          val ratioPct = (debt.balance / total) * 100.0
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .background(color = color, shape = CircleShape)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = debt.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
              )
            }
            Text(
              text = "$currency${String.format("%,.0f", debt.balance)} (${String.format("%.1f", ratioPct)}%)",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@Composable
fun DebtPayoffChart(
  result: DebtPlannerResult,
  currency: String,
  lang: LanguageCode,
  modifier: Modifier = Modifier
) {
  val projection = result.monthlyProjection
  if (projection.isEmpty()) return

  var selectedIndex by remember(projection) { mutableStateOf<Int?>(null) }
  val textMeasurer = rememberTextMeasurer()
  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val outlineColor = MaterialTheme.colorScheme.outlineVariant
  val bodyTextColor = MaterialTheme.colorScheme.onSurface

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "Debt Payoff Progress",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      Text(
        text = "Interactive visualization of your payoff timeline and balance reduction. Tap the chart to inspect any month.",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Canvas for drawing the curve
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp)
      ) {
        val width = maxWidth
        val height = maxHeight

        Canvas(
          modifier = Modifier
            .fillMaxSize()
            .pointerInput(projection) {
              detectTapGestures { offset ->
                val plotWidth = size.width - 100f
                val stepX = plotWidth / (projection.size - 1).coerceAtLeast(1)
                val tappedX = offset.x - 70f
                val index = (tappedX / stepX).roundToInt().coerceIn(0, projection.size - 1)
                selectedIndex = index
              }
            }
        ) {
          val paddingLeft = 70f
          val paddingRight = 30f
          val paddingTop = 20f
          val paddingBottom = 30f

          val plotWidth = size.width - paddingLeft - paddingRight
          val plotHeight = size.height - paddingTop - paddingBottom

          if (projection.size < 2) return@Canvas

          val maxVal = projection.maxOfOrNull { it.totalRemainingBalance } ?: 1.0
          val minVal = 0.0
          val valRange = maxVal - minVal

          // 1. Draw grids and Y-axis scale
          val gridLines = 4
          for (i in 0..gridLines) {
            val ratio = i.toFloat() / gridLines
            val y = paddingTop + plotHeight * (1f - ratio)
            drawLine(
              color = outlineColor.copy(alpha = 0.4f),
              start = Offset(paddingLeft, y),
              end = Offset(paddingLeft + plotWidth, y),
              strokeWidth = 1f,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Y tick text
            val tickVal = minVal + ratio * valRange
            val tickLabel = if (tickVal >= 1000) {
              "$currency${String.format("%.0fk", tickVal / 1000)}"
            } else {
              "$currency${tickVal.toInt()}"
            }
            drawText(
              textMeasurer = textMeasurer,
              text = tickLabel,
              topLeft = Offset(5f, y - 14f),
              style = TextStyle(
                color = bodyTextColor.copy(alpha = 0.6f),
                fontSize = 9.sp
              )
            )
          }

          // Generate coordinates
          val points = projection.mapIndexed { index, month ->
            val ratioX = index.toFloat() / (projection.size - 1)
            val ratioY = (month.totalRemainingBalance - minVal) / valRange
            Offset(
              x = paddingLeft + ratioX * plotWidth,
              y = paddingTop + plotHeight * (1f - ratioY).toFloat()
            )
          }

          // 2. Build curve path
          val path = Path().apply {
            if (points.isNotEmpty()) {
              moveTo(points.first().x, points.first().y)
              for (i in 1 until points.size) {
                val pPrev = points[i - 1]
                val pCurr = points[i]
                val controlX1 = pPrev.x + (pCurr.x - pPrev.x) / 2
                val controlY1 = pPrev.y
                val controlX2 = pPrev.x + (pCurr.x - pPrev.x) / 2
                val controlY2 = pCurr.y
                cubicTo(controlX1, controlY1, controlX2, controlY2, pCurr.x, pCurr.y)
              }
            }
          }

          // Build fill path (connect bottom right and bottom left)
          val fillPath = Path().apply {
            addPath(path)
            if (points.isNotEmpty()) {
              lineTo(points.last().x, paddingTop + plotHeight)
              lineTo(points.first().x, paddingTop + plotHeight)
              close()
            }
          }

          // 3. Draw gradient filled area under the line
          drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
              colors = listOf(
                primaryColor.copy(alpha = 0.35f),
                primaryColor.copy(alpha = 0.01f)
              ),
              startY = paddingTop,
              endY = paddingTop + plotHeight
            )
          )

          // 4. Draw curve line on top
          drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(
              width = 3.dp.toPx(),
              cap = StrokeCap.Round
            )
          )

          // 5. Draw interactive indicator if selected
          selectedIndex?.let { index ->
            if (index in points.indices) {
              val selectedPoint = points[index]
              // Draw vertical line
              drawLine(
                color = secondaryColor,
                start = Offset(selectedPoint.x, paddingTop),
                end = Offset(selectedPoint.x, paddingTop + plotHeight),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
              )

              // Outer selection circle
              drawCircle(
                color = secondaryColor.copy(alpha = 0.35f),
                radius = 8.dp.toPx(),
                center = selectedPoint
              )

              // Inner selection dot
              drawCircle(
                color = secondaryColor,
                radius = 4.dp.toPx(),
                center = selectedPoint
              )
            }
          }

          // 6. Draw X-axis timeline markers
          val xTicks = if (projection.size > 12) 5 else projection.size
          for (i in 0 until xTicks) {
            val ratio = i.toFloat() / (xTicks - 1)
            val index = (ratio * (projection.size - 1)).roundToInt().coerceIn(0, projection.size - 1)
            val point = points[index]

            // Draw small notch
            drawLine(
              color = outlineColor,
              start = Offset(point.x, paddingTop + plotHeight),
              end = Offset(point.x, paddingTop + plotHeight + 4f),
              strokeWidth = 2f
            )

            // X tick text label
            val label = "M${projection[index].monthNumber}"
            drawText(
              textMeasurer = textMeasurer,
              text = label,
              topLeft = Offset(point.x - 14f, paddingTop + plotHeight + 6f),
              style = TextStyle(
                color = bodyTextColor.copy(alpha = 0.6f),
                fontSize = 9.sp
              )
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Selected Month Detail Display
      val index = selectedIndex
      if (index != null && index in projection.indices) {
        val selectedMonth = projection[index]
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
              shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(
              text = "Month ${selectedMonth.monthNumber} Inspector",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Total outstanding balance of all pool accounts.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "$currency${String.format("%,.2f", selectedMonth.totalRemainingBalance)}",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Int charged: $currency${String.format("%,.2f", selectedMonth.totalInterestPaidThisMonth)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "💡 Tap or hold anywhere along the curve to inspect specific monthly values.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}


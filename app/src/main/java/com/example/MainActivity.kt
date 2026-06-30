package com.example

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlin.math.max
import kotlin.math.min
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.ScrollableTabRow
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
  private lateinit var viewModelInstance: LoanCalculatorViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    try {
      MobileAds.initialize(this) {}
    } catch (e: Exception) {
      Log.e("MainActivity", "AdMob initialization failed: ${e.message}")
    }

    setContent {
      viewModelInstance = viewModel()
      val colorTheme by viewModelInstance.colorTheme.collectAsState()
      MyApplicationTheme(colorTheme = colorTheme) {
        MainScreen(viewModelInstance)
      }
    }
  }

  override fun onStop() {
    super.onStop()
    if (::viewModelInstance.isInitialized) {
      viewModelInstance.lockComparisonFeature()
      viewModelInstance.lockDebtPlannerFeature()
      viewModelInstance.lockRentVsBuyFeature()
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
  val isAdFree by viewModel.isAdFreeVersion.collectAsState()
  
  var activeTab by remember { mutableStateOf(0) }
  var langMenuExpanded by remember { mutableStateOf(false) }
  var showPremiumUpgradeDialog by remember { mutableStateOf(false) }
  var showReportBugDialog by remember { mutableStateOf(false) }
  var showSettingsScreen by remember { mutableStateOf(false) }

  if (showSettingsScreen) {
    SettingsScreen(
      viewModel = viewModel,
      lang = lang,
      onDismiss = { showSettingsScreen = false },
      onShowBugReport = { showReportBugDialog = true }
    )
  }

  if (showPremiumUpgradeDialog) {
    SimulatedPremiumUpgradeDialog(
      viewModel = viewModel,
      lang = lang,
      onDismiss = { showPremiumUpgradeDialog = false }
    )
  }

  if (showReportBugDialog) {
    ReportBugDialog(lang = lang, onDismiss = { showReportBugDialog = false })
  }

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
          // Minimal premium indicator button
          IconButton(
            onClick = { showPremiumUpgradeDialog = true },
            modifier = Modifier
              .padding(end = 4.dp)
              .background(
                color = if (isAdFree) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color(0xFFFFD700).copy(alpha = 0.15f),
                shape = CircleShape
              )
              .testTag("premium_upgrade_top_btn")
          ) {
            Text(
              text = if (isAdFree) "💎" else "👑",
              fontSize = 18.sp,
              modifier = Modifier.testTag("premium_text_descriptor")
            )
          }

          // Settings (Gear) button
          IconButton(
            onClick = { showSettingsScreen = true },
            modifier = Modifier
              .padding(end = 8.dp)
              .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape
              )
              .testTag("settings_top_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Settings",
              tint = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.size(20.dp)
            )
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
      
      // Tabs Navigation (Responsive adaptive layout)
      val tabTitles = listOf(
        Translations.get(TranslationKey.CALCULATOR_TAB, lang),
        Translations.get(TranslationKey.SCHEDULE_TAB, lang),
        Translations.get(TranslationKey.COMPARISON_TAB, lang),
        Translations.get(TranslationKey.DEBT_PLANNER_TAB, lang),
        Translations.get(TranslationKey.RENT_VS_BUY_TAB, lang)
      )

      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isCompactScreen = maxWidth < 480.dp
        if (isCompactScreen) {
          ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
              if (activeTab < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                  modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                  color = MaterialTheme.colorScheme.primary
                )
              }
            },
            divider = {
              HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            }
          ) {
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    fontSize = 12.sp
                  )
                }
              )
            }
          }
        } else {
          TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
              if (activeTab < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                  modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                  color = MaterialTheme.colorScheme.primary
                )
              }
            },
            divider = {
              HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            }
          ) {
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    fontSize = 14.sp
                  )
                }
              )
            }
          }
        }
      }

      // Responsive contents container
      Box(modifier = Modifier.weight(1f)) {
        when (activeTab) {
          0 -> CalculatorTab(viewModel, result, lang, loanTypeVal)
          1 -> AmortizationTab(viewModel, result, lang, loanTypeVal)
          2 -> {
            val isCompUnlocked by viewModel.isComparisonUnlocked.collectAsState()
            val isAdFreeFlow by viewModel.isAdFreeVersion.collectAsState()
            if (isCompUnlocked || isAdFreeFlow) {
              ComparisonTab(viewModel, result, lang)
            } else {
              AdInteractiveScreen(
                featureName = Translations.get(TranslationKey.COMPARISON_TAB, lang),
                lang = lang,
                onUnlocked = { viewModel.unlockComparisonFeature() }
              )
            }
          }
          3 -> {
            val isDebtPlannerUnlocked by viewModel.isDebtPlannerUnlocked.collectAsState()
            val isAdFreeFlow by viewModel.isAdFreeVersion.collectAsState()
            if (isDebtPlannerUnlocked || isAdFreeFlow) {
              DebtPlannerTab(viewModel, lang)
            } else {
              AdInteractiveScreen(
                featureName = Translations.get(TranslationKey.DEBT_PLANNER_TAB, lang),
                lang = lang,
                onUnlocked = { viewModel.unlockDebtPlannerFeature() }
              )
            }
          }
          4 -> {
            val isRentVsBuyUnlocked by viewModel.isRentVsBuyUnlocked.collectAsState()
            val isAdFreeFlow by viewModel.isAdFreeVersion.collectAsState()
            if (isRentVsBuyUnlocked || isAdFreeFlow) {
              RentVsBuyTab(viewModel, lang)
            } else {
              AdInteractiveScreen(
                featureName = Translations.get(TranslationKey.RENT_VS_BUY_TAB, lang),
                lang = lang,
                onUnlocked = { viewModel.unlockRentVsBuyFeature() }
              )
            }
          }
        }
      }

      // Persistent Sticky Banner Ad for monetization
      BannerAdComponent(
        viewModel = viewModel,
        onRemoveAdsClick = { showPremiumUpgradeDialog = true }
      )
    }
  }
}

@Composable
fun FinancialDisclaimerText(lang: LanguageCode) {
  Text(
    text = Translations.get(TranslationKey.FINANCIAL_DISCLAIMER, lang),
    style = MaterialTheme.typography.bodySmall.copy(
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
      fontSize = 11.sp,
      lineHeight = 15.sp,
      textAlign = TextAlign.Center
    ),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 4.dp, vertical = 12.dp)
  )
}

@Composable
fun CalculatorTab(
  viewModel: LoanCalculatorViewModel,
  result: CalculationResult,
  lang: LanguageCode,
  loanTypeVal: String
) {
  val customCurrency by viewModel.customCurrencySymbol.collectAsState()
  val curSymbol = customCurrency ?: lang.currencySymbol
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
          Spacer(modifier = Modifier.height(8.dp))
          FinancialDisclaimerText(lang)
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
        FinancialDisclaimerText(lang)
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
  var activeHelpType by remember { mutableStateOf<HelpType?>(null) }

  val customCurrency by viewModel.customCurrencySymbol.collectAsState()
  val cur = customCurrency ?: lang.currencySymbol

  OutlinedCard(
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
            val coerced = coerceInputString(it, 100000000.0, true)
            homePriceInput = coerced
            viewModel.homePrice.value = coerced
            viewModel.updateInputs()
          },
          label = { Text("${Translations.get(TranslationKey.HOME_PRICE, lang)} (${cur})") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("home_price_input"),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // Down Payment
        OutlinedTextField(
          value = downPaymentInput,
          onValueChange = {
            val maxDp = homePriceInput.parseToDoubleOrNull() ?: 100000000.0
            val coerced = coerceInputString(it, maxDp, true)
            downPaymentInput = coerced
            viewModel.downPayment.value = coerced
            viewModel.updateInputs()
          },
          label = { Text("${Translations.get(TranslationKey.DOWN_PAYMENT, lang)} (${cur})") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("down_payment_input"),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          trailingIcon = {
            IconButton(
              onClick = { activeHelpType = HelpType.LTV },
              modifier = Modifier.testTag("help_ltv_button")
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "What is LTV?",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
              )
            }
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )
      } else {
        OutlinedTextField(
          value = loanAmtInput,
          onValueChange = {
            val coerced = coerceInputString(it, 100000000.0, true)
            loanAmtInput = coerced
            viewModel.loanAmountInput.value = coerced
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
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
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
            val coerced = coerceInputString(it, 35.0, false)
            intRateInput = coerced
            viewModel.interestRate.value = coerced
            viewModel.updateInputs()
          },
          label = { Text("${Translations.get(TranslationKey.ANNUAL_INTEREST, lang)} (%)") },
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("interest_rate_input"),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          trailingIcon = {
            IconButton(
              onClick = { activeHelpType = HelpType.APR },
              modifier = Modifier.testTag("help_apr_button")
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "What is APR?",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
              )
            }
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )
        
        Spacer(modifier = Modifier.width(16.dp))

        // Simple interactive speed adjustments
        IconButton(
          onClick = {
            val curVal = intRateInput.parseToDoubleOrNull() ?: 0.0
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
            val curVal = intRateInput.parseToDoubleOrNull() ?: 0.0
            val newVal = min(35.0, curVal + 0.25).format(2)
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
          val coerced = coerceInputString(it, 50.0, true)
          loanTermInput = coerced
          viewModel.loanTermYears.value = coerced
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
          unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Extra Payments input with quick boost + - buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = extraPaymentInput,
          onValueChange = {
            val coerced = coerceInputString(it, 1000000.0, true)
            extraPaymentInput = coerced
            viewModel.extraPayment.value = coerced
            viewModel.updateInputs()
          },
          label = { Text("${Translations.get(TranslationKey.EXTRA_PAYMENT, lang)} (${cur})") },
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("extra_payment_input"),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )
        
        Spacer(modifier = Modifier.width(16.dp))

        // Quick set minus button for extras
        IconButton(
          onClick = {
            val curVal = extraPaymentInput.parseToDoubleOrNull() ?: 0.0
            val newVal = max(0.0, curVal - 50.0).toLong().toString()
            extraPaymentInput = newVal
            viewModel.extraPayment.value = newVal
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

        // Quick set plus button for extras
        IconButton(
          onClick = {
            val curVal = extraPaymentInput.parseToDoubleOrNull() ?: 0.0
            val newVal = min(1000000.0, curVal + 50.0).toLong().toString()
            extraPaymentInput = newVal
            viewModel.extraPayment.value = newVal
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

      // Mortgage Advanced Taxes/Insurance Collapse Section
      if (loanTypeVal == "Mortgage") {
        Spacer(modifier = Modifier.height(16.dp))
        
        val hideText = when (lang) {
          LanguageCode.ES -> "▲ Ocultar"
          LanguageCode.FR -> "▲ Masquer"
          LanguageCode.DE -> "▲ Ausblenden"
          LanguageCode.HI -> "▲ छिपाएं"
          LanguageCode.TA -> "▲ மறை"
          else -> "▲ Hide"
        }
        val expandText = when (lang) {
          LanguageCode.ES -> "▼ Expandir"
          LanguageCode.FR -> "▼ Afficher"
          LanguageCode.DE -> "▼ Einblenden"
          LanguageCode.HI -> "▼ विस्तार करें"
          LanguageCode.TA -> "▼ விரிவாக்கு"
          else -> "▼ Expand"
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showAdvancedCosts = !showAdvancedCosts }
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = Translations.get(TranslationKey.TAX_INS_DEDUCT_TITLE, lang),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = if (showAdvancedCosts) hideText else expandText,
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
                value = insInput,
                onValueChange = {
                  val coerced = coerceInputString(it, 200000.0, true)
                  insInput = coerced
                  viewModel.homeInsurance.value = coerced
                  viewModel.updateInputs()
                },
                label = { Text(Translations.get(TranslationKey.INSURANCE_ANNUAL, lang)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
              )

              OutlinedTextField(
                value = pmiInput,
                onValueChange = {
                  val coerced = coerceInputString(it, 5.0, false)
                  pmiInput = coerced
                  viewModel.pmiRate.value = coerced
                  viewModel.updateInputs()
                },
                label = { Text(Translations.get(TranslationKey.PMI_PERCENT, lang)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                  IconButton(
                    onClick = { activeHelpType = HelpType.PMI },
                    modifier = Modifier.testTag("help_pmi_button")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Info,
                      contentDescription = "What is PMI?",
                      tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                      modifier = Modifier.size(18.dp)
                    )
                  }
                },
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
              )
            }
          }
        }
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)

      // Variable Interest Rate scenarios
      val isVarEnabled by viewModel.isVariableRateEnabled.collectAsState()
      val varPeriodVal by viewModel.variablePeriodYears.collectAsState()
      val varAdjustVal by viewModel.subsequentAdjustRate.collectAsState()

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = Translations.get(TranslationKey.ARM_TITLE, lang),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
              onClick = { activeHelpType = HelpType.ARM },
              modifier = Modifier.size(24.dp).testTag("help_arm_button")
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "What is ARM?",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
              )
            }
          }
          Text(
            text = Translations.get(TranslationKey.ARM_SUBTITLE, lang),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = isVarEnabled,
          onCheckedChange = { viewModel.updateVariableRate(it, varPeriodVal, varAdjustVal) },
          modifier = Modifier.testTag("variable_rate_switch")
        )
      }

      if (isVarEnabled) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          var varPeriodInput by remember { mutableStateOf(varPeriodVal) }
          var varAdjustInput by remember { mutableStateOf(varAdjustVal) }

          OutlinedTextField(
            value = varPeriodInput,
            onValueChange = {
              val coerced = coerceInputString(it, 50.0, true)
              varPeriodInput = coerced
              viewModel.updateVariableRate(isVarEnabled, coerced, varAdjustInput)
            },
            label = { Text(Translations.get(TranslationKey.ARM_FIXED_PERIOD, lang)) },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("arm_fixed_period_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
          )

          OutlinedTextField(
            value = varAdjustInput,
            onValueChange = {
              val coerced = coerceInputString(it, 20.0, false)
              varAdjustInput = coerced
              viewModel.updateVariableRate(isVarEnabled, varPeriodInput, coerced)
            },
            label = { Text(Translations.get(TranslationKey.ARM_RESET_ADJ, lang)) },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("arm_adjustment_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
          )
        }
        Text(
          text = Translations.get(TranslationKey.ARM_INFO, lang).format(varPeriodVal, varAdjustVal),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(top = 8.dp)
        )
      }
      
      if (activeHelpType != null) {
        HelpTooltipDialog(
          helpType = activeHelpType!!,
          lang = lang,
          onDismiss = { activeHelpType = null }
        )
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
    OutlinedCard(
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

  OutlinedCard(
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
          lang = lang,
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
            text = Translations.get(TranslationKey.PAYING_OFF_EARLY, lang).replace("%s", result.savingYearsEarly.format(1)),
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

      // Total Cost Split up Details Card
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val labels = when(lang) {
            LanguageCode.ES -> listOf("Monto Principal del Préstamo", "Total de Intereses Pagados", "Pagos Adicionales Totales", "Seguros y PMI")
            LanguageCode.FR -> listOf("Montant Principal du Prêt", "Total des Intérêts Payés", "Paiements Supplémentaires Totaux", "Assurances & PMI")
            LanguageCode.DE -> listOf("Darlehenshauptbetrag", "Gezahlte Gesamtzinsen", "Zusätzliche Gesamtzahlungen", "Versicherungen & PMI")
            LanguageCode.HI -> listOf("ऋण मूलधन राशि", "कुल भुगतान किया गया ब्याज", "कुल अतिरिक्त भुगतान", "बीमा और पीएमआई")
            LanguageCode.TA -> listOf("அசல் கடன் தொகை", "வட்டி செலுத்திய தொகை", "கூடுதல் செலுத்திய தொகை", "காப்பீடு & பிஎம்ஐ")
            else -> listOf("Principal Loan Amount", "Total Interest Paid", "Total Extra Paid", "Insurance & PMI")
          }

          val pAmt = result.principalLoanAmount
          val tInt = result.totalInterestPaid
          val tExt = result.totalExtraPaid
          val fees = max(0.0, result.totalPaidAmount - pAmt - tInt - tExt)

          ResultsRowLabel(
            label = labels[0],
            value = "$currency ${String.format("%,.2f", pAmt)}"
          )
          ResultsRowLabel(
            label = labels[1],
            value = "$currency ${String.format("%,.2f", tInt)}"
          )
          if (tExt > 0.0) {
            ResultsRowLabel(
              label = labels[2],
              value = "$currency ${String.format("%,.2f", tExt)}"
            )
          }
          if (fees > 0.1 || loanTypeVal == "Mortgage") {
            ResultsRowLabel(
              label = labels[3],
              value = "$currency ${String.format("%,.2f", fees)}"
            )
          }
        }
      }

      // Variable ARM Forecast
      val isVarEnabled by viewModel.isVariableRateEnabled.collectAsState()
      val varPeriodVal by viewModel.variablePeriodYears.collectAsState()
      val varAdjustVal by viewModel.subsequentAdjustRate.collectAsState()

      if (isVarEnabled) {
          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.background)
          Spacer(modifier = Modifier.height(12.dp))
          
          Text(
              text = Translations.get(TranslationKey.ARM_FORECAST_TITLE, lang),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
          )
          
          Spacer(modifier = Modifier.height(8.dp))
          
          OutlinedCard(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
          ) {
              Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                      val yearLabel = when(lang) {
                        LanguageCode.ES -> "Año 1 a $varPeriodVal"
                        LanguageCode.FR -> "Année 1 à $varPeriodVal"
                        LanguageCode.DE -> "Jahr 1 bis $varPeriodVal"
                        LanguageCode.HI -> "वर्ष 1 से $varPeriodVal"
                        LanguageCode.TA -> "ஆண்டு 1 முதல் $varPeriodVal"
                        else -> "Year 1 to $varPeriodVal"
                      }
                      val standardRateLabel = when(lang) {
                        LanguageCode.ES -> "${viewModel.interestRate.value}% tasa estándar"
                        LanguageCode.FR -> "${viewModel.interestRate.value}% taux standard"
                        LanguageCode.DE -> "${viewModel.interestRate.value}% Standardzinssatz"
                        LanguageCode.HI -> "${viewModel.interestRate.value}% मानक दर"
                        LanguageCode.TA -> "${viewModel.interestRate.value}% நிலையான வட்டி விகிதம்"
                        else -> "${viewModel.interestRate.value}% standard rate"
                      }
                      Text(yearLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                      Text(standardRateLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                      val directAdj = varAdjustVal.parseToDoubleOrNull() ?: 0.0
                      val initialRate = viewModel.interestRate.value.parseToDoubleOrNull() ?: 0.0
                      val resetRate = initialRate + directAdj
                      val resetLabel = when(lang) {
                        LanguageCode.ES -> "Año ${(varPeriodVal.toIntOrNull() ?: 5) + 1}+ Restablecer"
                        LanguageCode.FR -> "Année ${(varPeriodVal.toIntOrNull() ?: 5) + 1}+ Réajustement"
                        LanguageCode.DE -> "Jahr ${(varPeriodVal.toIntOrNull() ?: 5) + 1}+ Anpassung"
                        LanguageCode.HI -> "वर्ष ${(varPeriodVal.toIntOrNull() ?: 5) + 1}+ रीसेट"
                        LanguageCode.TA -> "ஆண்டு ${(varPeriodVal.toIntOrNull() ?: 5) + 1}+ வட்டி விகித மாற்றம்"
                        else -> "Year ${(varPeriodVal.toIntOrNull() ?: 5) + 1}+ Reset"
                      }
                      val forecastLabel = when(lang) {
                        LanguageCode.ES -> "${String.format("%.2f", resetRate)}% de previsión"
                        LanguageCode.FR -> "${String.format("%.2f", resetRate)}% prévisions"
                        LanguageCode.DE -> "${String.format("%.2f", resetRate)}% Prognose"
                        LanguageCode.HI -> "${String.format("%.2f", resetRate)}% पूर्वानुमान"
                        LanguageCode.TA -> "${String.format("%.2f", resetRate)}% முன்னறிவிப்பு"
                        else -> "${String.format("%.2f", resetRate)}% forecast"
                      }
                      Text(resetLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                      Text(forecastLabel, style = MaterialTheme.typography.bodySmall, color = if (directAdj >= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                  }
              }
          }
      }

      // Interest Rate Sensitivity Analyzer List
      if (result.sensitivityAnalysis.isNotEmpty()) {
          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.background)
          Spacer(modifier = Modifier.height(12.dp))
          
          Text(
              text = Translations.get(TranslationKey.SENSITIVITY_MATRIX_TITLE, lang),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
          )
          
          Spacer(modifier = Modifier.height(4.dp))
          Text(
              text = Translations.get(TranslationKey.SENSITIVITY_MATRIX_SUBTITLE, lang),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          
          Spacer(modifier = Modifier.height(8.dp))
          
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              // Header Row
              Row(
                  modifier = Modifier
                      .fillMaxWidth()
                      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                      .padding(vertical = 6.dp, horizontal = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween
              ) {
                  Text(Translations.get(TranslationKey.ANNUAL_INTEREST, lang), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                  Text(Translations.get(TranslationKey.PRINCIPAL_AND_INTEREST, lang), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                  Text(Translations.get(TranslationKey.TOTAL_INTEREST, lang), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
              }
              
              result.sensitivityAnalysis.forEach { sItem ->
                  val isBase = Math.abs(sItem.rate - (viewModel.interestRate.value.parseToDoubleOrNull() ?: 0.0)) < 0.01
                  Row(
                      modifier = Modifier
                          .fillMaxWidth()
                          .background(
                              if (isBase) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                              else Color.Transparent, 
                              RoundedCornerShape(4.dp)
                          )
                          .padding(vertical = 6.dp, horizontal = 8.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                  ) {
                      Text(
                          text = "${String.format("%.2f", sItem.rate)}%" + if (isBase) " ★" else "", 
                          style = MaterialTheme.typography.bodySmall, 
                          fontWeight = if (isBase) FontWeight.Bold else FontWeight.Normal,
                          color = if (isBase) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                          modifier = Modifier.weight(1f)
                      )
                      
                      Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                          Text(
                              text = "$currency ${String.format("%,.2f", sItem.monthlyPayment)}", 
                              style = MaterialTheme.typography.bodySmall,
                              fontWeight = if (isBase) FontWeight.Bold else FontWeight.Normal
                          )
                          if (!isBase) {
                              val sign = if (sItem.deltaMonthly >= 0) "+" else ""
                              Text(
                                  text = "$sign$currency ${String.format("%,.0f", sItem.deltaMonthly)}", 
                                  style = MaterialTheme.typography.labelSmall,
                                  color = if (sItem.deltaMonthly >= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                              )
                          }
                      }
                      
                      Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {
                          Text(
                              text = "$currency ${String.format("%,.0f", sItem.totalInterest)}", 
                              style = MaterialTheme.typography.bodySmall,
                              fontWeight = if (isBase) FontWeight.Bold else FontWeight.Normal
                          )
                          if (!isBase) {
                              val sign = if (sItem.deltaInterest >= 0) "+" else ""
                              Text(
                                  text = "$sign$currency ${String.format("%,.0f", sItem.deltaInterest)}", 
                                  style = MaterialTheme.typography.labelSmall,
                                  color = if (sItem.deltaInterest >= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                              )
                          }
                      }
                  }
              }
          }
      }
    }
  }
}

@Composable
fun ComparisonDataRow(
    metric: String,
    valA: String,
    valB: String,
    highlightA: Boolean = false,
    highlightB: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = metric,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = valA,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (highlightA) FontWeight.Bold else FontWeight.Normal,
            color = if (highlightA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1.1f)
                .background(
                    if (highlightA) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Text(
            text = valB,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (highlightB) FontWeight.Bold else FontWeight.Normal,
            color = if (highlightB) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1.1f)
                .background(
                    if (highlightB) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else Color.Transparent,
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
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
  val customCurrency by viewModel.customCurrencySymbol.collectAsState()
  val curSymbol = customCurrency ?: lang.currencySymbol
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
          val success = PdfReportExporter.generateAndSharePdf(
            context = context,
            result = result,
            lang = lang,
            loanType = loanTypeVal,
            byMonthly = viewByMonthly,
            currencySymbol = customCurrency
          )
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
          Text(text = Translations.get(TranslationKey.PAID_HEADER, lang), modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.End)
          Text(text = Translations.get(TranslationKey.PRINCIPAL_HEADER, lang), modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.End)
          Text(text = Translations.get(TranslationKey.INTEREST_HEADER, lang), modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.End)
          Text(text = Translations.get(TranslationKey.BALANCE_HEADER, lang), modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.End)
        }

        if (viewByMonthly) {
          LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(result.schedule) { item ->
              MonthlyAmortizationRow(item, curSymbol)
            }
          }
        } else {
          LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(result.yearlySchedule) { item ->
              YearlyAmortizationRow(item, curSymbol)
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
  val customCurrency by viewModel.customCurrencySymbol.collectAsState()
  val cur = customCurrency ?: lang.currencySymbol
  val debts by viewModel.debtsList.collectAsState()
  val budgetStr by viewModel.debtPlannerBudget.collectAsState()
  val strategy by viewModel.debtPayoffStrategy.collectAsState()
  val result by viewModel.debtPlannerResult.collectAsState()

  var newDebtName by remember { mutableStateOf("") }
  var newDebtBalance by remember { mutableStateOf("") }
  var newDebtIntRate by remember { mutableStateOf("") }
  var newDebtMinPay by remember { mutableStateOf("") }

  val sumMinPayments = debts.sumOf { it.minimumPayment }

  @Composable
  fun HeaderSection() {
    OutlinedCard(
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
  }

  @Composable
  fun PayoffSettingsSection() {
    OutlinedCard(
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
          onValueChange = {
            val coerced = coerceInputString(it, 1000000.0, true)
            viewModel.updatePlannerBudget(coerced)
          },
          label = { Text(Translations.get(TranslationKey.DEBT_PLANNER_BUDGET, lang) + " (${cur})") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_budget_input"),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )

        if (debts.isNotEmpty() && (budgetStr.parseToDoubleOrNull() ?: 0.0) < sumMinPayments) {
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
  }

  @Composable
  fun AddDebtSection() {
    OutlinedCard(
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
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = newDebtBalance,
            onValueChange = {
              val coerced = coerceInputString(it, 10000000.0, true)
              newDebtBalance = coerced
            },
            label = { Text(Translations.get(TranslationKey.DEBT_PLANNER_BALANCE, lang) + " (${cur})") },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("debt_balance_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
          )

          OutlinedTextField(
            value = newDebtIntRate,
            onValueChange = {
              val coerced = coerceInputString(it, 100.0, false)
              newDebtIntRate = coerced
            },
            label = { Text(Translations.get(TranslationKey.DEBT_PLANNER_INT_RATE, lang)) },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("debt_rate_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = newDebtMinPay,
          onValueChange = {
            val maxMin = newDebtBalance.parseToDoubleOrNull() ?: 1000000.0
            val coerced = coerceInputString(it, maxMin, true)
            newDebtMinPay = coerced
          },
          label = { Text(Translations.get(TranslationKey.DEBT_PLANNER_MIN_PAY, lang) + " (${cur})") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_min_payment_input"),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = {
            val name = newDebtName.trim()
            val balance = newDebtBalance.parseToDoubleOrNull() ?: 0.0
            val rate = newDebtIntRate.parseToDoubleOrNull() ?: 0.0
            val minPay = newDebtMinPay.parseToDoubleOrNull() ?: 0.0
            if (name.isNotEmpty() && balance > 0.0 && rate >= 0.0 && minPay >= 0.0) {
              viewModel.addDebt(name, balance, rate, minPay)
              newDebtName = ""
              newDebtBalance = ""
              newDebtIntRate = ""
              newDebtMinPay = ""
            } else {
              Toast.makeText(context, Translations.get(TranslationKey.DEBT_PLANNER_ERROR_FIELDS, lang), Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("add_debt_button"),
          shape = RoundedCornerShape(20.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Text(text = Translations.get(TranslationKey.ADD_DEBT_BTN, lang), fontWeight = FontWeight.Bold)
        }
      }
    }
  }

  @Composable
  fun ExistingDebtsSection() {
    if (debts.isEmpty()) {
      OutlinedCard(
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
      OutlinedCard(
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
    }
  }

  @Composable
  fun PayoffResultsSection() {
    OutlinedCard(
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

          Button(
            onClick = {
              val budget = budgetStr.parseToDoubleOrNull() ?: 0.0
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
  }

  @Composable
  fun TimelineSection() {
    OutlinedCard(
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
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(6.dp))

            // Split per loan
            OutlinedCard(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 2.dp, bottom = 4.dp),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
              ),
              shape = RoundedCornerShape(8.dp)
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                debts.forEach { debt ->
                  val payment = step.payments[debt.id] ?: 0.0
                  val remainingBal = step.balances[debt.id] ?: 0.0

                  if (payment > 0.0 || remainingBal > 0.0) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                      ) {
                        Box(
                          modifier = Modifier
                            .size(6.dp)
                            .background(
                              if (remainingBal == 0.0) MaterialTheme.colorScheme.error
                              else MaterialTheme.colorScheme.primary,
                              CircleShape
                            )
                        )
                        Text(
                          text = debt.name,
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        if (remainingBal == 0.0 && payment > 0.0) {
                          Box(
                            modifier = Modifier
                              .background(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                RoundedCornerShape(4.dp)
                              )
                              .padding(horizontal = 4.dp, vertical = 2.dp)
                          ) {
                            Text(
                              text = "PAID OFF",
                              style = MaterialTheme.typography.labelSmall,
                              fontSize = 8.sp,
                              fontWeight = FontWeight.Bold,
                              color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                          }
                        }
                      }

                      Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "Paid: $cur${String.format("%,.0f", payment)}",
                          style = MaterialTheme.typography.bodySmall,
                          fontWeight = FontWeight.SemiBold,
                          color = if (payment > 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                          text = "Bal: $cur${String.format("%,.0f", remainingBal)}",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                  }
                }
              }
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.background)
        }
      }
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isTablet = maxWidth > 600.dp
    
    if (isTablet) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          HeaderSection()
          PayoffSettingsSection()
          AddDebtSection()
          Spacer(modifier = Modifier.height(60.dp))
        }

        Column(
          modifier = Modifier
            .weight(1.2f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          ExistingDebtsSection()
          if (result.isValid) {
            PayoffResultsSection()
          }
          if (debts.isNotEmpty()) {
            DebtPortfolioBreakdown(debts = debts, currency = cur)
            Spacer(modifier = Modifier.height(8.dp))
            DebtPayoffChart(result = result, currency = cur, lang = lang)
            Spacer(modifier = Modifier.height(8.dp))
            TimelineSection()
          }
          Spacer(modifier = Modifier.height(60.dp))
        }
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        HeaderSection()
        PayoffSettingsSection()
        AddDebtSection()
        ExistingDebtsSection()
        if (result.isValid) {
          PayoffResultsSection()
        }
        if (debts.isNotEmpty()) {
          DebtPortfolioBreakdown(debts = debts, currency = cur)
          Spacer(modifier = Modifier.height(8.dp))
          DebtPayoffChart(result = result, currency = cur, lang = lang)
          Spacer(modifier = Modifier.height(8.dp))
          TimelineSection()
        }
        Spacer(modifier = Modifier.height(60.dp))
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
  lang: LanguageCode = LanguageCode.EN,
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

  var selectedPart by remember(principal, interest, fees) { mutableStateOf<String?>(null) }

  Box(
    modifier = modifier
      .size(160.dp)
      .pointerInput(principal, interest, fees) {
        detectTapGestures { offset ->
          val center = Offset(size.width / 2f, size.height / 2f)
          val dx = offset.x - center.x
          val dy = offset.y - center.y
          val dist = Math.sqrt((dx * dx + dy * dy).toDouble())
          
          if (dist > 15f) { // tap within the donut area
            var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
            if (angle < 0) {
              angle += 360f
            }
            // Start of arcs is at -90 degrees (top vertical center). Adjust.
            val normalizedAngle = (angle + 90f) % 360f
            selectedPart = when {
              normalizedAngle < principalSweep -> "Principal"
              normalizedAngle < principalSweep + interestSweep -> "Interest"
              else -> {
                if (fees > 0) "Fees" else "Principal"
              }
            }
          } else {
            selectedPart = null
          }
        }
      },
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
        color = if (selectedPart == null || selectedPart == "Principal") colors[0] else colors[0].copy(alpha = 0.35f),
        startAngle = startAngle,
        sweepAngle = principalSweep,
        useCenter = false,
        style = Stroke(width = strokeWidth + (if (selectedPart == "Principal") 4f else 0f), cap = StrokeCap.Round),
        size = size / 1.15f,
        topLeft = Offset((size.width - size.width/1.15f)/2, (size.height - size.height/1.15f)/2)
      )
      startAngle += principalSweep

      // Interest arc
      drawArc(
        color = if (selectedPart == null || selectedPart == "Interest") colors[1] else colors[1].copy(alpha = 0.35f),
        startAngle = startAngle,
        sweepAngle = interestSweep,
        useCenter = false,
        style = Stroke(width = strokeWidth + (if (selectedPart == "Interest") 4f else 0f), cap = StrokeCap.Round),
        size = size / 1.15f,
        topLeft = Offset((size.width - size.width/1.15f)/2, (size.height - size.height/1.15f)/2)
      )
      startAngle += interestSweep

      // Fees arc
      if (feesSweep > 0) {
        drawArc(
          color = if (selectedPart == null || selectedPart == "Fees") colors[2] else colors[2].copy(alpha = 0.35f),
          startAngle = startAngle,
          sweepAngle = feesSweep,
          useCenter = false,
          style = Stroke(width = strokeWidth + (if (selectedPart == "Fees") 4f else 0f), cap = StrokeCap.Round),
          size = size / 1.15f,
          topLeft = Offset((size.width - size.width / 1.15f) / 2, (size.height - size.height / 1.15f) / 2)
        )
      }
    }

    if (selectedPart == null) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "$currencySymbol${String.format("%,.0f", totalMonthly)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = Translations.get(TranslationKey.TOTAL_MO, lang),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    } else {
      val (label, amount, color) = when (selectedPart) {
        "Principal" -> Triple(
          Translations.get(TranslationKey.PRINCIPAL_HEADER, lang),
          principal,
          colors[0]
        )
        "Interest" -> Triple(
          Translations.get(TranslationKey.INTEREST_HEADER, lang),
          interest,
          colors[1]
        )
        else -> Triple(
          Translations.get(TranslationKey.OTHER_FEES, lang),
          fees,
          colors[2]
        )
      }
      val pct = (amount / total) * 100

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .padding(8.dp)
          .clickable { selectedPart = null }
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = color
        )
        Text(
          text = "$currencySymbol${String.format("%,.0f", amount)}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${String.format("%.1f", pct)}%",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = color
        )
      }
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

  OutlinedCard(
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

  OutlinedCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = Translations.get(TranslationKey.DEBT_PAYOFF_PROGRESS, lang),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      Text(
        text = Translations.get(TranslationKey.DEBT_CHART_INSTRUCTION, lang),
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

        // Floating interactive tooltip card overlay for Debt Payoff Curve
        selectedIndex?.let { idx ->
          if (idx in projection.indices) {
            val selectedMonth = projection[idx]
            OutlinedCard(
              modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp, start = 8.dp, end = 8.dp)
                .fillMaxWidth(0.92f),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
              ),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = Translations.get(TranslationKey.MONTH_INSPECTOR, lang).replace("%s", selectedMonth.monthNumber.toString()),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${Translations.get(TranslationKey.REMAINING_BALANCE, lang)}: $currency${String.format("%,.0f", selectedMonth.totalRemainingBalance)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                  Text(
                    text = Translations.get(TranslationKey.INTEREST_CHARGED, lang).replace("%s", "$currency${String.format("%,.0f", selectedMonth.totalInterestPaidThisMonth)}"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                  )
                }

                IconButton(
                  onClick = { selectedIndex = null },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Informational status bar across languages
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
          text = Translations.get(TranslationKey.DEBT_CHART_TAP_PROMPT, lang),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
fun SimulatedPremiumUpgradeDialog(
  viewModel: LoanCalculatorViewModel,
  lang: LanguageCode,
  onDismiss: () -> Unit
) {
  val isAdFree by viewModel.isAdFreeVersion.collectAsState()
  var isPurchaseInProgress by remember { mutableStateOf(false) }
  var isPurchaseSuccess by remember { mutableStateOf(false) }

  val customCurrency by viewModel.customCurrencySymbol.collectAsState()
  val curSymbol = customCurrency ?: lang.currencySymbol
  val formattedPrice = Translations.getLocalizedPremiumPrice(curSymbol)
  val activity = LocalContext.current as? Activity

  Dialog(
    onDismissRequest = { if (!isPurchaseInProgress) onDismiss() }
  ) {
    OutlinedCard(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(24.dp),
      border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        if (isPurchaseInProgress) {
          Text(
            Translations.get(TranslationKey.PREM_UPGRADE_PROCESSING, lang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 5.dp
          )
          Text(
            Translations.get(TranslationKey.PREM_UPGRADE_GATEWAY, lang),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
          LaunchedEffect(Unit) {
            delay(2000)
            isPurchaseInProgress = false
            isPurchaseSuccess = true
          }
        } else if (isPurchaseSuccess) {
          Box(
            modifier = Modifier
              .size(64.dp)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text("🏆", fontSize = 32.sp)
          }

          Text(
            Translations.get(TranslationKey.PREM_UPGRADE_SUCCESS_TITLE, lang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
          )

          Text(
            Translations.get(TranslationKey.PREM_UPGRADE_SUCCESS_DESC, lang),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )

          Button(
            onClick = {
              viewModel.purchaseAdFree()
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().testTag("upgrade_superb_ok"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(Translations.get(TranslationKey.PREM_SUPERB, lang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
          }
        } else if (isAdFree) {
          Box(
            modifier = Modifier
              .size(64.dp)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text("💎", fontSize = 32.sp)
          }

          Text(
            Translations.get(TranslationKey.PREM_ACTIVE_TITLE, lang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
          )

          Text(
            Translations.get(TranslationKey.PREM_ACTIVE_DESC, lang),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )

          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().testTag("premium_already_active_done"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(Translations.get(TranslationKey.PREM_GREAT, lang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
          }
        } else {
          // Purchase Screen
          Box(
            modifier = Modifier
              .size(64.dp)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text("🚀", fontSize = 32.sp)
          }

          Text(
            Translations.get(TranslationKey.PREM_GO_PREMIUM, lang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
          )

          Text(
            Translations.get(TranslationKey.PREM_GO_PREMIUM_DESC, lang),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
          )

          // Features checklist
          Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("✅", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
              Text(Translations.get(TranslationKey.PREM_BENEFIT_1, lang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("✅", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
              Text(Translations.get(TranslationKey.PREM_BENEFIT_2, lang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("✅", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
              Text(Translations.get(TranslationKey.PREM_BENEFIT_3, lang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("✅", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
              Text(Translations.get(TranslationKey.PREM_BENEFIT_4, lang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Button(
            onClick = {
              if (activity != null) {
                viewModel.purchaseAdFreeReal(activity) {
                  isPurchaseInProgress = true
                }
              } else {
                isPurchaseInProgress = true
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().testTag("buy_ad_free_premium_direct"),
            shape = RoundedCornerShape(14.dp)
          ) {
            val buttonText = Translations.get(TranslationKey.PREM_UPGRADE_BTN, lang).replace("%s", formattedPrice)
            Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = { onDismiss() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(Translations.get(TranslationKey.PREM_KEEP_FREE, lang), style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  }
}

@Composable
fun BannerAdComponent(
  viewModel: LoanCalculatorViewModel,
  onRemoveAdsClick: () -> Unit
) {
  val isAdFree by viewModel.isAdFreeVersion.collectAsState()
  if (isAdFree) return

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
      .padding(vertical = 4.dp, horizontal = 12.dp)
      .testTag("banner_ad_stub")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center
      ) {
        androidx.compose.ui.viewinterop.AndroidView(
          modifier = Modifier.fillMaxWidth().height(50.dp),
          factory = { ctx ->
            com.google.android.gms.ads.AdView(ctx).apply {
              setAdSize(com.google.android.gms.ads.AdSize.BANNER)
              adUnitId = "ca-app-pub-3940256099942544/6300978111"
              loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
            }
          }
        )
      }

      IconButton(
        onClick = onRemoveAdsClick,
        modifier = Modifier.size(24.dp).testTag("close_banner_ad_icon")
      ) {
        Text("✕", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
fun AdInteractiveScreen(
  featureName: String,
  lang: LanguageCode,
  onUnlocked: () -> Unit
) {
  var showSimulatedAd by remember { mutableStateOf(false) }
  var adLoading by remember { mutableStateOf(false) }
  var secondsLeft by remember { mutableStateOf(5) }
  val context = LocalContext.current

  val adsList = when (lang) {
    LanguageCode.ES -> listOf(
      "Refinanciación SmartRefi\n¡Las tasas de interés de préstamos hipotecarios han bajado! Refinancie ahora para asegurar un 4.25% APR y ahorrar un promedio de $350 al mes.",
      "Protección SafeShield\nDesde solo $45 al mes, proteja sus bienes inmuebles de manera segura con el proveedor de seguros de hogar mejor calificado del año.",
      "Transferencias EliteCard\nCombine múltiples tarjetas de crédito en un solo pago mensual con una tasa APR de introducción del 0% durante 18 meses."
    )
    LanguageCode.FR -> listOf(
      "Réfiancement SmartRefi\nLes taux d'intérêt sur les prêts hypothécaires sont en baisse ! Réfiancez dès maintenant pour obtenir un taux de 4,25% et économiser 350 $ par mois.",
      "Protection SafeShield\nÀ partir de seulement 45 $/mois, protégez vos biens immobiliers de manière fiable grâce à l'assurance habitation la mieux notée de l'année.",
      "Transfert de Solde EliteCard\nRegroupez plusieurs cartes de crédit en un seul paiement mensuel avec un taux d'intérêt de 0% pendant 18 mois."
    )
    LanguageCode.DE -> listOf(
      "SmartRefi Refinanzierung\nDie Bauzinsen sinken! Refinanzieren Sie jetzt zu einem h_effektiven Jahreszins von 4,25% und sparen Sie durchschnittlich 350 $ im Monat.",
      "SafeShield Heimschutz\nAb nur 45 $/Monat - sichern Sie Ihr Eigenheim bei dem am besten bewerteten Wohngebäudeversicherer des Jahres ab.",
      "EliteCard Guthabenübertrag\nFassen Sie mehrere Kreditkarten in einer monatlichen Rate mit 0% Einführungszins für 18 Monate zusammen."
    )
    LanguageCode.HI -> listOf(
      "स्मार्टरेफी पुनर्वित्त (SmartRefi Refinancing)\nगृह ऋण ब्याज दरें नीचे आ गई हैं! 4.25% APR सुरक्षित करने और हर महीने औसतन $350 बचाने के लिए अभी पुनर्वित्त करें।",
      "सेफशील्ड होमगार्ड (SafeShield Homeguard)\nमात्र $45/माह से शुरू, वर्ष के उच्चतम श्रेणी के गृह बीमा प्रदाता के साथ अपनी अचल संपत्ति को सुरक्षित रखें।",
      "एलीटकार्ड बैलेंस ट्रांसफर (EliteCard Transfers)\n18 महीनों के लिए 0% परिचयात्मक APR के साथ कई क्रेडिट कार्डों को एक मासिक भुगतान में संयोजित करें।",
    )
    LanguageCode.TA -> listOf(
      "ஸ்மார்ட்ரெஃபி மறுநிதியளிப்பு (SmartRefi Refinancing)\nவீட்டுக்கடன் வட்டி விகிதங்கள் குறைந்துள்ளன! 4.25% APR வட்டி விகிதத்தைப் பெறவும், சராசரியாக மாதத்திற்கு $350 சேமிக்கவும் இப்போதே விண்ணப்பிக்கவும்.",
      "சேஃப்ஷீல்டு ஹோம்கார்டு (SafeShield Homeguard)\nமாதம் வெறும் $45 முதல் தொடங்கும் வீட்டுக் காப்பீடு மூலம் உங்கள் சொத்துக்களைப் பாதுகாப்பாக வைத்திருங்கள்.",
      "எலைட்கார்டு பேலன்ஸ் டிரான்ஸ்ஃபர் (EliteCard Transfers)\n18 மாதங்களுக்கு 0% வட்டியில் உங்கள் பல கிரெடிட் கார்டு நிலுவைகளை ஒரே சுலபத் தவணையாக மாற்றிக் கொள்ளுங்கள்."
    )
    else -> listOf(
      "SmartRefi Refinancing\nHome loan interest rates are down! Refinance now to secure 4.25% APR and save an average of $350 every month.",
      "SafeShield Homeguard\nStarting at just $45/month, safeguard your real estate with the highest rated home insurance provider of the year.",
      "EliteCard Balance Transfers\nCombine multiple credit cards into one monthly payment with 0% introductory APR for 18 months."
    )
  }
  val activeAdIndex = remember { (0 until adsList.size).random() }

  if (showSimulatedAd) {
    Dialog(
      onDismissRequest = { /* force watching */ }
    ) {
      OutlinedCard(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          if (adLoading) {
            Text(
              Translations.get(TranslationKey.AD_SPONSOR_LOADING, lang),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            CircularProgressIndicator(
              modifier = Modifier.size(48.dp),
              color = MaterialTheme.colorScheme.primary,
              strokeWidth = 4.dp
            )
            Text(
              Translations.get(TranslationKey.AD_PREPARING_ENGINE, lang),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
            LaunchedEffect(Unit) {
              delay(1500)
              adLoading = false
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(Translations.get(TranslationKey.AD_SPONSORED_LABEL, lang), style = MaterialTheme.typography.labelSmall, color = Color.Black, fontWeight = FontWeight.Bold)
              }

              Text(
                text = if (secondsLeft > 0) Translations.get(TranslationKey.AD_SECONDS_LEFT, lang).format(secondsLeft) else Translations.get(TranslationKey.AD_COMPLETED, lang),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (secondsLeft > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
              )
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedCard(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text(if (activeAdIndex == 0) "🏠" else if (activeAdIndex == 1) "🛡️" else "💳", fontSize = 32.sp)
                }

                val fullAdText = adsList[activeAdIndex].split("\n")
                Text(
                  text = fullAdText[0],
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  textAlign = TextAlign.Center
                )
                Text(
                  text = fullAdText[1],
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center,
                  lineHeight = 18.sp
                )

                Button(
                  onClick = { /* click */ },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                  Text(Translations.get(TranslationKey.AD_LEARN_MORE, lang), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
              }
            }

            LaunchedEffect(secondsLeft) {
              if (secondsLeft > 0) {
                delay(1000)
                secondsLeft -= 1
              }
            }

            Button(
              onClick = {
                if (secondsLeft <= 0) {
                  onUnlocked()
                  showSimulatedAd = false
                  Toast.makeText(context, Translations.get(TranslationKey.AD_UNLOCK_SUCCESS, lang), Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(context, Translations.get(TranslationKey.AD_FINISH_PROMPT, lang), Toast.LENGTH_SHORT).show()
                }
              },
              enabled = secondsLeft <= 0,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
              ),
              modifier = Modifier.fillMaxWidth().testTag("claim_unlock_btn"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text(
                text = if (secondsLeft > 0) Translations.get(TranslationKey.AD_WATCH_TO_UNLOCK, lang) else Translations.get(TranslationKey.AD_CLAIM_UNLOCK, lang),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
              )
            }
          }
        }
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    OutlinedCard(
      modifier = Modifier.fillMaxWidth().align(Alignment.Center),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text("🔒", fontSize = 32.sp)
        }

        Text(
          text = Translations.get(TranslationKey.AD_X_IS_LOCKED, lang).format(featureName),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )

        Text(
          text = Translations.get(TranslationKey.AD_LOCKED_DESC, lang).format(featureName),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          lineHeight = 20.sp
        )

        Button(
          onClick = {
            secondsLeft = 5
            adLoading = true
            showSimulatedAd = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier.fillMaxWidth().testTag("unlock_${featureName.replace(" ", "_").lowercase()}"),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text(Translations.get(TranslationKey.AD_PLAY_VIDEO_BTN, lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun ComparisonTab(
  viewModel: LoanCalculatorViewModel,
  result: CalculationResult,
  lang: LanguageCode
) {
  val customCurrency by viewModel.customCurrencySymbol.collectAsState()
  val cur = customCurrency ?: lang.currencySymbol
  val isCompEnabled = true
  
  val compAmountVal by viewModel.comparisonLoanAmount.collectAsState()
  val compDownPaymentVal by viewModel.comparisonDownPayment.collectAsState()
  val compRateVal by viewModel.comparisonInterestRate.collectAsState()
  val compTermVal by viewModel.comparisonLoanTermYears.collectAsState()
  val compExtraVal by viewModel.comparisonExtraPayment.collectAsState()
  val compResult by viewModel.comparisonResult.collectAsState()
  val loanTypeVal by viewModel.loanType.collectAsState()

  LaunchedEffect(Unit) {
    viewModel.isComparisonActive.value = true
    viewModel.recalculateComparison()
  }

  var compAmountInput by remember { mutableStateOf(compAmountVal) }
  var compDownPaymentInput by remember { mutableStateOf(compDownPaymentVal) }
  var compRateInput by remember { mutableStateOf(compRateVal) }
  var compTermInput by remember { mutableStateOf(compTermVal) }
  var compExtraInput by remember { mutableStateOf(compExtraVal) }

  @Composable
  fun HeaderSection() {
    OutlinedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .padding(8.dp)
        ) {
          Text("⚖️", fontSize = 20.sp)
        }
        Column {
          Text(
            text = Translations.get(TranslationKey.COMPARE_ANALYTICS_TITLE, lang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = Translations.get(TranslationKey.COMPARE_ANALYTICS_SUBTITLE, lang),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }

  @Composable
  fun ErrorSection() {
    OutlinedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val errTitleText = when (lang) {
          LanguageCode.ES -> "⚠️ Se requiere escenario base"
          LanguageCode.FR -> "⚠️ Scénario de référence requis"
          LanguageCode.DE -> "⚠️ Baseline-Szenario erforderlich"
          LanguageCode.HI -> "⚠️ बेसलाइन परिदृश्य आवश्यक"
          LanguageCode.TA -> "⚠️ அடிப்படை கடன் விவரம் தேவை"
          else -> "⚠️ Baseline Scenario Required"
        }
        Text(errTitleText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
          text = Translations.get(TranslationKey.COMPARE_ERR_MSG, lang),
          style = MaterialTheme.typography.bodySmall,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }

  @Composable
  fun ConfigSection() {
    OutlinedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = Translations.get(TranslationKey.CONFIGURE_SCENARIO_B, lang),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(bottom = 12.dp)
        )

        if (loanTypeVal == "Mortgage") {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = compAmountInput,
              onValueChange = {
                val coerced = coerceInputString(it, 100000000.0, true)
                compAmountInput = coerced
                viewModel.updateComparison(true, coerced, compDownPaymentInput, compRateInput, compTermInput, compExtraInput)
              },
              label = { Text("${Translations.get(TranslationKey.HOME_PRICE, lang)} ($cur)") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("comparison_amount_input"),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
              )
            )

            OutlinedTextField(
              value = compDownPaymentInput,
              onValueChange = {
                val maxDp = compAmountInput.parseToDoubleOrNull() ?: 100000000.0
                val coerced = coerceInputString(it, maxDp, true)
                compDownPaymentInput = coerced
                viewModel.updateComparison(true, compAmountInput, coerced, compRateInput, compTermInput, compExtraInput)
              },
              label = { Text("${Translations.get(TranslationKey.DOWN_PAYMENT, lang)} ($cur)") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("comparison_down_payment_input"),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
              )
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = compRateInput,
              onValueChange = {
                val coerced = coerceInputString(it, 35.0, false)
                compRateInput = coerced
                viewModel.updateComparison(true, compAmountInput, compDownPaymentInput, coerced, compTermInput, compExtraInput)
              },
              label = { Text("${Translations.get(TranslationKey.ANNUAL_INTEREST, lang)} %") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("comparison_rate_input"),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
              )
            )

            OutlinedTextField(
              value = compTermInput,
              onValueChange = {
                val coerced = coerceInputString(it, 50.0, true)
                compTermInput = coerced
                viewModel.updateComparison(true, compAmountInput, compDownPaymentInput, compRateInput, coerced, compExtraInput)
              },
              label = { Text(Translations.get(TranslationKey.LOAN_TERM, lang)) },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("comparison_term_input"),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
              )
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = compExtraInput,
            onValueChange = {
              val coerced = coerceInputString(it, 1000000.0, true)
              compExtraInput = coerced
              viewModel.updateComparison(true, compAmountInput, compDownPaymentInput, compRateInput, compTermInput, coerced)
            },
            label = { Text(Translations.get(TranslationKey.EXTRA_PAYMENT, lang)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("comparison_extra_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
          )
        } else {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = compAmountInput,
              onValueChange = {
                val coerced = coerceInputString(it, 100000000.0, true)
                compAmountInput = coerced
                viewModel.updateComparison(true, coerced, "0", compRateInput, compTermInput, compExtraInput)
              },
              label = { Text("${Translations.get(TranslationKey.LOAN_AMOUNT, lang)} ($cur)") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("comparison_amount_input"),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
              )
            )

            OutlinedTextField(
              value = compRateInput,
              onValueChange = {
                val coerced = coerceInputString(it, 35.0, false)
                compRateInput = coerced
                viewModel.updateComparison(true, compAmountInput, "0", coerced, compTermInput, compExtraInput)
              },
              label = { Text("${Translations.get(TranslationKey.ANNUAL_INTEREST, lang)} %") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("comparison_rate_input"),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
              )
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = compTermInput,
              onValueChange = {
                val coerced = coerceInputString(it, 50.0, true)
                compTermInput = coerced
                viewModel.updateComparison(true, compAmountInput, "0", compRateInput, coerced, compExtraInput)
              },
              label = { Text(Translations.get(TranslationKey.LOAN_TERM, lang)) },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("comparison_term_input"),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
              )
            )

            OutlinedTextField(
              value = compExtraInput,
              onValueChange = {
                val coerced = coerceInputString(it, 1000000.0, true)
                compExtraInput = coerced
                viewModel.updateComparison(true, compAmountInput, "0", compRateInput, compTermInput, coerced)
              },
              label = { Text(Translations.get(TranslationKey.EXTRA_PAYMENT, lang)) },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("comparison_extra_input"),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
              )
            )
          }
        }
      }
    }
  }

  @Composable
  fun SideBySideMatrix() {
        OutlinedCard(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = Translations.get(TranslationKey.SAVINGS_MATRIX_TITLE, lang),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
              Text(Translations.get(TranslationKey.SUMMARY, lang), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.2f))
              val labelA = when (lang) {
                LanguageCode.ES -> "A (Base)"
                LanguageCode.FR -> "A (Référence)"
                LanguageCode.DE -> "A (Basis)"
                LanguageCode.HI -> "A (आधार रेखा)"
                LanguageCode.TA -> "A (அடிப்படை)"
                else -> "A (Baseline)"
              }
              val labelB = when (lang) {
                LanguageCode.ES -> "B (Alternativa)"
                LanguageCode.FR -> "B (Alternative)"
                LanguageCode.DE -> "B (Alternative)"
                LanguageCode.HI -> "B (வैकल्पिक)"
                LanguageCode.TA -> "B (மாற்று)"
                else -> "B (Alternate)"
              }
              Text(labelA, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
              Text(labelB, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }

            ComparisonDataRow(
              metric = Translations.get(TranslationKey.LOAN_AMOUNT, lang),
              valA = "$cur ${String.format("%,.0f", result.principalLoanAmount)}",
              valB = "$cur ${String.format("%,.0f", compResult.principalLoanAmount)}"
            )

            ComparisonDataRow(
              metric = Translations.get(TranslationKey.PRINCIPAL_AND_INTEREST, lang),
              valA = "$cur ${String.format("%,.2f", result.baseMonthlyPayment)}",
              valB = "$cur ${String.format("%,.2f", compResult.baseMonthlyPayment)}"
            )

            ComparisonDataRow(
              metric = Translations.get(TranslationKey.EXTRA_PAYMENT, lang),
              valA = "$cur ${String.format("%,.0f", result.totalExtraPaid / max(1, result.actualRepaymentMonths))}",
              valB = "$cur ${String.format("%,.0f", compResult.totalExtraPaid / max(1, compResult.actualRepaymentMonths))}"
            )

            ComparisonDataRow(
              metric = Translations.get(TranslationKey.LOAN_TERM, lang),
              valA = "${result.actualRepaymentMonths} ${Translations.get(TranslationKey.MONTH, lang)}",
              valB = "${compResult.actualRepaymentMonths} ${Translations.get(TranslationKey.MONTH, lang)}",
              highlightA = result.actualRepaymentMonths < compResult.actualRepaymentMonths,
              highlightB = compResult.actualRepaymentMonths < result.actualRepaymentMonths
            )

            ComparisonDataRow(
              metric = Translations.get(TranslationKey.TOTAL_INTEREST, lang),
              valA = "$cur ${String.format("%,.0f", result.totalInterestPaid)}",
              valB = "$cur ${String.format("%,.0f", compResult.totalInterestPaid)}",
              highlightA = result.totalInterestPaid < compResult.totalInterestPaid,
              highlightB = compResult.totalInterestPaid < result.totalInterestPaid
            )

            ComparisonDataRow(
              metric = Translations.get(TranslationKey.TOTAL_LOAN_COST, lang),
              valA = "$cur ${String.format("%,.0f", result.totalPaidAmount)}",
              valB = "$cur ${String.format("%,.0f", compResult.totalPaidAmount)}",
              highlightA = result.totalPaidAmount < compResult.totalPaidAmount,
              highlightB = compResult.totalPaidAmount < result.totalPaidAmount
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))

            val interestDiff = result.totalInterestPaid - compResult.totalInterestPaid
            if (interestDiff != 0.0) {
              OutlinedCard(
                colors = CardDefaults.cardColors(
                  containerColor = if (interestDiff < 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                  else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = if (interestDiff < 0) {
                    Translations.get(TranslationKey.SCENARIO_A_SAVES, lang).replace("%s", "$cur ${String.format("%,.0f", Math.abs(interestDiff))}")
                  } else {
                    Translations.get(TranslationKey.SCENARIO_B_SAVES, lang).replace("%s", "$cur ${String.format("%,.0f", Math.abs(interestDiff))}")
                  },
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (interestDiff < 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.padding(12.dp),
                  textAlign = TextAlign.Center
                )
              }
            } else {
              OutlinedCard(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = Translations.get(TranslationKey.EQUAL_INTEREST_MSG, lang),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(12.dp),
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isTablet = maxWidth > 600.dp
    
    if (isTablet) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          HeaderSection()
          if (!result.isValid) {
            ErrorSection()
          } else {
            ConfigSection()
          }
          Spacer(modifier = Modifier.height(72.dp))
        }

        Column(
          modifier = Modifier
            .weight(1.2f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          if (result.isValid && compResult.isValid) {
            SideBySideMatrix()
          }
          Spacer(modifier = Modifier.height(72.dp))
        }
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        HeaderSection()
        if (!result.isValid) {
          ErrorSection()
        } else {
          ConfigSection()
          if (compResult.isValid) {
            SideBySideMatrix()
          }
        }
        Spacer(modifier = Modifier.height(72.dp))
      }
    }
  }
}

@Composable
fun ReportBugDialog(lang: LanguageCode, onDismiss: () -> Unit) {
  val context = LocalContext.current
  var summary by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var severity by remember { mutableStateOf("Medium") }
  var email by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }
  var isSuccess by remember { mutableStateOf(false) }
  var severityMenuExpanded by remember { mutableStateOf(false) }

  val localizedLow = when(lang) {
    LanguageCode.ES -> "Baja 🟢"
    LanguageCode.FR -> "Faible 🟢"
    LanguageCode.DE -> "Niedrig 🟢"
    LanguageCode.HI -> "कम 🟢"
    LanguageCode.TA -> "குறைந்த 🟢"
    else -> "Low 🟢"
  }
  val localizedMedium = when(lang) {
    LanguageCode.ES -> "Media 🟡"
    LanguageCode.FR -> "Moyenne 🟡"
    LanguageCode.DE -> "Mittel 🟡"
    LanguageCode.HI -> "मध्यम 🟡"
    LanguageCode.TA -> "நடுத்தர 🟡"
    else -> "Medium 🟡"
  }
  val localizedHigh = when(lang) {
    LanguageCode.ES -> "Alta 🟠"
    LanguageCode.FR -> "Élevée 🟠"
    LanguageCode.DE -> "Hoch 🟠"
    LanguageCode.HI -> "उच्च 🟠"
    LanguageCode.TA -> "அதிக 🟠"
    else -> "High 🟠"
  }
  val localizedCritical = when(lang) {
    LanguageCode.ES -> "Crítica 🔴"
    LanguageCode.FR -> "Critique 🔴"
    LanguageCode.DE -> "Kritisch 🔴"
    LanguageCode.HI -> "गंभीर 🔴"
    LanguageCode.TA -> "மிகவும் ஆபத்தான 🔴"
    else -> "Critical 🔴"
  }

  val severityLocalizedMap = mapOf(
    "Low" to localizedLow,
    "Medium" to localizedMedium,
    "High" to localizedHigh,
    "Critical" to localizedCritical
  )
  
  Dialog(onDismissRequest = if (isSubmitting) {{}} else onDismiss) {
    OutlinedCard(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        if (isSuccess) {
          Text("🎉", fontSize = 48.sp, modifier = Modifier.testTag("bug_success_emoji"))
          Text(
            text = Translations.get(TranslationKey.BUG_SUCCESS_TITLE, lang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("bug_success_title")
          )
          Text(
            text = Translations.get(TranslationKey.BUG_SUCCESS_DESC, lang),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(8.dp))
          Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().testTag("bug_done_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(Translations.get(TranslationKey.DONE, lang))
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = Translations.get(TranslationKey.BUG_REPORT_TITLE, lang),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.testTag("bug_dialog_title")
            )
            IconButton(onClick = onDismiss, enabled = !isSubmitting, modifier = Modifier.testTag("bug_close_btn")) {
              Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
          }
          
          Text(
            text = Translations.get(TranslationKey.BUG_REPORT_DESC, lang),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          
          OutlinedTextField(
            value = summary,
            onValueChange = { summary = it },
            label = { Text(Translations.get(TranslationKey.BUG_SUMMARY_LABEL, lang)) },
            placeholder = { Text(Translations.get(TranslationKey.BUG_SUMMARY_PLACEHOLDER, lang)) },
            modifier = Modifier.fillMaxWidth().testTag("bug_summary_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isSubmitting,
            colors = OutlinedTextFieldDefaults.colors()
          )
          
          // Severity Selector
          Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = "${Translations.get(TranslationKey.BUG_SEVERITY_LABEL, lang)}: ${severityLocalizedMap[severity] ?: severity}",
              onValueChange = {},
              readOnly = true,
              trailingIcon = {
                IconButton(onClick = { if (!isSubmitting) severityMenuExpanded = true }, modifier = Modifier.testTag("bug_severity_dropdown_btn")) {
                  Text("▼", fontSize = 12.sp)
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .clickable { if (!isSubmitting) severityMenuExpanded = true }
                .testTag("bug_severity_input"),
              shape = RoundedCornerShape(12.dp),
              enabled = !isSubmitting
            )
            DropdownMenu(
              expanded = severityMenuExpanded,
              onDismissRequest = { severityMenuExpanded = false },
              modifier = Modifier.fillMaxWidth(0.8f)
            ) {
              severityLocalizedMap.forEach { (levelKey, levelLabel) ->
                DropdownMenuItem(
                  text = { Text(levelLabel) },
                  onClick = {
                    severity = levelKey
                    severityMenuExpanded = false
                  },
                  modifier = Modifier.testTag("bug_severity_option_$levelKey")
                )
              }
            }
          }

          OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(Translations.get(TranslationKey.BUG_DESC_LABEL, lang)) },
            placeholder = { Text(Translations.get(TranslationKey.BUG_DESC_PLACEHOLDER, lang)) },
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 100.dp)
              .testTag("bug_description_input"),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSubmitting
          )
          
          OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(Translations.get(TranslationKey.BUG_EMAIL_LABEL, lang)) },
            placeholder = { Text(Translations.get(TranslationKey.BUG_EMAIL_PLACEHOLDER, lang)) },
            modifier = Modifier.fillMaxWidth().testTag("bug_email_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
          )
          
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Button(
              onClick = onDismiss,
              modifier = Modifier.weight(1f).testTag("bug_cancel_button"),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
              shape = RoundedCornerShape(12.dp),
              enabled = !isSubmitting
            ) {
              Text(Translations.get(TranslationKey.BUG_CANCEL, lang))
            }
            
            Button(
              onClick = {
                if (summary.trim().isEmpty() || description.trim().isEmpty()) {
                  return@Button
                }
                isSubmitting = true
              },
              modifier = Modifier.weight(1.5f).testTag("bug_submit_button"),
              shape = RoundedCornerShape(12.dp),
              enabled = !isSubmitting && summary.trim().isNotEmpty() && description.trim().isNotEmpty()
            ) {
              if (isSubmitting) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp
                )
              } else {
                Text(Translations.get(TranslationKey.BUG_SUBMIT, lang))
              }
            }
          }
          
          if (isSubmitting) {
            LaunchedEffect(Unit) {
              val refNum = "LM-" + (100000..999999).random().toString()
              val recipient = "loopzerotech@gmail.com"
              val subject = "[Bug Report] Loan Math - Ref: $refNum"
              val body = """
                  === LOAN MATH DEBT CALCULATOR ===
                  Professional Bug Report Summary
                  
                  Reference Number: $refNum
                  Severity Level: $severity
                  Issue Title: $summary
                  Contact Email: ${if (email.trim().isNotEmpty()) email.trim() else "Anonymous User"}
                  
                  ------------------------------------------
                  DETAILED DESCRIPTION:
                  $description
                  ------------------------------------------
                  
                  METADATA & ENVIRONMENT:
                  Device OS Version: Android API ${android.os.Build.VERSION.SDK_INT}
                  Device Hardware Model: ${android.os.Build.MODEL} (Product: ${android.os.Build.PRODUCT})
                  Application build version: 1.0 (PRO Premium)
              """.trimIndent()

              val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
              }
              
              try {
                context.startActivity(Intent.createChooser(intent, "Send Email..."))
              } catch (e: Exception) {
                // Ignore fallback if mail container is missing in emulator
              }
              
              delay(1000)
              isSubmitting = false
              isSuccess = true
            }
          }
        }
      }
    }
  }
}

enum class HelpType {
    PMI,
    ARM,
    LTV,
    APR
}

@Composable
fun HelpTooltipDialog(
    helpType: HelpType,
    lang: LanguageCode,
    onDismiss: () -> Unit
) {
    val title = when (helpType) {
        HelpType.PMI -> Translations.get(TranslationKey.HELP_PMI_TITLE, lang)
        HelpType.ARM -> Translations.get(TranslationKey.HELP_ARM_TITLE, lang)
        HelpType.LTV -> Translations.get(TranslationKey.HELP_LTV_TITLE, lang)
        HelpType.APR -> Translations.get(TranslationKey.HELP_APR_TITLE, lang)
    }
    val description = when (helpType) {
        HelpType.PMI -> Translations.get(TranslationKey.HELP_PMI_DESC, lang)
        HelpType.ARM -> Translations.get(TranslationKey.HELP_ARM_DESC, lang)
        HelpType.LTV -> Translations.get(TranslationKey.HELP_LTV_DESC, lang)
        HelpType.APR -> Translations.get(TranslationKey.HELP_APR_DESC, lang)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("help_tooltip_confirm_button")
            ) {
                Text(
                    text = Translations.get(TranslationKey.DONE, lang),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("help_tooltip_dialog_${helpType.name.lowercase()}")
    )
}

@Composable
fun SettingsScreen(
  viewModel: LoanCalculatorViewModel,
  lang: LanguageCode,
  onDismiss: () -> Unit,
  onShowBugReport: () -> Unit
) {
  val customCurrency by viewModel.customCurrencySymbol.collectAsState()
  val isAdFree by viewModel.isAdFreeVersion.collectAsState()
  val colorTheme by viewModel.colorTheme.collectAsState()
  var showPrivacyDialog by remember { mutableStateOf(false) }

  if (showPrivacyDialog) {
    PrivacyPolicyDialog(lang = lang, onDismiss = { showPrivacyDialog = false })
  }

  Dialog(
    onDismissRequest = onDismiss
  ) {
    OutlinedCard(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 680.dp)
        .padding(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(24.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        // Header
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "⚙️",
              fontSize = 22.sp,
              modifier = Modifier.padding(end = 8.dp)
            )
            Text(
              text = Translations.get(TranslationKey.SETTINGS_TITLE, lang),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("settings_close_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 12.dp),
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // 0. Color Scheme Section
          item {
            Column {
              Text(
                text = "Color Scheme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
              )
              
              OutlinedCard(
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
              ) {
                Column {
                  val themes = listOf("blue" to "Blue (Default)", "red" to "Red", "green" to "Green", "yellow" to "Yellow")
                  themes.forEachIndexed { index, (mode, label) ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setColorTheme(mode) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (colorTheme == mode) FontWeight.Bold else FontWeight.Normal
                      )
                      if (colorTheme == mode) {
                        Text(
                          text = "✓",
                          fontSize = 18.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.primary
                        )
                      }
                    }
                    if (index < themes.size - 1) {
                      HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    }
                  }
                }
              }
            }
          }

          // 1. Language Section
          item {
            Column {
              Text(
                text = Translations.get(TranslationKey.SETTINGS_LANG, lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
              )
              
              OutlinedCard(
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
              ) {
                Column {
                  val languages = LanguageCode.values()
                  languages.forEachIndexed { index, option ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setLanguage(option) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = option.flagEmoji,
                          fontSize = 18.sp,
                          modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                          text = option.displayName,
                          style = MaterialTheme.typography.bodyLarge,
                          color = MaterialTheme.colorScheme.onSurface,
                          fontWeight = if (lang == option) FontWeight.Bold else FontWeight.Normal
                        )
                      }
                      if (lang == option) {
                        Text(
                          text = "✓",
                          fontSize = 18.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.primary
                        )
                      }
                    }
                    if (index < languages.size - 1) {
                      HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    }
                  }
                }
              }
            }
          }

          // 2. Currency Section
          item {
            Column {
              Text(
                text = Translations.get(TranslationKey.SETTINGS_CURR, lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
              )
              
              OutlinedCard(
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 4.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                  ) {
                    val currencies = listOf("$", "€", "£", "₹", "¥", "₩", "₪")
                    currencies.forEach { symbol ->
                      val isSelected = (customCurrency == symbol) || (customCurrency == null && lang.currencySymbol == symbol)
                      Box(
                        modifier = Modifier
                          .size(38.dp)
                          .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                          )
                          .clip(CircleShape)
                          .clickable { viewModel.setCustomCurrencySymbol(symbol) },
                        contentAlignment = Alignment.Center
                      ) {
                        Text(
                          text = symbol,
                          style = MaterialTheme.typography.bodyLarge,
                          fontWeight = FontWeight.Bold,
                          color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                      }
                    }
                  }
                  
                  Spacer(modifier = Modifier.height(12.dp))
                  
                  // Reset to default button
                  Button(
                    onClick = { viewModel.setCustomCurrencySymbol(null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                      contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                  ) {
                    val defaultSymbol = lang.currencySymbol
                    Text(
                      text = "Reset to Default ($defaultSymbol)",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }
              }
            }
          }

          // 3. Privacy Policy & App Info Section
          item {
            Column {
              Text(
                text = "App Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
              )

              OutlinedCard(
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
              ) {
                Column {
                  // View Privacy Policy option
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable { showPrivacyDialog = true }
                      .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "📄",
                      fontSize = 18.sp,
                      modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = Translations.get(TranslationKey.PRIVACY_POLICY_LABEL, lang),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Text(
                        text = "Read our official offline compliance document",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                      )
                    }
                    Text(
                      text = "➔",
                      color = MaterialTheme.colorScheme.primary,
                      fontWeight = FontWeight.Bold
                    )
                  }
                  
                  HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                  // Report bug option
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable {
                        onDismiss()
                        onShowBugReport()
                      }
                      .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "🪲",
                      fontSize = 18.sp,
                      modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "Report a Bug",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Text(
                        text = "Send diagnostics or feedback to loopzerotech@gmail.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                      )
                    }
                    Text(
                      text = "➔",
                      color = MaterialTheme.colorScheme.primary,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Footer Version info
        Text(
          text = "Loan Math v2.5.0 • Loop Zero",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
          modifier = Modifier.align(Alignment.CenterHorizontally),
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
fun PrivacyPolicyDialog(
  lang: LanguageCode,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    OutlinedCard(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 560.dp)
        .padding(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = Translations.get(TranslationKey.PRIVACY_POLICY_LABEL, lang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
          }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        val scrollState = rememberScrollState()
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp)
        ) {
          Text(
            text = """
              Privacy Policy for Loan Math
              Last Updated: June 7, 2026

              At Loan Math, we value your privacy. This Privacy Policy describes how your personal info is processed.

              1. Offline Calculation & Financial Data
              • All calculation inputs (mortgages, interest, down payments) are processed locally on your device. None of your inputs are sent to our servers.
              • Any saved configurations are stored locally inside sandboxed storage.

              2. GDPR Compliance (General Data Protection Regulation)
              • Loan Math is fully compliant with GDPR and UK GDPR rules.
              • Privacy by Design (Article 25): All financial computations reside strictly inside your device's sandboxed storage. No user registration is required.
              • Erasure Right (Article 17): You can instantly erase all local data by un-installing the app or clearing device storage (Settings > Apps > Loan Math > Storage > Clear Data).
              • Support Data Processing: Contact emails are handled via legitimate interest and solely used to answer bug/support concerns.

              3. Third-Party Services & Ads
              • Google AdMob context-appropriate advertisements may be loaded, in accordance with Google's dynamic privacy policies.
              • In-app simulation of premium services uses local sandboxing. No actual financial transactions are carried out on servers.

              4. Contact Us & Support
              • Email: loopzerotech@gmail.com
            """.trimIndent(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
          )
        }
      }
    }
  }
}


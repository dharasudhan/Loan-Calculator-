package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun prepareViewModel(): LoanCalculatorViewModel {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val viewModel = LoanCalculatorViewModel(context as Application)

    // Set ad-free features unlocked
    viewModel.isAdFreeVersion.value = true
    viewModel.isComparisonUnlocked.value = true
    viewModel.isDebtPlannerUnlocked.value = true
    viewModel.isRentVsBuyUnlocked.value = true

    // Populate data for Calculator and Amortization Tab
    viewModel.loanType.value = "Mortgage"
    viewModel.homePrice.value = "350000"
    viewModel.downPayment.value = "70000"
    viewModel.interestRate.value = "6.5"
    viewModel.loanTermYears.value = "30"
    viewModel.extraPayment.value = "200"
    viewModel.propertyTaxRate.value = "1.2"
    viewModel.homeInsurance.value = "1200"
    viewModel.pmiRate.value = "0.5"

    // Recalculate
    viewModel.updateInputs()

    // Populate data for Comparison
    viewModel.updateComparison(
      enabled = true,
      amount = "280000",
      downPaymentVal = "70000",
      rate = "5.8",
      term = "30",
      extraAmt = "400"
    )

    // Populate data for Debt Planner
    viewModel.addDebt("Credit Card Pro", 4500.0, 18.5, 120.0)
    viewModel.addDebt("Premium Auto Loan", 18000.0, 5.0, 350.0)
    viewModel.addDebt("College Tuition Loan", 25000.0, 4.2, 220.0)
    viewModel.updatePlannerBudget("900")
    viewModel.updateStrategy("Snowball")

    return viewModel
  }

  // ================= MOBILE SCREENSHOTS =================

  @Test
  @Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
  fun mobile_0_calculator() {
    val viewModel = prepareViewModel()
    composeTestRule.setContent { MyApplicationTheme { MainScreen(viewModel) } }
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/mobile_0_calculator.png")
  }

  @Test
  @Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
  fun mobile_1_amortization() {
    val viewModel = prepareViewModel()
    composeTestRule.setContent { MyApplicationTheme { MainScreen(viewModel) } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tab_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/mobile_1_amortization.png")
  }

  @Test
  @Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
  fun mobile_2_comparison() {
    val viewModel = prepareViewModel()
    composeTestRule.setContent { MyApplicationTheme { MainScreen(viewModel) } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tab_2").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/mobile_2_comparison.png")
  }

  @Test
  @Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
  fun mobile_3_debt_planner() {
    val viewModel = prepareViewModel()
    composeTestRule.setContent { MyApplicationTheme { MainScreen(viewModel) } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tab_3").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/mobile_3_debt_planner.png")
  }

  @Test
  @Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
  fun mobile_4_rent_vs_buy() {
    val viewModel = prepareViewModel()
    composeTestRule.setContent { MyApplicationTheme { MainScreen(viewModel) } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tab_4").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/mobile_4_rent_vs_buy.png")
  }

  // ================= 7 INCH TABLET SCREENSHOTS =================

  @Test
  @Config(qualifiers = "sw600dp-w600dp-h960dp-xhdpi", sdk = [34])
  fun tablet_7_0_calculator() {
    val viewModel = prepareViewModel()
    composeTestRule.setContent { MyApplicationTheme { MainScreen(viewModel) } }
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tablet_7_0_calculator.png")
  }

  @Test
  @Config(qualifiers = "sw600dp-w600dp-h960dp-xhdpi", sdk = [34])
  fun tablet_7_1_amortization() {
    val viewModel = prepareViewModel()
    composeTestRule.setContent { MyApplicationTheme { MainScreen(viewModel) } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tab_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tablet_7_1_amortization.png")
  }

  @Test
  @Config(qualifiers = "sw600dp-w600dp-h960dp-xhdpi", sdk = [34])
  fun tablet_7_2_comparison() {
    val viewModel = prepareViewModel()
    composeTestRule.setContent { MyApplicationTheme { MainScreen(viewModel) } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tab_2").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tablet_7_2_comparison.png")
  }

  @Test
  @Config(qualifiers = "sw600dp-w600dp-h960dp-xhdpi", sdk = [34])
  fun tablet_7_3_debt_planner() {
    val viewModel = prepareViewModel()
    composeTestRule.setContent { MyApplicationTheme { MainScreen(viewModel) } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tab_3").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tablet_7_3_debt_planner.png")
  }

  @Test
  @Config(qualifiers = "sw600dp-w600dp-h960dp-xhdpi", sdk = [34])
  fun tablet_7_4_rent_vs_buy() {
    val viewModel = prepareViewModel()
    composeTestRule.setContent { MyApplicationTheme { MainScreen(viewModel) } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tab_4").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tablet_7_4_rent_vs_buy.png")
  }

  // ================= 10 INCH TABLET SCREENSHOTS =================

  @Test
  @Config(qualifiers = "sw800dp-w800dp-h1280dp-xhdpi", sdk = [34])
  fun tablet_10_0_calculator() {
    val viewModel = prepareViewModel()
    composeTestRule.setContent { MyApplicationTheme { MainScreen(viewModel) } }
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tablet_10_0_calculator.png")
  }

  @Test
  @Config(qualifiers = "sw800dp-w800dp-h1280dp-xhdpi", sdk = [34])
  fun tablet_10_1_amortization() {
    val viewModel = prepareViewModel()
    composeTestRule.setContent { MyApplicationTheme { MainScreen(viewModel) } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tab_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tablet_10_1_amortization.png")
  }

  @Test
  @Config(qualifiers = "sw800dp-w800dp-h1280dp-xhdpi", sdk = [34])
  fun tablet_10_2_comparison() {
    val viewModel = prepareViewModel()
    composeTestRule.setContent { MyApplicationTheme { MainScreen(viewModel) } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tab_2").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tablet_10_2_comparison.png")
  }

  @Test
  @Config(qualifiers = "sw800dp-w800dp-h1280dp-xhdpi", sdk = [34])
  fun tablet_10_3_debt_planner() {
    val viewModel = prepareViewModel()
    composeTestRule.setContent { MyApplicationTheme { MainScreen(viewModel) } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tab_3").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tablet_10_3_debt_planner.png")
  }

  @Test
  @Config(qualifiers = "sw800dp-w800dp-h1280dp-xhdpi", sdk = [34])
  fun tablet_10_4_rent_vs_buy() {
    val viewModel = prepareViewModel()
    composeTestRule.setContent { MyApplicationTheme { MainScreen(viewModel) } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tab_4").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tablet_10_4_rent_vs_buy.png")
  }
}

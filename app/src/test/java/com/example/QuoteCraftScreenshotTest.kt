package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.screens.ExecutiveKpiCard
import com.example.ui.theme.QuoteCraftTheme
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
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class QuoteCraftScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun quotecraft_kpi_card_screenshot() {
    composeTestRule.setContent {
      QuoteCraftTheme {
        ExecutiveKpiCard(
          totalProposals = 12,
          wonProposals = 8,
          marginReviewCount = 2,
          verifiedRevenuePkr = 35000.0,
          onNavigateToLedger = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/quotecraft_kpi.png")
  }
}

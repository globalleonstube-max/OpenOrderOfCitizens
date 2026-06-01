package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.theme.MyApplicationTheme
import com.example.presentation.OpenOrderSocialOSApp
import com.example.presentation.SuiteViewModel
import android.app.Application
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
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun greeting_screenshot() {
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @Test
  fun testOnboardingAndTabNavigationFlow() {
    composeTestRule.waitForIdle()
    // Find skip button on onboarding, click it
    try {
      composeTestRule.onNodeWithTag("onboarding_skip_btn").performClick()
    } catch (e: Throwable) {
      // In case user settings already had onboarding false, catch it gracefully
    }
    composeTestRule.waitForIdle()

    // Find and click sync button
    composeTestRule.onNodeWithTag("sync_btn").performClick()
    composeTestRule.waitForIdle()
  }
}


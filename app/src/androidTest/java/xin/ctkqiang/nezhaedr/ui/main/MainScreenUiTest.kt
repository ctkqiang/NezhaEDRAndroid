package xin.ctkqiang.nezhaedr.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import xin.ctkqiang.nezhaedr.MainActivity

@RunWith(AndroidJUnit4::class)
class MainScreenUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testHeaderAndNavigationTabs() {
        // Verify Nezha EDR Header is visible
        composeTestRule.onNodeWithText("NEZHA EDR").assertIsDisplayed()

        // Navigate to Rules Tab
        composeTestRule.onNodeWithText("Rules").performClick()
        composeTestRule.waitForIdle()

        // Navigate to Alerts Tab
        composeTestRule.onNodeWithText("Alerts").performClick()
        composeTestRule.waitForIdle()

        // Navigate to Vault Tab
        composeTestRule.onNodeWithText("Vault").performClick()
        composeTestRule.waitForIdle()

        // Navigate to Settings Tab
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()

        // Return to Sentinel (Dashboard) Tab
        composeTestRule.onNodeWithText("Sentinel").performClick()
        composeTestRule.waitForIdle()
    }
}

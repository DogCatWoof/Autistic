package org.meow.autistic

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNav_threeTabsDisplayed() {
        // Each label appears in both the bottom nav and the drawer — [0] is the bottom nav item
        composeTestRule.onAllNodesWithText("Todo")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Scan")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Notes")[0].assertIsDisplayed()
    }

    @Test
    fun defaultTab_showsTodoScreen() {
        composeTestRule.onNodeWithContentDescription("Add Todo").assertIsDisplayed()
    }

    @Test
    fun clickTodoTab_showsTodoScreen() {
        // Clicking Todo opens the sub-nav sheet; FAB remains accessible behind the sheet
        composeTestRule.onAllNodesWithText("Todo")[0].performClick()
        composeTestRule.onNodeWithContentDescription("Add Todo").assertIsDisplayed()
    }

    @Test
    fun clickScanTab_showsScanScreen() {
        // Scan has no sub-items — navigates directly
        composeTestRule.onAllNodesWithText("Scan")[0].performClick()
        composeTestRule.onNodeWithText("Product database not available").assertIsDisplayed()
    }

    @Test
    fun clickNotesTab_showsNotesScreen() {
        // Notes has no sub-items — navigates directly
        composeTestRule.onAllNodesWithText("Notes")[0].performClick()
        composeTestRule.onNodeWithText("Notes Screen").assertIsDisplayed()
    }

    @Test
    fun drawer_showsSubItemsInline() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        // Sub-items are rendered inline below their parent in the drawer
        composeTestRule.onAllNodesWithText("Today")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Events")[0].assertIsDisplayed()
    }

    @Test
    fun drawer_subItemClick_navigatesToTodayScreen() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onAllNodesWithText("Today")[0].performClick()
        composeTestRule.onNodeWithText("Daily Screen").assertIsDisplayed()
    }

    @Test
    fun drawer_subItemClick_navigatesToEventsScreen() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onAllNodesWithText("Events")[0].performClick()
        composeTestRule.onNodeWithText("Events Screen").assertIsDisplayed()
    }

    @Test
    fun drawerCloseButton_closesDrawer() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithContentDescription("Close menu").performClick()
        composeTestRule.onNodeWithContentDescription("Add Todo").assertIsDisplayed()
    }

    @Test
    fun settingsButton_showsSettingsTitle() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun settingsButton_togglesBackToCurrentTab() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithContentDescription("Add Todo").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_showsConnectButton_whenNotAuthenticated() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        // Sign out first if the device is already authenticated from a prior session
        val alreadyConnected = composeTestRule
            .onAllNodesWithText("Disconnect Google")
            .fetchSemanticsNodes()
            .isNotEmpty()
        if (alreadyConnected) {
            composeTestRule.onNodeWithText("Disconnect Google").performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithText("Connect Google Account").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_syncItem_opensProductsSyncList() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Sync").performClick()
        composeTestRule.onNodeWithText("Open Food Facts").assertIsDisplayed()
    }

    @Test
    fun syncScreen_showsTasksAndCalendarItems() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Sync").performClick()
        composeTestRule.onNodeWithText("Tasks").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calendar").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_syncSubScreen_backReturnsToSettings() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Sync").performClick()
        composeTestRule.onNodeWithText("Open Food Facts").assertIsDisplayed()
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.onNodeWithText("Sync").assertIsDisplayed()
    }
}

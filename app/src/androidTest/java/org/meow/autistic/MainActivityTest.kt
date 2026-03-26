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
    fun bottomNav_allFiveTabsDisplayed() {
        // "Todo" appears in both the top bar title and the nav tab label when it's the active tab
        composeTestRule.onAllNodesWithText("Todo")[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily").assertIsDisplayed()
        composeTestRule.onNodeWithText("Events").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mood").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notes").assertIsDisplayed()
    }

    @Test
    fun defaultTab_showsTodoScreen() {
        composeTestRule.onNodeWithContentDescription("Add Todo").assertIsDisplayed()
    }

    @Test
    fun clickDailyTab_showsDailyScreen() {
        composeTestRule.onNodeWithText("Daily").performClick()
        composeTestRule.onNodeWithText("Daily Screen").assertIsDisplayed()
    }

    @Test
    fun clickEventsTab_showsEventsScreen() {
        composeTestRule.onNodeWithText("Events").performClick()
        composeTestRule.onNodeWithText("Events Screen").assertIsDisplayed()
    }

    @Test
    fun clickMoodTab_showsMoodScreen() {
        composeTestRule.onNodeWithText("Mood").performClick()
        composeTestRule.onNodeWithText("Mood Screen").assertIsDisplayed()
    }

    @Test
    fun clickNotesTab_showsNotesScreen() {
        composeTestRule.onNodeWithText("Notes").performClick()
        composeTestRule.onNodeWithText("Notes Screen").assertIsDisplayed()
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
}

package org.meow.autistic.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaceholderScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dailyScreen_showsCorrectLabel() {
        composeTestRule.setContent { DailyScreen() }
        composeTestRule.onNodeWithText("Daily Screen").assertIsDisplayed()
    }

    @Test
    fun eventsScreen_showsCorrectLabel() {
        composeTestRule.setContent { EventsScreen() }
        composeTestRule.onNodeWithText("Events Screen").assertIsDisplayed()
    }

    @Test
    fun moodScreen_showsCorrectLabel() {
        composeTestRule.setContent { MoodScreen() }
        composeTestRule.onNodeWithText("Mood Screen").assertIsDisplayed()
    }

    @Test
    fun notesScreen_showsCorrectLabel() {
        composeTestRule.setContent { NotesScreen() }
        composeTestRule.onNodeWithText("Notes Screen").assertIsDisplayed()
    }
}

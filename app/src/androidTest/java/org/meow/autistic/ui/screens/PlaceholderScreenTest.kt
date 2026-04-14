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
    fun moodScreen_emptyState_showsMessage() {
        composeTestRule.setContent { MoodScreen() }
        composeTestRule.onNodeWithText("No mood readings yet.").assertIsDisplayed()
    }

    @Test
    fun notesScreen_showsNotesTitle() {
        composeTestRule.setContent { NotesScreen() }
        composeTestRule.onNodeWithText("Notes").assertIsDisplayed()
    }
}

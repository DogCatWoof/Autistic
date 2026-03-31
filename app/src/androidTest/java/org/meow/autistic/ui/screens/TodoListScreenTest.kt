package org.meow.autistic.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.assertIsEnabled
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.meow.autistic.data.todo.TodoDatabase

@RunWith(AndroidJUnit4::class)
class TodoListScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        runBlocking {
            TodoDatabase.getDatabase(ApplicationProvider.getApplicationContext())
                .clearAllTables()
        }
        composeTestRule.setContent { TodoListScreen() }
    }

    @Test
    fun fab_isDisplayed() {
        composeTestRule.onNodeWithContentDescription("Add Todo").assertIsDisplayed()
    }

    @Test
    fun fabClick_showsAddDialog() {
        composeTestRule.onNodeWithContentDescription("Add Todo").performClick()
        composeTestRule.onNodeWithText("New Task").assertIsDisplayed()
    }

    @Test
    fun addDialog_cancelDismissesDialog() {
        composeTestRule.onNodeWithContentDescription("Add Todo").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("New Task").assertDoesNotExist()
    }

    @Test
    fun addDialog_saveDisabledWhenTaskBlank() {
        composeTestRule.onNodeWithContentDescription("Add Todo").performClick()
        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun addDialog_saveEnabledAfterTyping() {
        composeTestRule.onNodeWithContentDescription("Add Todo").performClick()
        composeTestRule.onNodeWithText("Task description").performTextInput("Buy milk")
        composeTestRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun completedTodo_isHiddenFromList() {
        composeTestRule.onNodeWithContentDescription("Add Todo").performClick()
        composeTestRule.onNodeWithText("Task description").performTextInput("Finish report")
        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.onNodeWithText("Finish report").assertIsDisplayed()

        composeTestRule.onNodeWithText("Finish report").performTouchInput { swipeRight() }

        composeTestRule.onNodeWithText("Finish report").assertDoesNotExist()
    }

    @Test
    fun addDialog_notesFieldDisplayed() {
        composeTestRule.onNodeWithContentDescription("Add Todo").performClick()
        composeTestRule.onNodeWithText("Notes").assertIsDisplayed()
    }

    @Test
    fun addDialog_notesAreShownOnTodoItem() {
        composeTestRule.onNodeWithContentDescription("Add Todo").performClick()
        composeTestRule.onNodeWithText("Task description").performTextInput("My task")
        composeTestRule.onNodeWithText("Notes").performTextInput("Some note here")
        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.onNodeWithText("Some note here").assertIsDisplayed()
    }
}

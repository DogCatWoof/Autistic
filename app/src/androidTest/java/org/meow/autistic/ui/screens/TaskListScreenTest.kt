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
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.assertIsEnabled
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.meow.autistic.data.task.TaskDatabase

@RunWith(AndroidJUnit4::class)
class TaskListScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        runBlocking {
            TaskDatabase.getDatabase(ApplicationProvider.getApplicationContext())
                .clearAllTables()
        }
        composeTestRule.setContent { TaskListScreen() }
    }

    @Test
    fun fab_isDisplayed() {
        composeTestRule.onNodeWithContentDescription("Add Task").assertIsDisplayed()
    }

    private fun openAddTaskDialog() {
        composeTestRule.onNodeWithContentDescription("Add Task").performClick()
        composeTestRule.onNodeWithContentDescription("Task").performClick()
    }

    @Test
    fun fabClick_showsSpeedDial() {
        composeTestRule.onNodeWithContentDescription("Add Task").performClick()
        composeTestRule.onNodeWithText("Task").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calendar Event").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily Task").assertIsDisplayed()
    }

    @Test
    fun fabClick_taskOption_showsAddDialog() {
        openAddTaskDialog()
        composeTestRule.onNodeWithText("New Task").assertIsDisplayed()
    }

    @Test
    fun addDialog_cancelDismissesDialog() {
        openAddTaskDialog()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("New Task").assertDoesNotExist()
    }

    @Test
    fun addDialog_saveDisabledWhenTaskBlank() {
        openAddTaskDialog()
        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun addDialog_saveEnabledAfterTyping() {
        openAddTaskDialog()
        composeTestRule.onNodeWithText("Task description").performTextInput("Buy milk")
        composeTestRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun completedTask_isRemovedFromList() {
        openAddTaskDialog()
        composeTestRule.onNodeWithText("Task description").performTextInput("Finish report")
        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.onNodeWithText("Finish report").assertIsDisplayed()

        composeTestRule.onNodeWithText("Finish report").performTouchInput { swipeRight() }

        composeTestRule.onNodeWithText("Finish report").assertDoesNotExist()
    }

    @Test
    fun swipeLeft_removesTask() {
        openAddTaskDialog()
        composeTestRule.onNodeWithText("Task description").performTextInput("Buy milk")
        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.onNodeWithText("Buy milk").assertIsDisplayed()

        composeTestRule.onNodeWithText("Buy milk").performTouchInput { swipeLeft() }

        composeTestRule.onNodeWithText("Buy milk").assertDoesNotExist()
    }

    @Test
    fun addDialog_notesFieldDisplayed() {
        openAddTaskDialog()
        composeTestRule.onNodeWithText("Notes").assertIsDisplayed()
    }

    @Test
    fun addDialog_notesAreShownOnTaskItem() {
        openAddTaskDialog()
        composeTestRule.onNodeWithText("Task description").performTextInput("My task")
        composeTestRule.onNodeWithText("Notes").performTextInput("Some note here")
        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.onNodeWithText("Some note here").assertIsDisplayed()
    }
}

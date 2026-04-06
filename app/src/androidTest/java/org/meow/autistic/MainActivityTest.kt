package org.meow.autistic

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Tasks as GmsTasks
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.meow.autistic.data.auth.TokenStore
import org.meow.autistic.data.navigation.NavPreferencesStore
import org.meow.autistic.data.todo.TaskDatabase

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearState() {
        val activity = composeTestRule.activity
        TokenStore.create(activity).clear()
        GmsTasks.await(
            GoogleSignIn.getClient(activity, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        )
        runBlocking {
            TaskDatabase.getDatabase(activity).clearAllTables()
            NavPreferencesStore.clear(activity)
        }
    }

    @Test
    fun bottomNav_threeTabsDisplayed() {
        composeTestRule.onNodeWithTag("nav_tab_task").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_scan").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_notes").assertIsDisplayed()
    }

    @Test
    fun defaultTab_showsTodoScreen() {
        composeTestRule.onNodeWithContentDescription("Add Task").assertIsDisplayed()
    }

    @Test
    fun clickTodoTab_showsTodoScreen() {
        composeTestRule.onNodeWithTag("nav_tab_task").performClick()
        composeTestRule.onNodeWithContentDescription("Add Task").assertIsDisplayed()
    }

    @Test
    fun clickScanTab_showsScanScreen() {
        // Scan has no sub-items — navigates directly
        composeTestRule.onNodeWithTag("nav_tab_scan").performClick()
        composeTestRule.onNodeWithText("Product database not available").assertIsDisplayed()
    }

    @Test
    fun clickNotesTab_showsNotesScreen() {
        // Notes has no sub-items — navigates directly
        composeTestRule.onNodeWithTag("nav_tab_notes").performClick()
        composeTestRule.onNodeWithText("Notes Screen").assertIsDisplayed()
    }

    @Test
    fun drawerCloseButton_closesDrawer() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithContentDescription("Close menu").performClick()
        composeTestRule.onNodeWithContentDescription("Add Task").assertIsDisplayed()
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
        composeTestRule.onNodeWithContentDescription("Add Task").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_showsConnectButton_whenNotAuthenticated() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
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

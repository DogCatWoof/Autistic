package org.meow.autistic

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import org.meow.autistic.data.navigation.NavStateStore
import org.meow.autistic.data.task.TaskDatabase

@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearState() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation
            .executeShellCommand(
                "pm grant ${instrumentation.targetContext.packageName} android.permission.CAMERA"
            )
            .close()
        val activity = composeTestRule.activity
        TokenStore.create(activity).clear()
        GmsTasks.await(
            GoogleSignIn.getClient(activity, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        )
        runBlocking {
            TaskDatabase.getDatabase(activity).clearAllTables()
            NavPreferencesStore.clear(activity)
            NavStateStore.clear(activity)
        }
        // Activity already launched with persisted destination; navigate to Task for a clean baseline.
        composeTestRule.onNodeWithTag("nav_tab_task").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun bottomNav_allTabsDisplayed() {
        composeTestRule.onNodeWithTag("nav_tab_task").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_scan").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_notes").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_mood").assertIsDisplayed()
    }

    @Test
    fun defaultTab_showsTaskScreen() {
        composeTestRule.onNodeWithContentDescription("Add Task").assertIsDisplayed()
    }

    @Test
    fun clickTaskTab_showsTaskScreen() {
        composeTestRule.onNodeWithTag("nav_tab_task").performClick()
        composeTestRule.onNodeWithContentDescription("Add Task").assertIsDisplayed()
    }

    @Test
    fun clickScanTab_showsScanScreen() {
        composeTestRule.onNodeWithTag("nav_tab_scan").performClick()
        composeTestRule.onNodeWithTag("scan_screen").assertExists()
    }

    @Test
    fun clickNotesTab_showsNotesScreen() {
        composeTestRule.onNodeWithTag("nav_tab_notes").performClick()
        composeTestRule.onNodeWithContentDescription("New Note").assertIsDisplayed()
    }

    @Test
    fun clickMoodTab_showsMoodScreen() {
        composeTestRule.onNodeWithTag("nav_tab_mood").performClick()
        composeTestRule.onNodeWithText("No mood readings yet.").assertIsDisplayed()
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
    fun settingsScreen_syncItem_expandsInPlace() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Sync").performClick()
        composeTestRule.onNodeWithText("Daily Reset").assertIsDisplayed()
    }

    @Test
    fun syncExpanded_showsAllThreeSyncItems() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Sync").performClick()
        composeTestRule.onNodeWithText("Daily Reset").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Task List").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Google Drive Backup").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun settingsScreen_syncCollapsesOnSecondClick() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Sync").performClick()
        composeTestRule.onNodeWithText("Task List").performScrollTo().assertIsDisplayed()
        // Use the icon content description to target the header, not the sync buttons inside items
        composeTestRule.onNodeWithContentDescription("Collapse sync").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Daily reset, task list, backup").assertIsDisplayed()
    }
}

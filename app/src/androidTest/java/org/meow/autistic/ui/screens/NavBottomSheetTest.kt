package org.meow.autistic.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Done
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.meow.autistic.NavigationItem

/**
 * Tests NavBottomSheet sub-navigation options by rendering the sheet directly,
 * avoiding the popup-window limitations of the full-activity test.
 */
@RunWith(AndroidJUnit4::class)
class NavBottomSheetTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val todoNavItem = NavigationItem(
        title = "Todo",
        selectedIcon = Icons.Filled.Done,
        unselectedIcon = Icons.Outlined.Done,
        subItems = listOf("Todo", "Today", "Events"),
    )

    @Test
    fun navSheet_displaysAllSubItems() {
        composeTestRule.setContent {
            NavBottomSheet(item = todoNavItem, onSubItemSelected = {}, onDismiss = {})
        }
        // "Today" and "Events" are unique; "Todo" appears as both title and list item
        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
        composeTestRule.onNodeWithText("Events").assertIsDisplayed()
    }

    @Test
    fun navSheet_eventsSelection_callsCallback() {
        var selected = ""
        composeTestRule.setContent {
            NavBottomSheet(
                item = todoNavItem,
                onSubItemSelected = { selected = it },
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("Events").performClick()
        assert(selected == "Events") { "Expected 'Events' but got '$selected'" }
    }

    @Test
    fun navSheet_todaySelection_callsCallback() {
        var selected = ""
        composeTestRule.setContent {
            NavBottomSheet(
                item = todoNavItem,
                onSubItemSelected = { selected = it },
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("Today").performClick()
        assert(selected == "Today") { "Expected 'Today' but got '$selected'" }
    }
}

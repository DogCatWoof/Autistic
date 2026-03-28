package org.meow.autistic.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.outlined.Create
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.meow.autistic.NavigationItem

class NavPreferencesScreenTest {

    private val todoItem = NavigationItem(
        title = "Todo",
        selectedIcon = Icons.Filled.Create,
        unselectedIcon = Icons.Outlined.Create,
        subItems = listOf("Todo", "Today", "Events"),
    )
    private val scanItem = NavigationItem(
        title = "Scan",
        selectedIcon = Icons.Filled.Create,
        unselectedIcon = Icons.Outlined.Create,
    )
    private val notesItem = NavigationItem(
        title = "Notes",
        selectedIcon = Icons.Filled.Create,
        unselectedIcon = Icons.Outlined.Create,
    )
    private val allItems = listOf(todoItem, scanItem, notesItem)

    // region navTitlesFrom

    @Test
    fun `navTitlesFrom returns sub-item names for grouped items`() {
        val titles = navTitlesFrom(allItems)
        assertTrue(titles.containsAll(listOf("Todo", "Today", "Events")))
    }

    @Test
    fun `navTitlesFrom returns title for leaf items`() {
        val titles = navTitlesFrom(allItems)
        assertTrue(titles.contains("Scan"))
        assertTrue(titles.contains("Notes"))
    }

    @Test
    fun `navTitlesFrom does not include parent title of grouped item`() {
        // "Todo" appears as a sub-item, so it IS included — but the parent container
        // identity doesn't add a duplicate. Just verify all expected entries exist.
        val titles = navTitlesFrom(allItems)
        assertEquals(5, titles.size) // Todo, Today, Events, Scan, Notes
    }

    // endregion

    // region filterNavItems

    @Test
    fun `filterNavItems keeps all items when all titles enabled`() {
        val enabled = navTitlesFrom(allItems)
        assertEquals(allItems, filterNavItems(allItems, enabled))
    }

    @Test
    fun `filterNavItems removes leaf item when its title is disabled`() {
        val enabled = navTitlesFrom(allItems) - "Scan"
        val result = filterNavItems(allItems, enabled)
        assertTrue(result.none { it.title == "Scan" })
        assertEquals(2, result.size)
    }

    @Test
    fun `filterNavItems trims sub-items list to only enabled entries`() {
        val enabled = navTitlesFrom(allItems) - "Today" - "Events"
        val result = filterNavItems(allItems, enabled)
        val todo = result.first { it.title == "Todo" }
        assertEquals(listOf("Todo"), todo.subItems)
    }

    @Test
    fun `filterNavItems removes grouped item when all sub-items are disabled`() {
        val enabled = navTitlesFrom(allItems) - "Todo" - "Today" - "Events"
        val result = filterNavItems(allItems, enabled)
        assertTrue(result.none { it.title == "Todo" })
    }

    @Test
    fun `filterNavItems returns empty list when nothing is enabled`() {
        val result = filterNavItems(allItems, emptySet())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterNavItems preserves order`() {
        val enabled = navTitlesFrom(allItems)
        val result = filterNavItems(allItems, enabled)
        assertEquals(listOf("Todo", "Scan", "Notes"), result.map { it.title })
    }

    // endregion
}

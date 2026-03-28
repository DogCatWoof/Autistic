package org.meow.autistic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import org.meow.autistic.NavigationItem
import org.meow.autistic.data.navigation.NavPreferencesStore

/**
 * Settings sub-screen for choosing which items appear in the bottom navigation bar.
 *
 * Items without sub-items show a single "Show in navigation" toggle.
 * Items with sub-items show one toggle per sub-item; the parent is hidden
 * automatically when all its sub-items are disabled.
 */
@Composable
fun NavPreferencesScreen(
    allItems: List<NavigationItem>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allTitles = remember(allItems) { navTitlesFrom(allItems) }
    val savedEnabled by NavPreferencesStore.getEnabledFlow(context).collectAsState(initial = null)
    val enabled = savedEnabled ?: allTitles

    fun toggle(title: String, checked: Boolean) {
        scope.launch { NavPreferencesStore.setEnabled(context, title, checked, allTitles) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        allItems.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider()
            if (item.subItems.isEmpty()) {
                SettingsSectionLabel(item.title)
                ListItem(
                    headlineContent = { Text("Show in navigation") },
                    trailingContent = {
                        Checkbox(
                            checked = item.title in enabled,
                            onCheckedChange = { toggle(item.title, it) },
                        )
                    },
                    modifier = Modifier.clickable { toggle(item.title, item.title !in enabled) },
                )
            } else {
                SettingsSectionLabel(item.title)
                item.subItems.forEach { subItem ->
                    ListItem(
                        headlineContent = { Text(subItem) },
                        trailingContent = {
                            Checkbox(
                                checked = subItem in enabled,
                                onCheckedChange = { toggle(subItem, it) },
                            )
                        },
                        modifier = Modifier.clickable { toggle(subItem, subItem !in enabled) },
                    )
                }
            }
        }
    }
}

/** Returns the trackable titles for [items]: sub-item names for grouped items, title for leaf items. */
internal fun navTitlesFrom(items: List<NavigationItem>): Set<String> =
    items.flatMap { if (it.subItems.isEmpty()) listOf(it.title) else it.subItems }.toSet()

/** Filters [allItems] to only those enabled by [enabled], trimming sub-item lists accordingly. */
internal fun filterNavItems(allItems: List<NavigationItem>, enabled: Set<String>): List<NavigationItem> =
    allItems.mapNotNull { item ->
        if (item.subItems.isEmpty()) {
            if (item.title in enabled) item else null
        } else {
            val visible = item.subItems.filter { it in enabled }
            if (visible.isEmpty()) null else item.copy(subItems = visible)
        }
    }

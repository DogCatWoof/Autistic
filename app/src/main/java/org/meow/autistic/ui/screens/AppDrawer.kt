package org.meow.autistic.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.meow.autistic.NavigationItem

/**
 * Slide-in drawer content listing all main navigation destinations and their sub-items.
 *
 * @param items The full list of navigation destinations.
 * @param currentDestination The currently active destination name.
 * @param showSettings Whether the Settings screen is currently shown (deselects all items).
 * @param onItemSelect Called with the chosen item when the user taps a parent drawer item.
 * @param onSubItemSelect Called with the sub-item name when the user taps a sub-item.
 * @param onClose Called when the user taps the hamburger icon to close the drawer.
 */
@Composable
fun AppDrawerSheet(
    items: List<NavigationItem>,
    currentDestination: String,
    showSettings: Boolean,
    onItemSelect: (NavigationItem) -> Unit,
    onSubItemSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    ModalDrawerSheet {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Menu, contentDescription = "Close menu")
        }
        HorizontalDivider()
        items.forEach { item ->
            val isParentActive = !showSettings && (
                (item.subItems.isEmpty() && item.title == currentDestination) ||
                    item.subItems.contains(currentDestination)
            )
            NavigationDrawerItem(
                label = { Text(item.title) },
                icon = {
                    Icon(
                        imageVector = if (isParentActive) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title,
                    )
                },
                selected = isParentActive && item.subItems.isEmpty(),
                onClick = { onItemSelect(item) },
            )
            item.subItems.forEach { subItem ->
                NavigationDrawerItem(
                    label = { Text(subItem) },
                    selected = !showSettings && subItem == currentDestination,
                    onClick = { onSubItemSelect(subItem) },
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}

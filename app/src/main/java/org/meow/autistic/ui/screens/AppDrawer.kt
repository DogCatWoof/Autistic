package org.meow.autistic.ui.screens

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
import org.meow.autistic.NavigationItem

/**
 * Slide-in drawer content listing all main navigation destinations.
 *
 * @param items The full list of navigation destinations.
 * @param selectedIndex The currently active destination index.
 * @param showSettings Whether the Settings screen is currently shown (deselects all items).
 * @param onSelect Called with the chosen index when the user taps a drawer item.
 * @param onClose Called when the user taps the hamburger icon to close the drawer.
 */
@Composable
fun AppDrawerSheet(
    items: List<NavigationItem>,
    selectedIndex: Int,
    showSettings: Boolean,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit,
) {
    ModalDrawerSheet {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Menu, contentDescription = "Close menu")
        }
        HorizontalDivider()
        items.forEachIndexed { index, item ->
            val isSelected = !showSettings && index == selectedIndex
            NavigationDrawerItem(
                label = { Text(item.title) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title,
                    )
                },
                selected = isSelected,
                onClick = { onSelect(index) },
            )
        }
    }
}

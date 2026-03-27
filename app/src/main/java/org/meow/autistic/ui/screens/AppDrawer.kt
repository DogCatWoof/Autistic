package org.meow.autistic.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.meow.autistic.NavigationItem

/**
 * Slide-in drawer content listing all main navigation destinations.
 *
 * @param items The full list of navigation destinations.
 * @param selectedIndex The currently active destination index.
 * @param showSettings Whether the Settings screen is currently shown (deselects all items).
 * @param onSelect Called with the chosen index when the user taps a drawer item.
 */
@Composable
fun AppDrawerSheet(
    items: List<NavigationItem>,
    selectedIndex: Int,
    showSettings: Boolean,
    onSelect: (Int) -> Unit,
) {
    ModalDrawerSheet {
        Text(
            text = "Navigation",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
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

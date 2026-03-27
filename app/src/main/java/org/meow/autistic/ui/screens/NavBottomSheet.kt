package org.meow.autistic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.meow.autistic.NavigationItem

/**
 * Bottom sheet showing sub-navigation options for a [NavigationItem].
 * Tapping a sub-item calls [onSubItemSelected] then closes automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavBottomSheet(
    item: NavigationItem,
    onSubItemSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Text(
            text = item.title,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        HorizontalDivider()
        item.subItems.forEach { subItem ->
            ListItem(
                headlineContent = { Text(subItem) },
                modifier = Modifier
                    .clickable { onSubItemSelected(subItem) }
                    .semantics { contentDescription = "$subItem option" },
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

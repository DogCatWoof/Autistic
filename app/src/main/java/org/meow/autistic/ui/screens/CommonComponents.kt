package org.meow.autistic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Red delete background shown when swiping an item end-to-start. */
@Composable
fun DeleteSwipeBackground(contentDescription: String = "Delete") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            Icons.Default.Delete,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(end = 24.dp),
        )
    }
}

/**
 * Formats [instant] as a short display string.
 * Returns the time only (e.g. "3:45 PM") when the instant falls on today;
 * otherwise formats with [historicalPattern] (default "MMM d, h:mm a").
 */
fun formatInstantForDisplay(
    instant: Instant,
    historicalPattern: String = "MMM d, h:mm a",
): String {
    val zdt = instant.atZone(ZoneId.systemDefault())
    val today = LocalDate.now(ZoneId.systemDefault())
    return if (zdt.toLocalDate() == today) {
        DateTimeFormatter.ofPattern("h:mm a").format(zdt)
    } else {
        DateTimeFormatter.ofPattern(historicalPattern).format(zdt)
    }
}

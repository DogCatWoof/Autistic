package org.meow.autistic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.sqrt

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

/** Blocks [SwipeToDismissBox] from starting unless the gesture is predominantly horizontal. */
internal fun Modifier.requireHorizontalSwipe(slopeRatio: Float = 5f): Modifier =
    pointerInput(slopeRatio) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var cumX = 0f
            var cumY = 0f
            var isVertical: Boolean? = null
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                cumX += change.position.x - change.previousPosition.x
                cumY += change.position.y - change.previousPosition.y
                if (isVertical == null) {
                    val dist = sqrt(cumX * cumX + cumY * cumY)
                    if (dist > viewConfiguration.touchSlop) {
                        isVertical = abs(cumY) * slopeRatio > abs(cumX)
                    }
                }
                if (isVertical == true) {
                    change.consume()
                }
            }
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

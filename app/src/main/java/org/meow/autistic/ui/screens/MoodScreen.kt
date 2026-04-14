package org.meow.autistic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.meow.autistic.data.mood.MOOD_EMOJIS
import org.meow.autistic.data.mood.MoodEntity

private val moodColors = listOf(
    Color(0xFFE53935), // 1 – red
    Color(0xFFFF7043), // 2 – orange
    Color(0xFFFFB300), // 3 – amber
    Color(0xFF7CB342), // 4 – light green
    Color(0xFF2E7D32), // 5 – green
)

private val moodLabels = listOf("Very Low", "Low", "Neutral", "Good", "Great")

/**
 * Mood screen — shows a list of past mood readings in reverse-chronological order.
 * Swipe left to delete a reading.
 */
@Composable
fun MoodScreen(modifier: Modifier = Modifier, viewModel: MoodViewModel = koinViewModel()) {
    val moods by viewModel.moods.collectAsState()
    MoodListContent(moods = moods, onDelete = viewModel::delete, modifier = modifier)
}

@Composable
private fun MoodListContent(
    moods: List<MoodEntity>,
    onDelete: (MoodEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        if (moods.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No mood readings yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(moods, key = { it.id }) { mood ->
                    MoodListItem(mood = mood, onDelete = { onDelete(mood) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoodListItem(mood: MoodEntity, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete()
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = { DeleteSwipeBackground() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (mood.emoji.isNotBlank()) {
                    EmojiBadge(emoji = mood.emoji)
                } else {
                    MoodLevelBadge(level = mood.level)
                }
                Column(modifier = Modifier.weight(1f)) {
                    val displayLabel = when {
                        mood.emoji.isNotBlank() -> MOOD_EMOJIS.firstOrNull { it.first == mood.emoji }?.second ?: mood.emoji
                        else -> moodLabels.getOrElse(mood.level - 1) { "?" }
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = displayLabel,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        )
                        Text(
                            text = formatInstantForDisplay(mood.createdAt),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            ),
                        )
                    }
                    if (mood.activity.isNotBlank()) {
                        Text(
                            text = mood.activity,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (mood.notes.isNotBlank()) {
                        Text(
                            text = mood.notes,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun EmojiBadge(emoji: String) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun MoodLevelBadge(level: Int) {
    val color = moodColors.getOrElse(level - 1) { Color.Gray }
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = color, shape = MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = level.toString(),
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}


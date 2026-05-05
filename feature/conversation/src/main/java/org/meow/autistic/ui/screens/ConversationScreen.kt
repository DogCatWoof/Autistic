package org.meow.autistic.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.koin.androidx.compose.koinViewModel
import org.meow.autistic.data.conversation.ResponseTemplate
import org.meow.autistic.data.conversation.TONES

/** Push-to-talk conversation scaffolding screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConversationScreen(viewModel: ConversationViewModel = koinViewModel()) {
    val transcribedText by viewModel.transcribedText.collectAsState()
    val chips by viewModel.chips.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()
    val isSpeakerTurn by viewModel.isSpeakerTurn.collectAsState()
    val tone by viewModel.tone.collectAsState()
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) viewModel.startListening()
    }

    var showAlternatives by remember { mutableStateOf(false) }
    val primaryChips = chips.take(2)
    val alternativeChips = chips.drop(2)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TurnIndicatorBanner(isSpeakerTurn = isSpeakerTurn, isListening = isListening)

        TranscriptionCard(text = transcribedText, modifier = Modifier.fillMaxWidth())

        if (chips.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                primaryChips.forEach { chip ->
                    ResponseChipItem(chip = chip, onClick = { viewModel.selectChip(chip) })
                }
            }

            if (alternativeChips.isNotEmpty()) {
                TextButton(onClick = { showAlternatives = !showAlternatives }) {
                    Icon(
                        if (showAlternatives) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                    )
                    Text(if (showAlternatives) "Less" else "More options")
                }
                AnimatedVisibility(visible = showAlternatives) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        alternativeChips.forEach { chip ->
                            ResponseChipItem(chip = chip, onClick = { viewModel.selectChip(chip) })
                        }
                    }
                }
            }
        }

        ToneSelector(selected = tone, onSelect = { viewModel.setTone(it) })

        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 8.dp)) {
            HoldToTalkButton(
                isListening = isListening,
                onPressStart = {
                    if (hasPermission) viewModel.startListening()
                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onPressEnd = { viewModel.stopListening() },
            )
        }

        IconButton(onClick = { viewModel.toggleTts() }) {
            Icon(
                if (ttsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                contentDescription = if (ttsEnabled) "Disable TTS" else "Enable TTS",
            )
        }
    }
}

@Composable
private fun TurnIndicatorBanner(isSpeakerTurn: Boolean, isListening: Boolean) {
    val (label, color) = when {
        isListening -> "Listening…" to MaterialTheme.colorScheme.primary
        isSpeakerTurn -> "Your turn to respond" to MaterialTheme.colorScheme.tertiary
        else -> "Hold mic to capture speech" to MaterialTheme.colorScheme.outline
    }
    Text(label, style = MaterialTheme.typography.labelLarge, color = color)
}

@Composable
private fun TranscriptionCard(text: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            text = text.ifBlank { "Transcribed speech will appear here…" },
            style = MaterialTheme.typography.bodyMedium,
            color = if (text.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
        )
    }
}

@Composable
private fun ResponseChipItem(chip: ResponseTemplate, onClick: () -> Unit) {
    FilterChip(selected = false, onClick = onClick, label = { Text(chip.text) })
}

@Composable
private fun ToneSelector(selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TONES.forEach { tone ->
            FilterChip(
                selected = tone == selected,
                onClick = { onSelect(tone) },
                label = { Text(tone.replaceFirstChar { it.uppercase() }) },
            )
        }
    }
}

@Composable
private fun HoldToTalkButton(isListening: Boolean, onPressStart: () -> Unit, onPressEnd: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .size(88.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressStart()
                        tryAwaitRelease()
                        onPressEnd()
                    },
                )
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = if (isListening) "Listening" else "Hold to talk",
                tint = if (isListening) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

package org.meow.autistic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.meow.autistic.data.sequence.SequenceEntity

/**
 * Settings sub-screen for managing sequence definitions and viewing/controlling the active run.
 */
@Composable
fun SequenceListScreen(modifier: Modifier = Modifier) {
    val vm: SequenceViewModel = koinViewModel()
    val runVm: SequenceRunViewModel = koinViewModel()
    val sequences by vm.sequences.collectAsState()
    val runState by runVm.state.collectAsState()

    var showAddSequence by remember { mutableStateOf(false) }
    var expandedSequenceId by remember { mutableStateOf<Long?>(null) }
    var addingStepForSequenceId by remember { mutableStateOf<Long?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            if (runState is SequenceRunUiState.Active) {
                item { ActiveRunCard(state = runState as SequenceRunUiState.Active, runVm = runVm) }
                item { HorizontalDivider() }
            }
            item { SettingsSectionLabel("Sequences") }
            items(sequences, key = { it.id }) { sequence ->
                SequenceItem(
                    sequence = sequence,
                    expanded = expandedSequenceId == sequence.id,
                    hasActiveRun = runState is SequenceRunUiState.Active,
                    onExpand = {
                        expandedSequenceId = if (expandedSequenceId == sequence.id) null else sequence.id
                    },
                    onDelete = { vm.deleteSequence(sequence) },
                    onStartRun = { runVm.startRun(sequence.id) },
                    onAddStep = { addingStepForSequenceId = sequence.id },
                    vm = vm,
                )
            }
        }

        FloatingActionButton(
            onClick = { showAddSequence = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Default.Add, contentDescription = "Add sequence") }
    }

    if (showAddSequence) {
        NameInputDialog(
            title = "New Sequence",
            label = "Sequence name",
            onConfirm = { name -> vm.addSequence(name); showAddSequence = false },
            onDismiss = { showAddSequence = false },
        )
    }

    addingStepForSequenceId?.let { seqId ->
        StepInputDialog(
            onConfirm = { instruction, minutes ->
                vm.addStep(seqId, instruction, minutes)
                addingStepForSequenceId = null
            },
            onDismiss = { addingStepForSequenceId = null },
        )
    }
}

@Composable
private fun ActiveRunCard(state: SequenceRunUiState.Active, runVm: SequenceRunViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Active Run: ${state.sequence.name}")
        LinearProgressIndicator(progress = { state.progressFraction }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        state.currentStep?.let { step ->
            Text("Step ${state.steps.indexOf(step) + 1} of ${state.steps.size}: ${step.instruction}")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            state.currentStep?.let { step ->
                Button(onClick = { runVm.completeStep(state.run.id, step.id) }) { Text("Done") }
            }
            OutlinedButton(onClick = { runVm.endRun(state.run.id) }) { Text("End Run") }
        }
    }
}

@Composable
private fun SequenceItem(
    sequence: SequenceEntity,
    expanded: Boolean,
    hasActiveRun: Boolean,
    onExpand: () -> Unit,
    onDelete: () -> Unit,
    onStartRun: () -> Unit,
    onAddStep: () -> Unit,
    vm: SequenceViewModel,
) {
    val steps by vm.getSteps(sequence.id).collectAsState(initial = emptyList())
    Column {
        ListItem(
            headlineContent = { Text(sequence.name) },
            supportingContent = { Text("${steps.size} step${if (steps.size == 1) "" else "s"}") },
            trailingContent = {
                Row {
                    if (!hasActiveRun) {
                        TextButton(onClick = onStartRun) { Text("Start") }
                    }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                    IconButton(onClick = onExpand) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                        )
                    }
                }
            },
        )
        if (expanded) {
            steps.forEach { step ->
                ListItem(
                    headlineContent = { Text("${step.position + 1}. ${step.instruction}") },
                    supportingContent = step.estimatedMinutes?.let { { Text("~$it min") } },
                    trailingContent = {
                        IconButton(onClick = { vm.deleteStep(step) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete step")
                        }
                    },
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
            TextButton(onClick = onAddStep, modifier = Modifier.padding(start = 16.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add step")
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun NameInputDialog(title: String, label: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(label) }) },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun StepInputDialog(onConfirm: (String, Int?) -> Unit, onDismiss: () -> Unit) {
    var instruction by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Step") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = instruction, onValueChange = { instruction = it }, label = { Text("Instruction") })
                OutlinedTextField(value = minutes, onValueChange = { minutes = it }, label = { Text("Estimated minutes (optional)") })
            }
        },
        confirmButton = {
            TextButton(enabled = instruction.isNotBlank(), onClick = {
                onConfirm(instruction, minutes.toIntOrNull())
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

package org.meow.autistic.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.meow.autistic.data.sequence.SequenceEntity
import org.meow.autistic.data.sequence.SequenceRepository
import org.meow.autistic.data.sequence.SequenceRunEntity
import org.meow.autistic.data.sequence.SequenceRunNotificationManager
import org.meow.autistic.data.sequence.SequenceStepEntity

sealed class SequenceRunUiState {
    data object Idle : SequenceRunUiState()
    data class Active(
        val run: SequenceRunEntity,
        val sequence: SequenceEntity,
        val steps: List<SequenceStepEntity>,
        val completedStepIds: Set<Long>,
        val currentStep: SequenceStepEntity?,
        val progressFraction: Float,
    ) : SequenceRunUiState()
}

/**
 * Drives the active-run UI.
 * Observes the active run and its step progress, emitting [SequenceRunUiState].
 */
class SequenceRunViewModel(
    private val repository: SequenceRepository,
    application: Application,
) : AndroidViewModel(application) {

    val state: StateFlow<SequenceRunUiState> = repository.getActiveRun()
        .flatMapLatest { run ->
            if (run == null) {
                flowOf(SequenceRunUiState.Idle)
            } else {
                repository.getProgress(run.id).map { progress ->
                    val sequence = repository.getById(run.sequenceId)
                        ?: return@map SequenceRunUiState.Idle
                    val steps = repository.getStepsOnce(run.sequenceId)
                    val completedIds = progress.map { it.stepId }.toSet()
                    val currentStep = steps.firstOrNull { it.id !in completedIds }
                    val fraction = if (steps.isEmpty()) 0f else completedIds.size.toFloat() / steps.size
                    SequenceRunUiState.Active(run, sequence, steps, completedIds, currentStep, fraction)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SequenceRunUiState.Idle)

    fun startRun(sequenceId: Long) = viewModelScope.launch {
        val runId = repository.startRun(sequenceId)
        SequenceRunNotificationManager.update(getApplication(), runId)
    }

    fun completeStep(runId: Long, stepId: Long) = viewModelScope.launch {
        repository.completeStep(runId, stepId)
        val run = repository.getActiveRunOnce() ?: return@launch
        val steps = repository.getStepsOnce(run.sequenceId)
        val completed = repository.getProgressOnce(runId)
        if (completed.size >= steps.size) {
            repository.completeRun(runId)
            SequenceRunNotificationManager.cancel(getApplication())
        } else {
            SequenceRunNotificationManager.update(getApplication(), runId)
        }
    }

    fun endRun(runId: Long) = viewModelScope.launch {
        repository.completeRun(runId)
        SequenceRunNotificationManager.cancel(getApplication())
    }
}

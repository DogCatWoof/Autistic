package org.meow.autistic.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.meow.autistic.data.sequence.SequenceEntity
import org.meow.autistic.data.sequence.SequenceRepository
import org.meow.autistic.data.sequence.SequenceStepEntity

/** Manages sequence definitions (create, delete, add/remove steps). Used in Settings. */
class SequenceViewModel(private val repository: SequenceRepository) : ViewModel() {

    val sequences: StateFlow<List<SequenceEntity>> = repository.getAllSequences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getSteps(sequenceId: Long): Flow<List<SequenceStepEntity>> = repository.getSteps(sequenceId)

    fun addSequence(name: String) = viewModelScope.launch {
        repository.insertSequence(SequenceEntity(name = name.trim()))
    }

    fun deleteSequence(sequence: SequenceEntity) = viewModelScope.launch {
        repository.deleteSequence(sequence)
    }

    fun addStep(sequenceId: Long, instruction: String, estimatedMinutes: Int?) = viewModelScope.launch {
        val existing = repository.getStepsOnce(sequenceId)
        repository.insertStep(
            SequenceStepEntity(
                sequenceId = sequenceId,
                instruction = instruction.trim(),
                estimatedMinutes = estimatedMinutes,
                position = existing.size,
            ),
        )
    }

    fun deleteStep(step: SequenceStepEntity) = viewModelScope.launch {
        repository.deleteStep(step)
    }
}

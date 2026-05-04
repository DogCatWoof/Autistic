package org.meow.autistic.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import org.meow.autistic.data.sequence.SequenceEntity
import org.meow.autistic.data.sequence.SequenceRunEntity
import org.meow.autistic.data.sequence.SequenceStepEntity
import org.meow.autistic.data.task.DailyTaskEntity

// ── Sequence ──────────────────────────────────────────────────────────────────

/** Firestore document model for [SequenceEntity]. Document ID = [SequenceEntity.firestoreId]. */
data class SequenceDocument(
    val name: String,
    val isDeleted: Boolean,
    val lastModifiedAt: Timestamp,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "name" to name, "isDeleted" to isDeleted, "lastModifiedAt" to lastModifiedAt,
    )

    companion object {
        fun fromSnapshot(s: DocumentSnapshot) = SequenceDocument(
            name = s.getString("name") ?: "",
            isDeleted = s.getBoolean("isDeleted") ?: false,
            lastModifiedAt = s.getTimestamp("lastModifiedAt") ?: Timestamp.now(),
        )
    }
}

fun SequenceEntity.toDocument() = SequenceDocument(
    name = name, isDeleted = isDeleted, lastModifiedAt = lastModifiedAt.toFirestoreTimestamp(),
)

fun SequenceDocument.toEntity(firestoreId: String, localId: Long = 0) = SequenceEntity(
    id = localId, firestoreId = firestoreId, name = name,
    isDeleted = isDeleted, lastModifiedAt = lastModifiedAt.toInstant(),
    pendingFirestoreSync = false,
)

// ── SequenceStep ──────────────────────────────────────────────────────────────

/**
 * Firestore document model for [SequenceStepEntity].
 * Document ID = [SequenceStepEntity.firestoreId].
 */
data class SequenceStepDocument(
    val sequenceFirestoreId: String,
    val instruction: String,
    val estimatedMinutes: Int?,
    val position: Int,
    val isDeleted: Boolean,
    val lastModifiedAt: Timestamp,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "sequenceFirestoreId" to sequenceFirestoreId, "instruction" to instruction,
        "estimatedMinutes" to estimatedMinutes, "position" to position,
        "isDeleted" to isDeleted, "lastModifiedAt" to lastModifiedAt,
    )

    companion object {
        fun fromSnapshot(s: DocumentSnapshot) = SequenceStepDocument(
            sequenceFirestoreId = s.getString("sequenceFirestoreId") ?: "",
            instruction = s.getString("instruction") ?: "",
            estimatedMinutes = s.getLong("estimatedMinutes")?.toInt(),
            position = s.getLong("position")?.toInt() ?: 0,
            isDeleted = s.getBoolean("isDeleted") ?: false,
            lastModifiedAt = s.getTimestamp("lastModifiedAt") ?: Timestamp.now(),
        )
    }
}

/**
 * @param sequenceFirestoreId The [SequenceEntity.firestoreId] of the parent sequence.
 */
fun SequenceStepEntity.toDocument(sequenceFirestoreId: String) = SequenceStepDocument(
    sequenceFirestoreId = sequenceFirestoreId, instruction = instruction,
    estimatedMinutes = estimatedMinutes, position = position,
    isDeleted = isDeleted, lastModifiedAt = lastModifiedAt.toFirestoreTimestamp(),
)

fun SequenceStepDocument.toEntity(
    firestoreId: String,
    sequenceLocalId: Long,
    localId: Long = 0,
) = SequenceStepEntity(
    id = localId, firestoreId = firestoreId, sequenceId = sequenceLocalId,
    instruction = instruction, estimatedMinutes = estimatedMinutes, position = position,
    isDeleted = isDeleted, lastModifiedAt = lastModifiedAt.toInstant(),
    pendingFirestoreSync = false,
)

// ── SequenceRun ───────────────────────────────────────────────────────────────

/**
 * Firestore document model for [SequenceRunEntity].
 * Document ID = [SequenceRunEntity.firestoreId].
 */
data class SequenceRunDocument(
    val sequenceFirestoreId: String,
    val startedAt: Timestamp,
    val completedAt: Timestamp?,
    val lastModifiedAt: Timestamp,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "sequenceFirestoreId" to sequenceFirestoreId,
        "startedAt" to startedAt, "completedAt" to completedAt,
        "lastModifiedAt" to lastModifiedAt,
    )

    companion object {
        fun fromSnapshot(s: DocumentSnapshot) = SequenceRunDocument(
            sequenceFirestoreId = s.getString("sequenceFirestoreId") ?: "",
            startedAt = s.getTimestamp("startedAt") ?: Timestamp.now(),
            completedAt = s.getTimestamp("completedAt"),
            lastModifiedAt = s.getTimestamp("lastModifiedAt") ?: Timestamp.now(),
        )
    }
}

fun SequenceRunEntity.toDocument(sequenceFirestoreId: String) = SequenceRunDocument(
    sequenceFirestoreId = sequenceFirestoreId,
    startedAt = startedAt.toFirestoreTimestamp(),
    completedAt = completedAt?.toFirestoreTimestamp(),
    lastModifiedAt = lastModifiedAt.toFirestoreTimestamp(),
)

fun SequenceRunDocument.toEntity(
    firestoreId: String,
    sequenceLocalId: Long,
    localId: Long = 0,
) = SequenceRunEntity(
    id = localId, firestoreId = firestoreId, sequenceId = sequenceLocalId,
    startedAt = startedAt.toInstant(), completedAt = completedAt?.toInstant(),
    lastModifiedAt = lastModifiedAt.toInstant(), pendingFirestoreSync = false,
)

// ── DailyTask ─────────────────────────────────────────────────────────────────

/** Firestore document model for [DailyTaskEntity]. Document ID = [DailyTaskEntity.firestoreId]. */
data class DailyTaskDocument(
    val title: String,
    val category: String,
    val timeMinutes: Int?,
    val expectedTimeMinutes: Int?,
    val isRequired: Boolean,
    val lastModifiedAt: Timestamp,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "title" to title, "category" to category, "timeMinutes" to timeMinutes,
        "expectedTimeMinutes" to expectedTimeMinutes, "isRequired" to isRequired,
        "lastModifiedAt" to lastModifiedAt,
    )

    companion object {
        fun fromSnapshot(s: DocumentSnapshot) = DailyTaskDocument(
            title = s.getString("title") ?: "",
            category = s.getString("category") ?: "General",
            timeMinutes = s.getLong("timeMinutes")?.toInt(),
            expectedTimeMinutes = s.getLong("expectedTimeMinutes")?.toInt(),
            isRequired = s.getBoolean("isRequired") ?: false,
            lastModifiedAt = s.getTimestamp("lastModifiedAt") ?: Timestamp.now(),
        )
    }
}

fun DailyTaskEntity.toDocument() = DailyTaskDocument(
    title = title, category = category, timeMinutes = timeMinutes,
    expectedTimeMinutes = expectedTimeMinutes, isRequired = isRequired,
    lastModifiedAt = lastModifiedAt.toFirestoreTimestamp(),
)

fun DailyTaskDocument.toEntity(firestoreId: String, localId: Long = 0) = DailyTaskEntity(
    id = localId, firestoreId = firestoreId, title = title, category = category,
    timeMinutes = timeMinutes, expectedTimeMinutes = expectedTimeMinutes,
    isRequired = isRequired, lastModifiedAt = lastModifiedAt.toInstant(),
    pendingFirestoreSync = false,
)

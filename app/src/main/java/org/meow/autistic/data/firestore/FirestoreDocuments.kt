package org.meow.autistic.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import org.meow.autistic.data.mood.MoodEntity
import org.meow.autistic.data.note.NoteEntity
import org.meow.autistic.data.task.TaskEntity
import java.time.Instant

// ── helpers ──────────────────────────────────────────────────────────────────

internal fun Instant.toFirestoreTimestamp() = Timestamp(epochSecond, nano)
internal fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())

// ── Task ─────────────────────────────────────────────────────────────────────

/**
 * Firestore document model for [TaskEntity].
 * Document ID = [TaskEntity.firestoreId].
 */
data class TaskDocument(
    val task: String,
    val createdAt: Timestamp,
    val dueAt: Timestamp?,
    val notes: String?,
    val category: String,
    val googleTaskId: String?,
    val dailyTaskId: Long?,
    val expectedTimeMinutes: Int?,
    val reminderMinutesBefore: Int?,
    val isImportant: Boolean,
    val completedAt: Timestamp?,
    val lastModifiedAt: Timestamp,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "task" to task, "createdAt" to createdAt, "dueAt" to dueAt,
        "notes" to notes, "category" to category, "googleTaskId" to googleTaskId,
        "dailyTaskId" to dailyTaskId, "expectedTimeMinutes" to expectedTimeMinutes,
        "reminderMinutesBefore" to reminderMinutesBefore, "isImportant" to isImportant,
        "completedAt" to completedAt, "lastModifiedAt" to lastModifiedAt,
    )

    companion object {
        fun fromSnapshot(s: DocumentSnapshot) = TaskDocument(
            task = s.getString("task") ?: "",
            createdAt = s.getTimestamp("createdAt") ?: Timestamp.now(),
            dueAt = s.getTimestamp("dueAt"),
            notes = s.getString("notes"),
            category = s.getString("category") ?: "General",
            googleTaskId = s.getString("googleTaskId"),
            dailyTaskId = s.getLong("dailyTaskId"),
            expectedTimeMinutes = s.getLong("expectedTimeMinutes")?.toInt(),
            reminderMinutesBefore = s.getLong("reminderMinutesBefore")?.toInt(),
            isImportant = s.getBoolean("isImportant") ?: false,
            completedAt = s.getTimestamp("completedAt"),
            lastModifiedAt = s.getTimestamp("lastModifiedAt") ?: Timestamp.now(),
        )
    }
}

fun TaskEntity.toDocument() = TaskDocument(
    task = task, createdAt = createdAt.toFirestoreTimestamp(),
    dueAt = dueAt?.toFirestoreTimestamp(), notes = notes, category = category,
    googleTaskId = googleTaskId, dailyTaskId = dailyTaskId,
    expectedTimeMinutes = expectedTimeMinutes, reminderMinutesBefore = reminderMinutesBefore,
    isImportant = isImportant, completedAt = completedAt?.toFirestoreTimestamp(),
    lastModifiedAt = lastModifiedAt.toFirestoreTimestamp(),
)

fun TaskDocument.toEntity(firestoreId: String, localId: Long = 0) = TaskEntity(
    id = localId, firestoreId = firestoreId, task = task,
    createdAt = createdAt.toInstant(), dueAt = dueAt?.toInstant(),
    notes = notes, category = category, googleTaskId = googleTaskId,
    dailyTaskId = dailyTaskId, expectedTimeMinutes = expectedTimeMinutes,
    reminderMinutesBefore = reminderMinutesBefore, isImportant = isImportant,
    completedAt = completedAt?.toInstant(), lastModifiedAt = lastModifiedAt.toInstant(),
    pendingFirestoreSync = false,
)

// ── Note ─────────────────────────────────────────────────────────────────────

/** Firestore document model for [NoteEntity]. Document ID = [NoteEntity.firestoreId]. */
data class NoteDocument(
    val title: String,
    val content: String,
    val createdAt: Timestamp,
    val isDeleted: Boolean,
    val lastModifiedAt: Timestamp,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "title" to title, "content" to content, "createdAt" to createdAt,
        "isDeleted" to isDeleted, "lastModifiedAt" to lastModifiedAt,
    )

    companion object {
        fun fromSnapshot(s: DocumentSnapshot) = NoteDocument(
            title = s.getString("title") ?: "",
            content = s.getString("content") ?: "",
            createdAt = s.getTimestamp("createdAt") ?: Timestamp.now(),
            isDeleted = s.getBoolean("isDeleted") ?: false,
            lastModifiedAt = s.getTimestamp("lastModifiedAt") ?: Timestamp.now(),
        )
    }
}

fun NoteEntity.toDocument() = NoteDocument(
    title = title, content = content,
    createdAt = createdAt.toFirestoreTimestamp(), isDeleted = isDeleted,
    lastModifiedAt = updatedAt.toFirestoreTimestamp(),
)

fun NoteDocument.toEntity(firestoreId: String, localId: Int = 0) = NoteEntity(
    id = localId, firestoreId = firestoreId, title = title, content = content,
    createdAt = createdAt.toInstant(), updatedAt = lastModifiedAt.toInstant(),
    isDeleted = isDeleted, pendingFirestoreSync = false,
)

// ── Mood ─────────────────────────────────────────────────────────────────────

/** Firestore document model for [MoodEntity]. Document ID = [MoodEntity.firestoreId]. */
data class MoodDocument(
    val level: Int,
    val emoji: String,
    val activity: String,
    val notes: String,
    val createdAt: Timestamp,
    val isDeleted: Boolean,
    val lastModifiedAt: Timestamp,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "level" to level, "emoji" to emoji, "activity" to activity, "notes" to notes,
        "createdAt" to createdAt, "isDeleted" to isDeleted, "lastModifiedAt" to lastModifiedAt,
    )

    companion object {
        fun fromSnapshot(s: DocumentSnapshot) = MoodDocument(
            level = s.getLong("level")?.toInt() ?: 0,
            emoji = s.getString("emoji") ?: "",
            activity = s.getString("activity") ?: "",
            notes = s.getString("notes") ?: "",
            createdAt = s.getTimestamp("createdAt") ?: Timestamp.now(),
            isDeleted = s.getBoolean("isDeleted") ?: false,
            lastModifiedAt = s.getTimestamp("lastModifiedAt") ?: Timestamp.now(),
        )
    }
}

fun MoodEntity.toDocument() = MoodDocument(
    level = level, emoji = emoji, activity = activity, notes = notes,
    createdAt = createdAt.toFirestoreTimestamp(), isDeleted = isDeleted,
    lastModifiedAt = lastModifiedAt.toFirestoreTimestamp(),
)

fun MoodDocument.toEntity(firestoreId: String, localId: Int = 0) = MoodEntity(
    id = localId, firestoreId = firestoreId, level = level, emoji = emoji,
    activity = activity, notes = notes, createdAt = createdAt.toInstant(),
    isDeleted = isDeleted, lastModifiedAt = lastModifiedAt.toInstant(),
    pendingFirestoreSync = false,
)

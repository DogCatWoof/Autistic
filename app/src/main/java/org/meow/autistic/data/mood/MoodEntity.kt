package org.meow.autistic.data.mood

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** Emoji options available in the notification mood picker. */
val MOOD_EMOJIS: List<Pair<String, String>> = listOf(
    "😊" to "Happy",
    "😢" to "Sad",
    "😠" to "Angry",
    "😌" to "Calm",
    "😴" to "Tired",
    "😑" to "Bored",
    "🥺" to "Lonely",
    "😜" to "Playful",
    "😇" to "Content",
    "🥰" to "Loved",
    "🤒" to "Sick",
    "😓" to "Stress",
    "😰" to "Anxious",
    "🤯" to "Too much",
)

/** A single mood reading recorded by the user. */
@Entity(tableName = "moods")
data class MoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val level: Int = 0,
    val emoji: String = "",
    val activity: String = "",
    val notes: String = "",
    val createdAt: Instant,
)

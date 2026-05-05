package org.meow.autistic.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import org.meow.autistic.data.foodlog.FoodLogItemEntry

/**
 * Firestore document model for [FoodLogItemEntry].
 * Document ID = [FoodLogItemEntry.firestoreId].
 */
data class FoodLogItemDocument(
    val date: String,
    val loggedAt: Timestamp,
    val description: String?,
    val calories: Double,
    val protein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
    val fiber: Double,
    val totalSugars: Double,
    val addedSugars: Double,
    val sugarAlcohols: Double,
    val isDeleted: Boolean,
    val lastModifiedAt: Timestamp,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "date" to date, "loggedAt" to loggedAt, "description" to description,
        "calories" to calories, "protein" to protein, "totalFat" to totalFat,
        "totalCarbs" to totalCarbs, "fiber" to fiber, "totalSugars" to totalSugars,
        "addedSugars" to addedSugars, "sugarAlcohols" to sugarAlcohols,
        "isDeleted" to isDeleted, "lastModifiedAt" to lastModifiedAt,
    )

    companion object {
        fun fromSnapshot(s: DocumentSnapshot) = FoodLogItemDocument(
            date = s.getString("date") ?: "",
            loggedAt = s.getTimestamp("loggedAt") ?: Timestamp.now(),
            description = s.getString("description"),
            calories = s.getDouble("calories") ?: 0.0,
            protein = s.getDouble("protein") ?: 0.0,
            totalFat = s.getDouble("totalFat") ?: 0.0,
            totalCarbs = s.getDouble("totalCarbs") ?: 0.0,
            fiber = s.getDouble("fiber") ?: 0.0,
            totalSugars = s.getDouble("totalSugars") ?: 0.0,
            addedSugars = s.getDouble("addedSugars") ?: 0.0,
            sugarAlcohols = s.getDouble("sugarAlcohols") ?: 0.0,
            isDeleted = s.getBoolean("isDeleted") ?: false,
            lastModifiedAt = s.getTimestamp("lastModifiedAt") ?: Timestamp.now(),
        )
    }
}

fun FoodLogItemEntry.toDocument() = FoodLogItemDocument(
    date = date, loggedAt = loggedAt.toFirestoreTimestamp(), description = description,
    calories = calories, protein = protein, totalFat = totalFat, totalCarbs = totalCarbs,
    fiber = fiber, totalSugars = totalSugars, addedSugars = addedSugars,
    sugarAlcohols = sugarAlcohols, isDeleted = isDeleted,
    lastModifiedAt = lastModifiedAt.toFirestoreTimestamp(),
)

fun FoodLogItemDocument.toEntry(firestoreId: String, localId: Long = 0) = FoodLogItemEntry(
    id = localId, firestoreId = firestoreId, date = date,
    loggedAt = loggedAt.toInstant(), description = description,
    calories = calories, protein = protein, totalFat = totalFat, totalCarbs = totalCarbs,
    fiber = fiber, totalSugars = totalSugars, addedSugars = addedSugars,
    sugarAlcohols = sugarAlcohols, isDeleted = isDeleted,
    lastModifiedAt = lastModifiedAt.toInstant(), pendingFirestoreSync = false,
)

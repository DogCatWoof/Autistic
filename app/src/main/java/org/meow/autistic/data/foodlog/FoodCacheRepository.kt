package org.meow.autistic.data.foodlog

import org.meow.autistic.data.photo.ParsedNutritionData

/** Cache of per-serving nutrition values keyed by food description. */
class FoodCacheRepository(private val dao: FoodCacheDao) {

    suspend fun findByDescription(description: String): ParsedNutritionData? {
        val entity = dao.findByDescription(description) ?: return null
        return ParsedNutritionData(
            description = entity.description,
            calories = entity.calories,
            protein = entity.protein,
            totalFat = entity.totalFat,
            totalCarbs = entity.totalCarbs,
            fiber = entity.fiber,
            totalSugars = entity.totalSugars,
            addedSugars = entity.addedSugars,
            sugarAlcohols = entity.sugarAlcohols,
        )
    }

    suspend fun save(data: ParsedNutritionData) {
        val description = data.description ?: return
        dao.upsert(FoodCacheEntity(
            description = description,
            calories = data.calories,
            protein = data.protein,
            totalFat = data.totalFat,
            totalCarbs = data.totalCarbs,
            fiber = data.fiber,
            totalSugars = data.totalSugars,
            addedSugars = data.addedSugars,
            sugarAlcohols = data.sugarAlcohols,
        ))
    }
}

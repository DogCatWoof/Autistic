package org.meow.autistic.data.product

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for a barcode → product mapping sourced from Open Food Facts.
 * Only products with a non-blank [name] are persisted.
 * [nutriments] is stored as a JSON blob via [NutrimentsConverter].
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val barcode: String,
    val name: String,
    val brands: String?,
    val quantity: String?,
    val servingsPerContainer: String?,
    val ingredients: String?,
    val foodGroups: String?,
    val nutriments: Nutriments?,
)

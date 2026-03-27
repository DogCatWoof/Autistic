package org.meow.autistic.data.product

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity storing a barcode → JSON product mapping sourced from Open Food Facts.
 * The [productJson] field contains the raw CSV row serialised as a key/value map.
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val barcode: String,
    val productJson: String,
)

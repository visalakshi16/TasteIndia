package com.example.tasteindia.domain

/** Lightweight meal used in lists and favourites (from filter.php or the favourites table). */
data class MealSummary(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
)

data class IngredientLine(
    val name: String,
    val measure: String?,
)

/** Full, normalised recipe (from lookup.php). Blank fields are already null/empty. */
data class MealDetail(
    val id: String,
    val name: String,
    val alternateName: String?,
    val category: String?,
    val area: String?,
    val thumbnailUrl: String?,
    val instructions: List<String>,
    val tags: List<String>,
    val ingredients: List<IngredientLine>,
    val sourceUrl: String?,
    val videoUrl: String?,
    val imageSourceUrl: String?,
) {
    fun toSummary() = MealSummary(id, name, thumbnailUrl)
}

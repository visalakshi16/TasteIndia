package com.example.tasteindia.data.remote

import com.example.tasteindia.domain.IngredientLine
import com.example.tasteindia.domain.MealDetail
import com.example.tasteindia.domain.MealSummary
import java.net.URI
import java.net.URISyntaxException

private const val MAX_INGREDIENTS = 20

internal fun Map<String, String?>.clean(key: String): String? =
    this[key]?.trim()?.takeIf { it.isNotEmpty() }

fun MealSummaryDto.toDomain(): MealSummary? {
    val title = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return MealSummary(
        id = id.trim(),
        name = title,
        thumbnailUrl = validHttpUrlOrNull(thumbnail),
    )
}

/** Returns the trimmed URL only if it is a well-formed http(s) URL, otherwise null. */
fun validHttpUrlOrNull(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return null
    val uri = try {
        URI(value)
    } catch (e: URISyntaxException) {
        return null
    }
    val scheme = uri.scheme?.lowercase()
    return if ((scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()) value else null
}

internal fun splitParagraphs(text: String): List<String> =
    text.split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }

/**
 * Normalises one lookup.php record. Returns null if it has no id/name.
 * Ingredient/measure pairs with a blank ingredient are dropped; blank measures become null.
 */
fun Map<String, String?>.toMealDetail(): MealDetail? {
    val id = clean("idMeal") ?: return null
    val name = clean("strMeal") ?: return null

    val ingredients = (1..MAX_INGREDIENTS).mapNotNull { i ->
        val ingredient = clean("strIngredient$i") ?: return@mapNotNull null
        IngredientLine(name = ingredient, measure = clean("strMeasure$i"))
    }

    // Intentionally not shown: strCreativeCommonsConfirmed, dateModified (internal metadata).
    return MealDetail(
        id = id,
        name = name,
        alternateName = clean("strMealAlternate"),
        category = clean("strCategory"),
        area = clean("strArea"),
        thumbnailUrl = validHttpUrlOrNull(this["strMealThumb"]),
        instructions = clean("strInstructions")?.let(::splitParagraphs).orEmpty(),
        tags = clean("strTags")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
        ingredients = ingredients,
        sourceUrl = validHttpUrlOrNull(this["strSource"]),
        videoUrl = validHttpUrlOrNull(this["strYoutube"]),
        imageSourceUrl = validHttpUrlOrNull(this["strImageSource"]),
    )
}

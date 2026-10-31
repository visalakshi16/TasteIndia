package com.example.tasteindia.ui.recipes

import com.example.tasteindia.domain.MealSummary

enum class SortOrder { NameAscending, NameDescending }

/** Everything the user has chosen on the Recipes screen. Persisted via SavedStateHandle. */
data class FilterState(
    val query: String = "",
    val category: String? = null,
    val ingredient: String? = null,
    val favouritesOnly: Boolean = false,
    val sort: SortOrder = SortOrder.NameAscending,
) {
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || category != null || ingredient != null || favouritesOnly

    /** Filters configured in the sheet (the search box is shown separately). */
    val sheetFilterCount: Int
        get() = listOf(category != null, ingredient != null, favouritesOnly).count { it }
}

/**
 * Pure filter + sort. [base] is always the Indian set, so category/ingredient ID sets
 * (which may contain non-Indian meals) can only ever *narrow* it: the Indian boundary is hard.
 * A null id set means "that filter is not active".
 */
fun filterAndSort(
    base: List<MealSummary>,
    filters: FilterState,
    categoryIds: Set<String>?,
    ingredientIds: Set<String>?,
    favouriteIds: Set<String>,
): List<MealSummary> {
    val query = filters.query.trim()
    val filtered = base.filter { meal ->
        (query.isEmpty() || meal.name.contains(query, ignoreCase = true)) &&
            (categoryIds == null || meal.id in categoryIds) &&
            (ingredientIds == null || meal.id in ingredientIds) &&
            (!filters.favouritesOnly || meal.id in favouriteIds)
    }
    val byName = compareBy<MealSummary, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
    return when (filters.sort) {
        SortOrder.NameAscending -> filtered.sortedWith(byName)
        SortOrder.NameDescending -> filtered.sortedWith(byName.reversed())
    }
}

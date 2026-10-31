package com.example.tasteindia

import com.example.tasteindia.domain.MealSummary
import com.example.tasteindia.ui.recipes.FilterState
import com.example.tasteindia.ui.recipes.SortOrder
import com.example.tasteindia.ui.recipes.filterAndSort
import org.junit.Assert.assertEquals
import org.junit.Test

/** Protects against: category/ingredient results leaking non-Indian meals into the list. */
class FilterLogicTest {

    private val base = listOf(
        MealSummary("1", "Butter Chicken", null),
        MealSummary("2", "dal Tadka", null),
        MealSummary("3", "Aloo Gobi", null),
    )

    private fun ids(list: List<MealSummary>) = list.map { it.id }

    @Test
    fun `category and ingredient intersect and never add meals outside the Indian base set`() {
        val result = filterAndSort(
            base = base,
            filters = FilterState(category = "Chicken", ingredient = "onion"),
            categoryIds = setOf("1", "2", "999"),     // 999 is not Indian
            ingredientIds = setOf("1", "3", "888"),   // 888 is not Indian
            favouriteIds = emptySet(),
        )
        assertEquals(listOf("1"), ids(result))
    }

    @Test
    fun `null id sets mean the filter is inactive`() {
        assertEquals(3, filterAndSort(base, FilterState(), null, null, emptySet()).size)
    }

    @Test
    fun `search is case-insensitive and trimmed`() {
        val result = filterAndSort(base, FilterState(query = "  DAL "), null, null, emptySet())
        assertEquals(listOf("2"), ids(result))
    }

    @Test
    fun `favourites only keeps just favourites`() {
        val result = filterAndSort(base, FilterState(favouritesOnly = true), null, null, setOf("3"))
        assertEquals(listOf("3"), ids(result))
    }

    @Test
    fun `sorting is case-insensitive in both directions`() {
        val asc = filterAndSort(base, FilterState(sort = SortOrder.NameAscending), null, null, emptySet())
        val desc = filterAndSort(base, FilterState(sort = SortOrder.NameDescending), null, null, emptySet())
        assertEquals(listOf("3", "1", "2"), ids(asc))
        assertEquals(listOf("2", "1", "3"), ids(desc))
    }
}

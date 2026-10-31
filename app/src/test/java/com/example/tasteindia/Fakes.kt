package com.example.tasteindia

import com.example.tasteindia.data.FavoritesRepository
import com.example.tasteindia.data.MealNotFoundException
import com.example.tasteindia.data.MealRepository
import com.example.tasteindia.domain.MealDetail
import com.example.tasteindia.domain.MealSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException

/** Deterministic stand-in for TheMealDB: no network, optional per-ingredient delays. */
class FakeMealRepository(
    private val indianMeals: List<MealSummary> = emptyList(),
    private val categories: List<String> = emptyList(),
    private val categoryIds: Map<String, Set<String>> = emptyMap(),
    private val ingredientIds: Map<String, Set<String>> = emptyMap(),
    private val ingredientDelaysMs: Map<String, Long> = emptyMap(),
    private val details: Map<String, MealDetail> = emptyMap(),
    var failIndianMeals: Boolean = false,
) : MealRepository {

    override suspend fun getIndianMeals(forceRefresh: Boolean): List<MealSummary> {
        if (failIndianMeals) throw IOException("offline")
        return indianMeals
    }

    override suspend fun getCategories() = categories
    override suspend fun getIngredientNames() = ingredientIds.keys.sorted()
    override suspend fun mealIdsForCategory(category: String) = categoryIds[category].orEmpty()

    override suspend fun mealIdsForIngredient(ingredient: String): Set<String> {
        delay(ingredientDelaysMs[ingredient] ?: 0L)
        return ingredientIds[ingredient].orEmpty()
    }

    override suspend fun getMeal(id: String): MealDetail = details[id] ?: throw MealNotFoundException(id)
}

class FakeFavouritesRepository(initial: List<MealSummary> = emptyList()) : FavoritesRepository {
    private val state = MutableStateFlow(initial)
    override val favourites: Flow<List<MealSummary>> = state

    override suspend fun toggle(meal: MealSummary) {
        state.update { list ->
            if (list.any { it.id == meal.id }) list.filterNot { it.id == meal.id } else list + meal
        }
    }

    override suspend fun remove(id: String) {
        state.update { list -> list.filterNot { it.id == id } }
    }
}

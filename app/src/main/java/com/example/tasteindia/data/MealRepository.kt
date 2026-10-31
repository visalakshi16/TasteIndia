package com.example.tasteindia.data

import com.example.tasteindia.data.remote.MealDbApi
import com.example.tasteindia.data.remote.MealSummaryResponse
import com.example.tasteindia.data.remote.toDomain
import com.example.tasteindia.data.remote.toMealDetail
import com.example.tasteindia.domain.MealDetail
import com.example.tasteindia.domain.MealSummary
import com.squareup.moshi.JsonDataException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

interface MealRepository {
    /** Authoritative Indian base set (filter.php?a=Indian). */
    suspend fun getIndianMeals(forceRefresh: Boolean = false): List<MealSummary>
    suspend fun getCategories(): List<String>
    suspend fun getIngredientNames(): List<String>
    suspend fun mealIdsForCategory(category: String): Set<String>
    suspend fun mealIdsForIngredient(ingredient: String): Set<String>
    suspend fun getMeal(id: String): MealDetail
}

/**
 * In-memory cache policy: lists/ID sets are cached for the process lifetime (refreshed only
 * on explicit retry). Detail lookups are cached per id, de-duplicated while in flight, and
 * bounded by a semaphore so at most [maxConcurrentDetailRequests] run at once.
 */
class DefaultMealRepository(
    private val api: MealDbApi,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    maxConcurrentDetailRequests: Int = 4,
) : MealRepository {

    private val detailPermits = Semaphore(maxConcurrentDetailRequests)
    private val lock = Any()

    @Volatile private var indianMeals: List<MealSummary>? = null
    @Volatile private var categories: List<String>? = null
    @Volatile private var ingredientNames: List<String>? = null
    private val categoryIds = ConcurrentHashMap<String, Set<String>>()
    private val ingredientIds = ConcurrentHashMap<String, Set<String>>()
    private val detailCache = HashMap<String, MealDetail>()
    private val inFlight = HashMap<String, Deferred<MealDetail>>()

    override suspend fun getIndianMeals(forceRefresh: Boolean): List<MealSummary> {
        if (!forceRefresh) indianMeals?.let { return it }
        val fetched = api.filterByArea(INDIAN_AREA).meals.orEmpty()
            .mapNotNull { it.toDomain() }
            .distinctBy { it.id }
        indianMeals = fetched
        return fetched
    }

    override suspend fun getCategories(): List<String> {
        categories?.let { return it }
        val fetched = api.listCategories().meals.orEmpty()
            .mapNotNull { it.name?.trim()?.takeIf { n -> n.isNotEmpty() } }
            .sorted()
        categories = fetched
        return fetched
    }

    override suspend fun getIngredientNames(): List<String> {
        ingredientNames?.let { return it }
        val fetched = api.listIngredients().meals.orEmpty()
            .mapNotNull { it.name?.trim()?.takeIf { n -> n.isNotEmpty() } }
            .sortedBy { it.lowercase() }
        ingredientNames = fetched
        return fetched
    }

    override suspend fun mealIdsForCategory(category: String): Set<String> {
        categoryIds[category]?.let { return it }
        val ids = idsFrom { api.filterByCategory(category) }
        categoryIds[category] = ids
        return ids
    }

    override suspend fun mealIdsForIngredient(ingredient: String): Set<String> {
        // The API expects lower-case words joined by underscores, e.g. chicken_breast.
        val key = ingredient.trim().lowercase().replace(Regex("\\s+"), "_")
        ingredientIds[key]?.let { return it }
        val ids = idsFrom { api.filterByIngredient(key) }
        ingredientIds[key] = ids
        return ids
    }

    override suspend fun getMeal(id: String): MealDetail {
        val deferred = synchronized(lock) {
            detailCache[id]?.let { return it }
            inFlight.getOrPut(id) {
                scope.async {
                    try {
                        val detail = detailPermits.withPermit { fetchMeal(id) }
                        synchronized(lock) { detailCache[id] = detail }
                        detail
                    } finally {
                        synchronized(lock) { inFlight.remove(id) }
                    }
                }
            }
        }
        return deferred.await()
    }

    private suspend fun fetchMeal(id: String): MealDetail =
        api.lookupMeal(id).meals?.firstOrNull()?.toMealDetail() ?: throw MealNotFoundException(id)

    /** TheMealDB may answer an empty filter with "meals":"None Available" (a string); treat as empty. */
    private suspend fun idsFrom(call: suspend () -> MealSummaryResponse): Set<String> =
        try {
            call().meals.orEmpty().mapTo(HashSet()) { it.id.trim() }
        } catch (e: JsonDataException) {
            emptySet()
        }

    private companion object {
        const val INDIAN_AREA = "India"
    }
}

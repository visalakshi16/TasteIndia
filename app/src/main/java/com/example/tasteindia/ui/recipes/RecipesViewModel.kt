package com.example.tasteindia.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasteindia.data.FavoritesRepository
import com.example.tasteindia.data.MealRepository
import com.example.tasteindia.data.runCatchingCancellable
import com.example.tasteindia.data.toUserMessage
import com.example.tasteindia.domain.MealSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoadStatus { Loading, Error, Ready }

data class RecipeItem(
    val meal: MealSummary,
    val isFavourite: Boolean,
    /** Only set when a category filter proved membership; never guessed. */
    val category: String?,
)

data class RecipesUiState(
    val status: LoadStatus = LoadStatus.Loading,
    val errorMessage: String? = null,
    val filters: FilterState = FilterState(),
    val categories: List<String> = emptyList(),
    val items: List<RecipeItem> = emptyList(),
    val totalCount: Int = 0,
    val isApplyingFilters: Boolean = false,
    val filterError: String? = null,
)

/**
 * Source of truth for search text, category, ingredient, favourites-only and sort order.
 * The filters live in [SavedStateHandle], so they survive navigation, rotation and process death.
 */
class RecipesViewModel(
    private val meals: MealRepository,
    private val favourites: FavoritesRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private sealed interface BaseState {
        data object Loading : BaseState
        data class Loaded(val meals: List<MealSummary>) : BaseState
        data class Failed(val message: String) : BaseState
    }

    /** ID sets fetched for the active category/ingredient filter, tagged with the filter they belong to. */
    private data class RemoteSets(
        val category: String? = null,
        val ingredient: String? = null,
        val categoryIds: Set<String>? = null,
        val ingredientIds: Set<String>? = null,
        val loading: Boolean = false,
        val error: String? = null,
    )

    private val base = MutableStateFlow<BaseState>(BaseState.Loading)
    private val categories = MutableStateFlow<List<String>>(emptyList())
    private val filters = MutableStateFlow(restoreFilters())
    private val retryTick = MutableStateFlow(0)
    private var baseJob: Job? = null

    /** Synchronous view of the filters (used by the search TextField so typing never lags). */
    val currentFilters: StateFlow<FilterState> = filters.asStateFlow()

    private val _ingredientNames = MutableStateFlow<List<String>>(emptyList())
    val ingredientNames: StateFlow<List<String>> = _ingredientNames.asStateFlow()

    private val favouriteIds: Flow<Set<String>> =
        favourites.favourites.map { list -> list.map { it.id }.toSet() }

    /**
     * Re-runs only when the category/ingredient selection changes. transformLatest cancels the
     * previous lookup, so a slow stale response can never overwrite a newer one.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val remoteSets: Flow<RemoteSets> =
        combine(
            filters.map { it.category to it.ingredient }.distinctUntilChanged(),
            retryTick,
        ) { key, _ -> key }
            .transformLatest { (category, ingredient) ->
                if (category == null && ingredient == null) {
                    emit(RemoteSets())
                    return@transformLatest
                }
                emit(RemoteSets(category, ingredient, loading = true))
                val result = runCatchingCancellable {
                    coroutineScope {
                        val categoryJob = category?.let { async { meals.mealIdsForCategory(it) } }
                        val ingredientJob = ingredient?.let { async { meals.mealIdsForIngredient(it) } }
                        categoryJob?.await() to ingredientJob?.await()
                    }
                }
                emit(
                    result.fold(
                        onSuccess = { (c, i) -> RemoteSets(category, ingredient, c, i) },
                        onFailure = { RemoteSets(category, ingredient, error = it.toUserMessage()) },
                    ),
                )
            }

    val uiState: StateFlow<RecipesUiState> =
        combine(base, filters, remoteSets, favouriteIds, categories) { baseState, f, sets, favIds, cats ->
            buildState(baseState, f, sets, favIds, cats)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecipesUiState(filters = filters.value),
        )

    init {
        refresh()
    }

    // ---------- events ----------

    fun refresh(forceRefresh: Boolean = false) {
        baseJob?.cancel()
        baseJob = viewModelScope.launch {
            base.value = BaseState.Loading
            launch { loadCategories() }
            base.value = runCatchingCancellable { meals.getIndianMeals(forceRefresh) }.fold(
                onSuccess = { BaseState.Loaded(it) },
                onFailure = { BaseState.Failed(it.toUserMessage()) },
            )
        }
    }

    fun onQueryChange(query: String) = updateFilters { it.copy(query = query) }

    fun onSortChange(sort: SortOrder) = updateFilters { it.copy(sort = sort) }

    fun applyFilterSelection(category: String?, ingredient: String?, favouritesOnly: Boolean) =
        updateFilters {
            it.copy(
                category = category,
                ingredient = ingredient?.trim()?.takeIf { s -> s.isNotEmpty() },
                favouritesOnly = favouritesOnly,
            )
        }

    fun clearCategory() = updateFilters { it.copy(category = null) }
    fun clearIngredient() = updateFilters { it.copy(ingredient = null) }
    fun clearFavouritesOnly() = updateFilters { it.copy(favouritesOnly = false) }

    /** Clears search + every filter; keeps the chosen sort order. */
    fun clearAll() = updateFilters { FilterState(sort = it.sort) }

    fun retryFilters() = retryTick.update { it + 1 }

    fun toggleFavourite(meal: MealSummary) {
        viewModelScope.launch { favourites.toggle(meal) }
    }

    fun loadIngredientNames() {
        if (_ingredientNames.value.isNotEmpty()) return
        viewModelScope.launch {
            runCatchingCancellable { meals.getIngredientNames() }
                .onSuccess { _ingredientNames.value = it }
        }
    }

    // ---------- internals ----------

    private suspend fun loadCategories() {
        runCatchingCancellable { meals.getCategories() }.onSuccess { categories.value = it }
    }

    private fun buildState(
        baseState: BaseState,
        f: FilterState,
        sets: RemoteSets,
        favIds: Set<String>,
        cats: List<String>,
    ): RecipesUiState = when (baseState) {
        is BaseState.Loading -> RecipesUiState(LoadStatus.Loading, filters = f, categories = cats)
        is BaseState.Failed -> RecipesUiState(
            LoadStatus.Error, errorMessage = baseState.message, filters = f, categories = cats,
        )
        is BaseState.Loaded -> {
            val setsMatchFilters = sets.category == f.category && sets.ingredient == f.ingredient
            when {
                !setsMatchFilters || sets.loading -> RecipesUiState(
                    LoadStatus.Ready, filters = f, categories = cats,
                    totalCount = baseState.meals.size, isApplyingFilters = true,
                )
                sets.error != null -> RecipesUiState(
                    LoadStatus.Ready, filters = f, categories = cats,
                    totalCount = baseState.meals.size, filterError = sets.error,
                )
                else -> {
                    val visible = filterAndSort(baseState.meals, f, sets.categoryIds, sets.ingredientIds, favIds)
                    RecipesUiState(
                        status = LoadStatus.Ready,
                        filters = f,
                        categories = cats,
                        items = visible.map { RecipeItem(it, it.id in favIds, f.category) },
                        totalCount = baseState.meals.size,
                    )
                }
            }
        }
    }

    private fun updateFilters(transform: (FilterState) -> FilterState) {
        val updated = transform(filters.value)
        filters.value = updated
        savedState[KEY_QUERY] = updated.query
        savedState[KEY_CATEGORY] = updated.category
        savedState[KEY_INGREDIENT] = updated.ingredient
        savedState[KEY_FAVOURITES_ONLY] = updated.favouritesOnly
        savedState[KEY_SORT_DESC] = updated.sort == SortOrder.NameDescending
    }

    private fun restoreFilters() = FilterState(
        query = savedState.get<String>(KEY_QUERY).orEmpty(),
        category = savedState.get<String>(KEY_CATEGORY),
        ingredient = savedState.get<String>(KEY_INGREDIENT),
        favouritesOnly = savedState.get<Boolean>(KEY_FAVOURITES_ONLY) ?: false,
        sort = if (savedState.get<Boolean>(KEY_SORT_DESC) == true) SortOrder.NameDescending else SortOrder.NameAscending,
    )

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_CATEGORY = "category"
        const val KEY_INGREDIENT = "ingredient"
        const val KEY_FAVOURITES_ONLY = "favouritesOnly"
        const val KEY_SORT_DESC = "sortDescending"
    }
}

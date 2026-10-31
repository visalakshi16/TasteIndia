package com.example.tasteindia

import androidx.lifecycle.SavedStateHandle
import com.example.tasteindia.domain.MealSummary
import com.example.tasteindia.ui.recipes.FilterState
import com.example.tasteindia.ui.recipes.LoadStatus
import com.example.tasteindia.ui.recipes.RecipesViewModel
import com.example.tasteindia.ui.recipes.SortOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipesViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val meals = listOf(
        MealSummary("1", "Butter Chicken", null),
        MealSummary("2", "Dal Tadka", null),
        MealSummary("3", "Aloo Gobi", null),
    )

    private fun TestScope.createViewModel(
        repo: FakeMealRepository,
        favourites: FakeFavouritesRepository = FakeFavouritesRepository(),
        handle: SavedStateHandle = SavedStateHandle(),
    ): RecipesViewModel {
        val vm = RecipesViewModel(repo, favourites, handle)
        backgroundScope.launch { vm.uiState.collect { } }   // keep WhileSubscribed upstream active
        return vm
    }

    private fun RecipesViewModel.visibleIds() = uiState.value.items.map { it.meal.id }

    @Test
    fun `initial load shows loading then the Indian list`() = runTest {
        val vm = createViewModel(FakeMealRepository(indianMeals = meals))
        assertEquals(LoadStatus.Loading, vm.uiState.value.status)
        advanceUntilIdle()
        assertEquals(LoadStatus.Ready, vm.uiState.value.status)
        assertEquals(listOf("3", "1", "2"), vm.visibleIds())
    }

    @Test
    fun `provider failure becomes a recoverable error state`() = runTest {
        val repo = FakeMealRepository(indianMeals = meals, failIndianMeals = true)
        val vm = createViewModel(repo)
        advanceUntilIdle()
        assertEquals(LoadStatus.Error, vm.uiState.value.status)

        repo.failIndianMeals = false
        vm.refresh(forceRefresh = true)
        advanceUntilIdle()
        assertEquals(LoadStatus.Ready, vm.uiState.value.status)
    }

    @Test
    fun `rapid search typing only shows results for the latest query`() = runTest {
        val vm = createViewModel(FakeMealRepository(indianMeals = meals))
        advanceUntilIdle()
        vm.onQueryChange("dal")
        vm.onQueryChange("aloo")
        advanceUntilIdle()
        assertEquals(listOf("3"), vm.visibleIds())
    }

    @Test
    fun `a slow stale ingredient lookup cannot overwrite the latest one`() = runTest {
        val repo = FakeMealRepository(
            indianMeals = meals,
            ingredientIds = mapOf("slow" to setOf("1"), "fast" to setOf("2", "999")),
            ingredientDelaysMs = mapOf("slow" to 5_000L, "fast" to 100L),
        )
        val vm = createViewModel(repo)
        advanceUntilIdle()

        vm.applyFilterSelection(null, "slow", false)
        vm.applyFilterSelection(null, "fast", false)
        advanceUntilIdle()

        assertEquals(listOf("2"), vm.visibleIds())          // 999 is outside the Indian set
        assertFalse(vm.uiState.value.isApplyingFilters)
    }

    @Test
    fun `favourites-only reflects toggled favourites`() = runTest {
        val vm = createViewModel(FakeMealRepository(indianMeals = meals))
        advanceUntilIdle()

        vm.toggleFavourite(meals[1])
        vm.applyFilterSelection(null, null, favouritesOnly = true)
        advanceUntilIdle()
        assertEquals(listOf("2"), vm.visibleIds())

        vm.toggleFavourite(meals[1])
        advanceUntilIdle()
        assertEquals(emptyList<String>(), vm.visibleIds())
    }

    @Test
    fun `filters are restored from saved state after the view model is recreated`() = runTest {
        val handle = SavedStateHandle()
        val first = RecipesViewModel(FakeMealRepository(indianMeals = meals), FakeFavouritesRepository(), handle)
        first.onQueryChange("dal")
        first.applyFilterSelection("Chicken", "onion", favouritesOnly = true)
        first.onSortChange(SortOrder.NameDescending)

        val second = RecipesViewModel(FakeMealRepository(indianMeals = meals), FakeFavouritesRepository(), handle)
        assertEquals(
            FilterState("dal", "Chicken", "onion", true, SortOrder.NameDescending),
            second.currentFilters.value,
        )
    }

    @Test
    fun `clear all resets search and filters but keeps sort order`() = runTest {
        val vm = createViewModel(FakeMealRepository(indianMeals = meals))
        advanceUntilIdle()
        vm.onSortChange(SortOrder.NameDescending)
        vm.onQueryChange("dal")
        vm.applyFilterSelection(null, null, favouritesOnly = true)
        vm.clearAll()
        advanceUntilIdle()
        assertEquals(FilterState(sort = SortOrder.NameDescending), vm.currentFilters.value)
        assertEquals(listOf("2", "1", "3"), vm.visibleIds())
    }
}

package com.example.tasteindia.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasteindia.data.FavoritesRepository
import com.example.tasteindia.data.MealRepository
import com.example.tasteindia.data.runCatchingCancellable
import com.example.tasteindia.data.toUserMessage
import com.example.tasteindia.domain.MealDetail
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Error(val message: String) : DetailsUiState
    data class Content(val meal: MealDetail, val isFavourite: Boolean) : DetailsUiState
}

class DetailsViewModel(
    private val mealId: String,
    private val meals: MealRepository,
    private val favourites: FavoritesRepository,
) : ViewModel() {

    private val load = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    private var loadJob: Job? = null

    val uiState: StateFlow<DetailsUiState> =
        combine(load, favourites.favourites.map { list -> list.any { it.id == mealId } }) { state, isFavourite ->
            if (state is DetailsUiState.Content) state.copy(isFavourite = isFavourite) else state
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsUiState.Loading)

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            load.value = DetailsUiState.Loading
            load.value = runCatchingCancellable { meals.getMeal(mealId) }.fold(
                onSuccess = { DetailsUiState.Content(it, isFavourite = false) },
                onFailure = { DetailsUiState.Error(it.toUserMessage()) },
            )
        }
    }

    fun toggleFavourite() {
        val meal = (load.value as? DetailsUiState.Content)?.meal ?: return
        viewModelScope.launch { favourites.toggle(meal.toSummary()) }
    }
}

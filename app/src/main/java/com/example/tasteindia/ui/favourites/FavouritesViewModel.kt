package com.example.tasteindia.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasteindia.data.FavoritesRepository
import com.example.tasteindia.domain.MealSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavouritesUiState(
    val loaded: Boolean = false,
    val items: List<MealSummary> = emptyList(),
)

class FavouritesViewModel(private val favourites: FavoritesRepository) : ViewModel() {

    val uiState: StateFlow<FavouritesUiState> =
        favourites.favourites
            .map { FavouritesUiState(loaded = true, items = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavouritesUiState())

    fun remove(id: String) {
        viewModelScope.launch { favourites.remove(id) }
    }
}

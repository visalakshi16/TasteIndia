package com.example.tasteindia.ui.favourites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tasteindia.ui.components.AttributionFooter
import com.example.tasteindia.ui.components.RecipeRow
import com.example.tasteindia.ui.components.StatusMessage
import com.example.tasteindia.ui.navigation.TasteIndiaBottomBar
import com.example.tasteindia.ui.navigation.TopLevelTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesRoute(
    viewModel: FavouritesViewModel,
    onOpenMeal: (String) -> Unit,
    onSelectTab: (TopLevelTab) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Favourites") }) },
        bottomBar = { TasteIndiaBottomBar(TopLevelTab.Favourites, onSelectTab) },
    ) { padding ->
        when {
            !state.loaded -> StatusMessage("Loading favourites…", Modifier.padding(padding), showSpinner = true)
            state.items.isEmpty() -> StatusMessage(
                "No favourites yet. Tap the heart on any recipe to save it here.",
                Modifier.padding(padding),
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.items, key = { it.id }) { meal ->
                    RecipeRow(
                        name = meal.name,
                        thumbnailUrl = meal.thumbnailUrl,
                        subtitle = null,
                        isFavourite = true,
                        onClick = { onOpenMeal(meal.id) },
                        onToggleFavourite = { viewModel.remove(meal.id) },
                    )
                }
                item(key = "attribution") { AttributionFooter() }
            }
        }
    }
}

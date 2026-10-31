package com.example.tasteindia.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tasteindia.domain.MealSummary
import com.example.tasteindia.ui.components.AttributionFooter
import com.example.tasteindia.ui.components.RecipeRow
import com.example.tasteindia.ui.components.StatusMessage
import com.example.tasteindia.ui.navigation.TasteIndiaBottomBar
import com.example.tasteindia.ui.navigation.TopLevelTab

@Stable
class RecipesActions(
    val onQueryChange: (String) -> Unit,
    val onSortChange: (SortOrder) -> Unit,
    val onApplyFilters: (category: String?, ingredient: String?, favouritesOnly: Boolean) -> Unit,
    val onClearCategory: () -> Unit,
    val onClearIngredient: () -> Unit,
    val onClearFavouritesOnly: () -> Unit,
    val onClearAll: () -> Unit,
    val onRetry: () -> Unit,
    val onRetryFilters: () -> Unit,
    val onFilterSheetOpened: () -> Unit,
    val onToggleFavourite: (MealSummary) -> Unit,
    val onOpenMeal: (String) -> Unit,
)

@Composable
fun RecipesRoute(
    viewModel: RecipesViewModel,
    onOpenMeal: (String) -> Unit,
    onSelectTab: (TopLevelTab) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val liveFilters by viewModel.currentFilters.collectAsStateWithLifecycle()
    val ingredientNames by viewModel.ingredientNames.collectAsStateWithLifecycle()

    val actions = remember(viewModel, onOpenMeal) {
        RecipesActions(
            onQueryChange = viewModel::onQueryChange,
            onSortChange = viewModel::onSortChange,
            onApplyFilters = viewModel::applyFilterSelection,
            onClearCategory = viewModel::clearCategory,
            onClearIngredient = viewModel::clearIngredient,
            onClearFavouritesOnly = viewModel::clearFavouritesOnly,
            onClearAll = viewModel::clearAll,
            onRetry = { viewModel.refresh(forceRefresh = true) },
            onRetryFilters = viewModel::retryFilters,
            onFilterSheetOpened = viewModel::loadIngredientNames,
            onToggleFavourite = viewModel::toggleFavourite,
            onOpenMeal = onOpenMeal,
        )
    }

    RecipesScreen(
        state = state,
        query = liveFilters.query,
        ingredientNames = ingredientNames,
        actions = actions,
        onSelectTab = onSelectTab,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipesScreen(
    state: RecipesUiState,
    query: String,
    ingredientNames: List<String>,
    actions: RecipesActions,
    onSelectTab: (TopLevelTab) -> Unit,
) {
    // rememberLazyListState is saveable, so scroll position is restored when coming back from Details.
    val listState = rememberLazyListState()
    var showSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("TasteIndia") }) },
        bottomBar = { TasteIndiaBottomBar(TopLevelTab.Recipes, onSelectTab) },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = actions.onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search recipes by name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { actions.onQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(onClick = { showSheet = true }) {
                            val count = state.filters.sheetFilterCount
                            Text(if (count > 0) "Filters ($count)" else "Filters")
                        }
                        SortChip("A–Z", state.filters.sort == SortOrder.NameAscending) {
                            actions.onSortChange(SortOrder.NameAscending)
                        }
                        SortChip("Z–A", state.filters.sort == SortOrder.NameDescending) {
                            actions.onSortChange(SortOrder.NameDescending)
                        }
                    }
                    ActiveFilters(state.filters, actions)
                    if (state.status == LoadStatus.Ready && !state.isApplyingFilters && state.filterError == null) {
                        Text(
                            text = "${state.items.size} of ${state.totalCount} recipes",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
            }

            when {
                state.status == LoadStatus.Loading -> item(key = "loading") {
                    StatusMessage("Loading Indian recipes…", showSpinner = true)
                }
                state.status == LoadStatus.Error -> item(key = "error") {
                    StatusMessage(state.errorMessage ?: "Something went wrong.") {
                        Button(onClick = actions.onRetry) { Text("Retry") }
                    }
                }
                state.isApplyingFilters -> item(key = "applying") {
                    StatusMessage("Applying filters…", showSpinner = true)
                }
                state.filterError != null -> item(key = "filterError") {
                    StatusMessage(state.filterError) {
                        Button(onClick = actions.onRetryFilters) { Text("Retry") }
                        OutlinedButton(onClick = actions.onClearAll) { Text("Clear all") }
                    }
                }
                state.items.isEmpty() -> item(key = "empty") {
                    StatusMessage("No recipes match your search and filters.") {
                        if (state.filters.hasActiveFilters) {
                            Button(onClick = actions.onClearAll) { Text("Clear all") }
                        }
                    }
                }
                else -> items(state.items, key = { it.meal.id }) { item ->
                    RecipeRow(
                        name = item.meal.name,
                        thumbnailUrl = item.meal.thumbnailUrl,
                        subtitle = item.category,
                        isFavourite = item.isFavourite,
                        onClick = { actions.onOpenMeal(item.meal.id) },
                        onToggleFavourite = { actions.onToggleFavourite(item.meal) },
                    )
                }
            }

            item(key = "attribution") { AttributionFooter() }
        }
    }

    if (showSheet) {
        LaunchedEffect(Unit) { actions.onFilterSheetOpened() }
        FilterSheet(
            filters = state.filters,
            categories = state.categories,
            ingredientNames = ingredientNames,
            onDismiss = { showSheet = false },
            onApply = { category, ingredient, favouritesOnly ->
                actions.onApplyFilters(category, ingredient, favouritesOnly)
                showSheet = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val checkIcon: (@Composable () -> Unit)? =
        if (selected) { { Icon(Icons.Default.Check, contentDescription = null) } } else null
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("Sort $label") },
        leadingIcon = checkIcon,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilters(filters: FilterState, actions: RecipesActions) {
    if (!filters.hasActiveFilters) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (filters.query.isNotBlank()) {
            RemovableChip("Search: “${filters.query.trim()}”") { actions.onQueryChange("") }
        }
        filters.category?.let { RemovableChip("Category: $it", actions.onClearCategory) }
        filters.ingredient?.let { RemovableChip("Ingredient: $it", actions.onClearIngredient) }
        if (filters.favouritesOnly) RemovableChip("Favourites only", actions.onClearFavouritesOnly)
        TextButton(onClick = actions.onClearAll) { Text("Clear all") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemovableChip(label: String, onRemove: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove filter: $label") },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    filters: FilterState,
    categories: List<String>,
    ingredientNames: List<String>,
    onDismiss: () -> Unit,
    onApply: (category: String?, ingredient: String?, favouritesOnly: Boolean) -> Unit,
) {
    // Draft values: nothing changes in the list until Apply is pressed.
    var category by rememberSaveable { mutableStateOf(filters.category) }
    var ingredient by rememberSaveable { mutableStateOf(filters.ingredient.orEmpty()) }
    var favouritesOnly by rememberSaveable { mutableStateOf(filters.favouritesOnly) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Filters",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )

            Text("Category", style = MaterialTheme.typography.titleMedium)
            if (categories.isEmpty()) {
                Text("Categories are unavailable right now.", style = MaterialTheme.typography.bodyMedium)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { name ->
                        val selected = category == name
                        val checkIcon: (@Composable () -> Unit)? =
                            if (selected) { { Icon(Icons.Default.Check, contentDescription = null) } } else null
                        FilterChip(
                            selected = selected,
                            onClick = { category = if (selected) null else name },
                            label = { Text(name) },
                            leadingIcon = checkIcon,
                        )
                    }
                }
            }

            Text("Main ingredient", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = ingredient,
                onValueChange = { ingredient = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("e.g. paneer, chicken") },
                singleLine = true,
            )
            val suggestions = remember(ingredient, ingredientNames) {
                val typed = ingredient.trim()
                if (typed.isEmpty()) emptyList()
                else ingredientNames
                    .filter { it.contains(typed, ignoreCase = true) && !it.equals(typed, ignoreCase = true) }
                    .take(6)
            }
            if (suggestions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    suggestions.forEach { suggestion ->
                        SuggestionChip(onClick = { ingredient = suggestion }, label = { Text(suggestion) })
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = favouritesOnly,
                        role = Role.Switch,
                        onValueChange = { favouritesOnly = it },
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Favourites only", modifier = Modifier.weight(1f))
                Switch(checked = favouritesOnly, onCheckedChange = null)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        category = null
                        ingredient = ""
                        favouritesOnly = false
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Reset") }
                Button(
                    onClick = { onApply(category, ingredient.trim().ifBlank { null }, favouritesOnly) },
                    modifier = Modifier.weight(1f),
                ) { Text("Apply") }
            }
        }
    }
}

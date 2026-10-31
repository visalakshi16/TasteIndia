package com.example.tasteindia.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tasteindia.AppContainer
import com.example.tasteindia.ui.details.DetailsRoute
import com.example.tasteindia.ui.details.DetailsViewModel
import com.example.tasteindia.ui.favourites.FavouritesRoute
import com.example.tasteindia.ui.favourites.FavouritesViewModel
import com.example.tasteindia.ui.recipes.RecipesRoute
import com.example.tasteindia.ui.recipes.RecipesViewModel

/** Central list of destinations. Details takes only a meal id, never a serialised model. */
object Routes {
    const val RECIPES = "recipes"
    const val FAVOURITES = "favourites"
    const val MEAL_ID_ARG = "mealId"
    const val DETAILS = "details/{$MEAL_ID_ARG}"

    fun details(mealId: String) = "details/${Uri.encode(mealId)}"
}

enum class TopLevelTab(val route: String, val label: String) {
    Recipes(Routes.RECIPES, "Recipes"),
    Favourites(Routes.FAVOURITES, "Favourites"),
}

@Composable
fun TasteIndiaBottomBar(selected: TopLevelTab, onSelect: (TopLevelTab) -> Unit) {
    NavigationBar {
        TopLevelTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = if (tab == TopLevelTab.Recipes) Icons.AutoMirrored.Filled.List else Icons.Filled.Favorite,
                        contentDescription = null,
                    )
                },
                label = { Text(tab.label) },
            )
        }
    }
}

private fun NavController.navigateToTab(tab: TopLevelTab) {
    navigate(tab.route) {
        // Save/restore each tab's back stack state so list position and filters survive tab switches.
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun TasteIndiaNavHost(container: AppContainer) {
    val navController = rememberNavController()

    val recipesFactory = remember(container) {
        viewModelFactory {
            initializer {
                RecipesViewModel(container.mealRepository, container.favouritesRepository, createSavedStateHandle())
            }
        }
    }
    val favouritesFactory = remember(container) {
        viewModelFactory { initializer { FavouritesViewModel(container.favouritesRepository) } }
    }
    val detailsFactory = remember(container) {
        viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                // Navigation puts route arguments into the SavedStateHandle.
                DetailsViewModel(
                    mealId = checkNotNull(handle.get<String>(Routes.MEAL_ID_ARG)),
                    meals = container.mealRepository,
                    favourites = container.favouritesRepository,
                )
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.RECIPES) {
        composable(Routes.RECIPES) {
            val viewModel: RecipesViewModel = viewModel(factory = recipesFactory)
            RecipesRoute(
                viewModel = viewModel,
                onOpenMeal = { navController.navigate(Routes.details(it)) },
                onSelectTab = navController::navigateToTab,
            )
        }
        composable(Routes.FAVOURITES) {
            val viewModel: FavouritesViewModel = viewModel(factory = favouritesFactory)
            FavouritesRoute(
                viewModel = viewModel,
                onOpenMeal = { navController.navigate(Routes.details(it)) },
                onSelectTab = navController::navigateToTab,
            )
        }
        composable(
            route = Routes.DETAILS,
            arguments = listOf(navArgument(Routes.MEAL_ID_ARG) { type = NavType.StringType }),
        ) {
            val viewModel: DetailsViewModel = viewModel(factory = detailsFactory)
            // Back and Up both pop, which returns to the still-alive Recipes/Favourites entry.
            DetailsRoute(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}

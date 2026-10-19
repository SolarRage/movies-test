package com.myarulin.movies_test.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myarulin.movies_test.sdk.AdsManager
import com.myarulin.movies_test.ui.details.DetailsScreen
import com.myarulin.movies_test.ui.details.DetailsViewModel
import com.myarulin.movies_test.ui.favorites.FavoritesScreen
import com.myarulin.movies_test.ui.favorites.FavoritesViewModel
import com.myarulin.movies_test.ui.search.SearchScreen
import com.myarulin.movies_test.ui.search.SearchViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

private const val SEARCH_ROUTE = "search"
private const val FAVORITES_ROUTE = "favorites"
private const val DETAILS_ROUTE = "details/{imdbId}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = currentRoute == SEARCH_ROUTE || currentRoute == FAVORITES_ROUTE

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentRoute) {
                            FAVORITES_ROUTE -> "Favorites"
                            DETAILS_ROUTE -> "Movie details"
                            else -> "Movie search"
                        },
                    )
                },
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = navController::navigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == SEARCH_ROUTE,
                        onClick = {
                            navController.navigate(SEARCH_ROUTE) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("Search") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == FAVORITES_ROUTE,
                        onClick = {
                            navController.navigate(FAVORITES_ROUTE) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text("Favorites") },
                    )
                }
            }
        },
    ) { padding ->
        val adsManager: AdsManager = koinInject()
        NavHost(
            navController = navController,
            startDestination = SEARCH_ROUTE,
            modifier = Modifier.padding(padding),
        ) {
            composable(SEARCH_ROUTE) {
                val viewModel: SearchViewModel = koinViewModel()
                SearchScreen(
                    viewModel = viewModel,
                    adsManager = adsManager,
                    onMovieClick = { imdbId -> navController.navigate("details/$imdbId") },
                )
            }
            composable(FAVORITES_ROUTE) {
                val viewModel: FavoritesViewModel = koinViewModel()
                FavoritesScreen(
                    viewModel = viewModel,
                    onMovieClick = { imdbId -> navController.navigate("details/$imdbId") },
                )
            }
            composable(
                route = DETAILS_ROUTE,
                arguments = listOf(navArgument("imdbId") { type = NavType.StringType }),
            ) { entry ->
                val imdbId = requireNotNull(entry.arguments?.getString("imdbId"))
                val viewModel: DetailsViewModel = koinViewModel(
                    parameters = { parametersOf(imdbId) },
                )
                DetailsScreen(viewModel = viewModel)
            }
        }
    }
}

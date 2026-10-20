package com.myarulin.movies_test.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.myarulin.movies_test.sdk.AdsManager
import com.myarulin.movies_test.sdk.NativeAdsLoadState
import com.myarulin.movies_test.ui.components.LoadingContent
import com.myarulin.movies_test.ui.components.MessageContent
import com.myarulin.movies_test.ui.components.MovieCard

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    adsManager: AdsManager,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val submittedQuery by viewModel.submittedQuery.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val movies = viewModel.movies.collectAsLazyPagingItems()
    val focusManager = LocalFocusManager.current
    val hasNativeAds = movies.itemCount >= MIN_MOVIES_FOR_NATIVE_ADS
    val nativeAdSlotIds = remember(submittedQuery, hasNativeAds) {
        if (submittedQuery.isNotBlank() && hasNativeAds) {
            setOf(
                "$submittedQuery:$FIRST_NATIVE_AD_INDEX",
                "$submittedQuery:$SECOND_NATIVE_AD_INDEX",
            )
        } else {
            emptySet()
        }
    }
    val nativeAdsLoadState = adsManager.prepareNativeAds(nativeAdSlotIds)
    val refreshState = movies.loadState.refresh

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChanged,
            label = { Text("Search movies") },
            placeholder = { Text("Title or keyword") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )

        when {
            submittedQuery.isBlank() -> MessageContent("Start typing to search the OMDb catalog.")
            refreshState is LoadState.Loading -> LoadingContent()
            refreshState is LoadState.Error -> {
                MessageContent(
                    message = refreshState.error.message ?: "Unable to load movies.",
                    actionLabel = "Retry",
                    onAction = movies::retry,
                )
            }
            refreshState is LoadState.NotLoading && movies.itemCount == 0 -> {
                MessageContent("No movies found for “$submittedQuery”.")
            }
            nativeAdsLoadState == NativeAdsLoadState.Loading -> LoadingContent()
            else -> {
                val displayCount = searchDisplayItemCount(movies.itemCount)
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        count = displayCount,
                        key = { displayIndex ->
                            when (val row = searchRowAt(displayIndex, movies.itemCount)) {
                                is SearchRow.NativeAdRow -> "ad:$submittedQuery:${row.slotIndex}"
                                is SearchRow.MovieRow -> movies.peek(row.movieIndex)?.imdbId
                                    ?: "movie-placeholder:${row.movieIndex}"
                            }
                        },
                    ) { displayIndex ->
                        when (val row = searchRowAt(displayIndex, movies.itemCount)) {
                            is SearchRow.NativeAdRow -> adsManager.NativeAd(
                                slotId = "$submittedQuery:${row.slotIndex}",
                                modifier = Modifier,
                            )
                            is SearchRow.MovieRow -> movies[row.movieIndex]?.let { movie ->
                                MovieCard(
                                    movie = movie,
                                    isFavorite = movie.imdbId in favoriteIds,
                                    onClick = { onMovieClick(movie.imdbId) },
                                    onFavoriteClick = { viewModel.toggleFavorite(movie) },
                                )
                            }
                        }
                    }

                    when (val append = movies.loadState.append) {
                        LoadState.Loading -> item(key = "append-loading") {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        is LoadState.Error -> item(key = "append-error") {
                            MessageContent(
                                message = append.error.message ?: "Unable to load the next page.",
                                actionLabel = "Retry",
                                onAction = movies::retry,
                            )
                        }
                        is LoadState.NotLoading -> Unit
                    }
                }
            }
        }
    }
}

private const val MIN_MOVIES_FOR_NATIVE_ADS = 3
private const val FIRST_NATIVE_AD_INDEX = 1
private const val SECOND_NATIVE_AD_INDEX = 3

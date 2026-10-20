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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.myarulin.movies_test.R
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
    val requestedNativeAdPositions = nativeAdPositions(movies.itemCount)
    val nativeAdSlotIds = remember(submittedQuery, requestedNativeAdPositions) {
        if (submittedQuery.isNotBlank()) {
            requestedNativeAdPositions.mapTo(linkedSetOf()) { position ->
                nativeAdSlotId(submittedQuery, position)
            }
        } else {
            emptySet()
        }
    }
    val nativeAdsLoadState = adsManager.prepareNativeAds(nativeAdSlotIds)
    val visibleNativeAdPositions = requestedNativeAdPositions.filterTo(linkedSetOf()) { position ->
        nativeAdsLoadState[nativeAdSlotId(submittedQuery, position)] != NativeAdsLoadState.Failed
    }
    val refreshState = movies.loadState.refresh

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChanged,
            label = { Text(stringResource(R.string.search_movies)) },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChanged("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear_search),
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )

        when {
            submittedQuery.isBlank() -> MessageContent(stringResource(R.string.search_start_message))
            refreshState is LoadState.Loading -> LoadingContent()
            refreshState is LoadState.Error -> {
                MessageContent(
                    message = refreshState.error.message ?: stringResource(R.string.search_load_error),
                    actionLabel = stringResource(R.string.retry),
                    onAction = movies::retry,
                )
            }
            refreshState is LoadState.NotLoading && movies.itemCount == 0 -> {
                MessageContent(stringResource(R.string.search_empty_message, submittedQuery))
            }
            else -> {
                val displayCount = searchDisplayItemCount(movies.itemCount, visibleNativeAdPositions)
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        count = displayCount,
                        key = { displayIndex ->
                            when (
                                val row = searchRowAt(
                                    displayIndex,
                                    movies.itemCount,
                                    visibleNativeAdPositions,
                                )
                            ) {
                                is SearchRow.NativeAdRow -> "ad:$submittedQuery:${row.slotIndex}"
                                is SearchRow.MovieRow -> {
                                    val imdbId = movies.peek(row.movieIndex)?.imdbId ?: "placeholder"
                                    "movie:${row.movieIndex}:$imdbId"
                                }
                            }
                        },
                    ) { displayIndex ->
                        when (
                            val row = searchRowAt(
                                displayIndex,
                                movies.itemCount,
                                visibleNativeAdPositions,
                            )
                        ) {
                            is SearchRow.NativeAdRow -> adsManager.NativeAd(
                                slotId = nativeAdSlotId(submittedQuery, row.slotIndex),
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
                                message = append.error.message
                                    ?: stringResource(R.string.search_next_page_error),
                                actionLabel = stringResource(R.string.retry),
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

private fun nativeAdSlotId(query: String, position: Int) = "$query:$position"

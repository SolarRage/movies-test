package com.myarulin.movies_test.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myarulin.movies_test.R
import com.myarulin.movies_test.ui.components.MessageContent
import com.myarulin.movies_test.ui.components.MovieCard

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    if (favorites.isEmpty()) {
        MessageContent(
            message = stringResource(R.string.favorites_empty_message),
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = favorites, key = { it.imdbId }) { movie ->
            MovieCard(
                movie = movie,
                isFavorite = true,
                onClick = { onMovieClick(movie.imdbId) },
                onFavoriteClick = { viewModel.remove(movie.imdbId) },
            )
        }
    }
}

package com.myarulin.movies_test.ui.details

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.myarulin.movies_test.ui.components.LoadingContent
import com.myarulin.movies_test.ui.components.MessageContent

@Composable
fun DetailsScreen(
    viewModel: DetailsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()

    when (val current = state) {
        DetailsUiState.Loading -> LoadingContent(modifier)
        is DetailsUiState.Error -> MessageContent(
            message = current.message,
            modifier = modifier,
            actionLabel = "Retry",
            onAction = viewModel::retry,
        )
        is DetailsUiState.Content -> {
            val movie = current.movie
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = "Poster for ${movie.title}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(20.dp))
                Text(movie.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    listOf(movie.year, movie.type).filter(String::isNotBlank).joinToString(" • "),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
                movie.plot?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                Button(
                    onClick = viewModel::toggleFavorite,
                    modifier = Modifier.padding(top = 20.dp),
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                    )
                    Text(
                        text = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

package com.myarulin.movies_test.domain.repository

import com.myarulin.movies_test.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeFavorites(): Flow<List<Movie>>

    fun observeFavoriteIds(): Flow<Set<String>>

    fun observeIsFavorite(imdbId: String): Flow<Boolean>

    suspend fun remove(imdbId: String)

    suspend fun toggle(movie: Movie)
}

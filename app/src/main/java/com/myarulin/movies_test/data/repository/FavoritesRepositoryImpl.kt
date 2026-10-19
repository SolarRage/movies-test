package com.myarulin.movies_test.data.repository

import com.myarulin.movies_test.data.local.FavoriteMovieDao
import com.myarulin.movies_test.data.local.toDomain
import com.myarulin.movies_test.data.local.toEntity
import com.myarulin.movies_test.domain.model.Movie
import com.myarulin.movies_test.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesRepositoryImpl(
    private val dao: FavoriteMovieDao,
    private val now: () -> Long = System::currentTimeMillis,
) : FavoritesRepository {
    override fun observeFavorites(): Flow<List<Movie>> =
        dao.observeAll().map { favorites -> favorites.map { it.toDomain() } }

    override fun observeFavoriteIds(): Flow<Set<String>> =
        dao.observeIds().map(List<String>::toSet)

    override fun observeIsFavorite(imdbId: String): Flow<Boolean> = dao.observeExists(imdbId)

    override suspend fun add(movie: Movie) {
        dao.upsert(movie.toEntity(addedAt = now()))
    }

    override suspend fun remove(imdbId: String) {
        dao.deleteById(imdbId)
    }

    override suspend fun toggle(movie: Movie) {
        if (dao.exists(movie.imdbId)) remove(movie.imdbId) else add(movie)
    }
}

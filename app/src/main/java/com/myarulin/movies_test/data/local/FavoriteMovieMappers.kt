package com.myarulin.movies_test.data.local

import com.myarulin.movies_test.domain.model.Movie

internal fun FavoriteMovieEntity.toDomain() = Movie(
    imdbId = imdbId,
    title = title,
    posterUrl = posterUrl,
    type = type,
    year = year,
)

internal fun Movie.toEntity(addedAt: Long) = FavoriteMovieEntity(
    imdbId = imdbId,
    title = title,
    posterUrl = posterUrl,
    type = type,
    year = year,
    addedAt = addedAt,
)

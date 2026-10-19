package com.myarulin.movies_test.data.remote

import com.myarulin.movies_test.data.remote.dto.MovieDetailsDto
import com.myarulin.movies_test.data.remote.dto.MovieDto
import com.myarulin.movies_test.domain.model.Movie
import com.myarulin.movies_test.domain.model.MovieDetails

internal fun MovieDto.toDomain() = Movie(
    imdbId = imdbId,
    title = title,
    posterUrl = poster.takeUnless { it.isNullOrBlank() || it == "N/A" },
    type = type.orEmpty().ifBlank { "Unknown" },
    year = year.orEmpty(),
)

internal fun MovieDetailsDto.toDomain(): MovieDetails {
    val id = imdbId ?: throw OmdbException("The details response does not contain an IMDb ID.")
    return MovieDetails(
        imdbId = id,
        title = title.orEmpty().ifBlank { "Untitled" },
        posterUrl = poster.takeUnless { it.isNullOrBlank() || it == "N/A" },
        type = type.orEmpty().ifBlank { "Unknown" },
        year = year.orEmpty(),
        plot = plot.takeUnless { it.isNullOrBlank() || it == "N/A" },
    )
}

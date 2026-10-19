package com.myarulin.movies_test.domain.model

data class MovieDetails(
    val imdbId: String,
    val title: String,
    val posterUrl: String?,
    val type: String,
    val year: String,
    val plot: String?,
)

package com.myarulin.movies_test.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MovieDetailsDto(
    @SerializedName("imdbID") val imdbId: String?,
    @SerializedName("Title") val title: String?,
    @SerializedName("Poster") val poster: String?,
    @SerializedName("Type") val type: String?,
    @SerializedName("Year") val year: String?,
    @SerializedName("Plot") val plot: String?,
    @SerializedName("Response") val response: String?,
    @SerializedName("Error") val error: String?,
)

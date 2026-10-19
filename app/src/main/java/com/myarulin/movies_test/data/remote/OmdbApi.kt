package com.myarulin.movies_test.data.remote

import com.myarulin.movies_test.data.remote.dto.MovieDetailsDto
import com.myarulin.movies_test.data.remote.dto.SearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OmdbApi {
    @GET("/")
    suspend fun search(
        @Query("apikey") apiKey: String,
        @Query("s") query: String,
        @Query("page") page: Int,
    ): SearchResponseDto

    @GET("/")
    suspend fun details(
        @Query("apikey") apiKey: String,
        @Query("i") imdbId: String,
        @Query("plot") plot: String = "short",
    ): MovieDetailsDto
}

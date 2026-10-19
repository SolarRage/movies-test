package com.myarulin.movies_test.domain.repository

import androidx.paging.PagingData
import com.myarulin.movies_test.domain.model.Movie
import com.myarulin.movies_test.domain.model.MovieDetails
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun search(query: String): Flow<PagingData<Movie>>

    suspend fun getDetails(imdbId: String): MovieDetails
}

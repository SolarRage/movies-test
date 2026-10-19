package com.myarulin.movies_test.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.myarulin.movies_test.data.remote.MissingApiKeyException
import com.myarulin.movies_test.data.remote.OmdbApi
import com.myarulin.movies_test.data.remote.OmdbException
import com.myarulin.movies_test.data.remote.OmdbMoviePagingSource
import com.myarulin.movies_test.data.remote.toDomain
import com.myarulin.movies_test.domain.model.Movie
import com.myarulin.movies_test.domain.model.MovieDetails
import com.myarulin.movies_test.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow

class MovieRepositoryImpl(
    private val api: OmdbApi,
    private val apiKey: String,
) : MovieRepository {
    override fun search(query: String): Flow<PagingData<Movie>> = Pager(
        config = PagingConfig(
            pageSize = OMDB_PAGE_SIZE,
            initialLoadSize = OMDB_PAGE_SIZE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = {
            OmdbMoviePagingSource(api = api, apiKey = apiKey, query = query)
        },
    ).flow

    override suspend fun getDetails(imdbId: String): MovieDetails {
        if (apiKey.isBlank()) throw MissingApiKeyException()
        val response = api.details(apiKey = apiKey, imdbId = imdbId)
        if (!response.response.equals("True", ignoreCase = true)) {
            throw OmdbException(response.error ?: "OMDb details request failed.")
        }
        return response.toDomain()
    }

    private companion object {
        const val OMDB_PAGE_SIZE = 10
    }
}

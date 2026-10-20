package com.myarulin.movies_test.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.myarulin.movies_test.domain.model.Movie
import kotlinx.coroutines.CancellationException

class OmdbMoviePagingSource(
    private val api: OmdbApi,
    private val apiKey: String,
    private val query: String,
) : PagingSource<Int, Movie>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: FIRST_PAGE
        return try {
            if (apiKey.isBlank()) throw MissingApiKeyException()

            val response = api.search(apiKey = apiKey, query = query, page = page)
            if (!response.response.equals("True", ignoreCase = true)) {
                if (response.error.equals("Movie not found!", ignoreCase = true)) {
                    return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
                }
                throw OmdbException(response.error ?: "OMDb search failed.")
            }

            val movies = response.results.orEmpty().mapNotNull { it.toDomain() }
            val totalResults = response.totalResults?.toIntOrNull() ?: movies.size
            val loadedThrough = page * OMDB_PAGE_SIZE
            LoadResult.Page(
                data = movies,
                prevKey = if (page == FIRST_PAGE) null else page - 1,
                nextKey = if (movies.isEmpty() || loadedThrough >= totalResults) null else page + 1,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? {
        val anchor = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchor) ?: return null
        return anchorPage.prevKey?.plus(1) ?: anchorPage.nextKey?.minus(1)
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val OMDB_PAGE_SIZE = 10
    }
}

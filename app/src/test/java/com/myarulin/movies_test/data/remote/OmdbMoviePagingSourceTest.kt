package com.myarulin.movies_test.data.remote

import androidx.paging.PagingSource
import com.google.common.truth.Truth.assertThat
import com.myarulin.movies_test.data.remote.dto.MovieDetailsDto
import com.myarulin.movies_test.data.remote.dto.MovieDto
import com.myarulin.movies_test.data.remote.dto.SearchResponseDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OmdbMoviePagingSourceTest {
    @Test
    fun `first page returns next key while more results exist`() = runTest {
        val api = FakeOmdbApi(
            SearchResponseDto(
                results = listOf(movieDto("tt1")),
                totalResults = "20",
                response = "True",
                error = null,
            ),
        )
        val source = OmdbMoviePagingSource(api, "test-key", "love")

        val result = source.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false),
        )

        val page = result as PagingSource.LoadResult.Page
        assertThat(page.data.map { it.imdbId }).containsExactly("tt1")
        assertThat(page.prevKey).isNull()
        assertThat(page.nextKey).isEqualTo(2)
    }

    @Test
    fun `movie not found response becomes an empty page`() = runTest {
        val api = FakeOmdbApi(
            SearchResponseDto(
                results = null,
                totalResults = null,
                response = "False",
                error = "Movie not found!",
            ),
        )
        val source = OmdbMoviePagingSource(api, "test-key", "missing")

        val result = source.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false),
        )

        val page = result as PagingSource.LoadResult.Page
        assertThat(page.data).isEmpty()
        assertThat(page.nextKey).isNull()
    }

    @Test
    fun `entries without an imdb id are ignored`() = runTest {
        val api = FakeOmdbApi(
            SearchResponseDto(
                results = listOf(movieDto(null), movieDto("tt1")),
                totalResults = "2",
                response = "True",
                error = null,
            ),
        )
        val source = OmdbMoviePagingSource(api, "test-key", "movie")

        val result = source.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page

        assertThat(result.data.map { it.imdbId }).containsExactly("tt1")
    }

    @Test
    fun `coroutine cancellation is rethrown`() = runTest {
        val cancellation = CancellationException("cancel search")
        val api = object : OmdbApi {
            override suspend fun search(apiKey: String, query: String, page: Int): SearchResponseDto {
                throw cancellation
            }

            override suspend fun details(apiKey: String, imdbId: String, plot: String) =
                error("Not used")
        }
        val source = OmdbMoviePagingSource(api, "test-key", "movie")

        try {
            source.load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false),
            )
            throw AssertionError("Expected CancellationException")
        } catch (error: CancellationException) {
            assertThat(error).isSameInstanceAs(cancellation)
        }
    }

    private fun movieDto(id: String?) = MovieDto(
        imdbId = id,
        title = "Movie",
        poster = "N/A",
        type = "movie",
        year = "2026",
    )
}

private class FakeOmdbApi(
    private val searchResponse: SearchResponseDto,
) : OmdbApi {
    override suspend fun search(apiKey: String, query: String, page: Int) = searchResponse

    override suspend fun details(apiKey: String, imdbId: String, plot: String) = MovieDetailsDto(
        imdbId = imdbId,
        title = "Movie",
        poster = null,
        type = "movie",
        year = "2026",
        plot = null,
        response = "True",
        error = null,
    )
}

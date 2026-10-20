package com.myarulin.movies_test.ui.details

import androidx.paging.PagingData
import com.google.common.truth.Truth.assertThat
import com.myarulin.movies_test.domain.model.Movie
import com.myarulin.movies_test.domain.model.MovieDetails
import com.myarulin.movies_test.domain.repository.FavoritesRepository
import com.myarulin.movies_test.domain.repository.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `repository failure becomes an error state`() = runTest(dispatcher) {
        val repository = DetailsMovieRepository { throw IllegalStateException("offline") }
        val viewModel = DetailsViewModel("tt1", repository, DetailsFavoritesRepository())

        runCurrent()

        assertThat(viewModel.state.value).isEqualTo(DetailsUiState.Error("offline"))
    }

    @Test
    fun `retry cancels the previous request before starting another`() = runTest(dispatcher) {
        var requestCount = 0
        var firstRequestCancelled = false
        val details = movieDetails()
        val repository = DetailsMovieRepository {
            requestCount += 1
            if (requestCount == 1) {
                try {
                    awaitCancellation()
                } finally {
                    firstRequestCancelled = true
                }
            }
            details
        }
        val viewModel = DetailsViewModel("tt1", repository, DetailsFavoritesRepository())
        runCurrent()

        viewModel.retry()
        runCurrent()

        assertThat(firstRequestCancelled).isTrue()
        assertThat(viewModel.state.value).isEqualTo(DetailsUiState.Content(details))
    }
}

private class DetailsMovieRepository(
    private val details: suspend () -> MovieDetails,
) : MovieRepository {
    override fun search(query: String): Flow<PagingData<Movie>> = flowOf(PagingData.empty())
    override suspend fun getDetails(imdbId: String): MovieDetails = details()
}

private class DetailsFavoritesRepository : FavoritesRepository {
    override fun observeFavorites(): Flow<List<Movie>> = emptyFlow()
    override fun observeFavoriteIds(): Flow<Set<String>> = emptyFlow()
    override fun observeIsFavorite(imdbId: String): Flow<Boolean> = flowOf(false)
    override suspend fun remove(imdbId: String) = Unit
    override suspend fun toggle(movie: Movie) = Unit
}

private fun movieDetails() = MovieDetails(
    imdbId = "tt1",
    title = "Movie",
    posterUrl = null,
    type = "movie",
    year = "2026",
    plot = null,
)

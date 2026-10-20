package com.myarulin.movies_test.ui.search

import androidx.paging.PagingData
import com.google.common.truth.Truth.assertThat
import com.myarulin.movies_test.domain.model.Movie
import com.myarulin.movies_test.domain.model.MovieDetails
import com.myarulin.movies_test.domain.repository.FavoritesRepository
import com.myarulin.movies_test.domain.repository.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
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
    fun `search waits for debounce and trims the query`() = runTest(dispatcher) {
        val repository = RecordingMovieRepository()
        val viewModel = SearchViewModel(repository, EmptyFavoritesRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.movies.collect {}
        }

        viewModel.onQueryChanged("  matrix  ")
        advanceTimeBy(499)
        runCurrent()
        assertThat(repository.queries).isEmpty()

        advanceTimeBy(1)
        runCurrent()
        assertThat(repository.queries).containsExactly("matrix")
    }
}

private class RecordingMovieRepository : MovieRepository {
    val queries = mutableListOf<String>()

    override fun search(query: String): Flow<PagingData<Movie>> {
        queries += query
        return flowOf(PagingData.empty())
    }

    override suspend fun getDetails(imdbId: String): MovieDetails = error("Not used")
}

private class EmptyFavoritesRepository : FavoritesRepository {
    override fun observeFavorites(): Flow<List<Movie>> = emptyFlow()
    override fun observeFavoriteIds(): Flow<Set<String>> = emptyFlow()
    override fun observeIsFavorite(imdbId: String): Flow<Boolean> = emptyFlow()
    override suspend fun remove(imdbId: String) = Unit
    override suspend fun toggle(movie: Movie) = Unit
}

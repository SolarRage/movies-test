package com.myarulin.movies_test.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.myarulin.movies_test.domain.model.Movie
import com.myarulin.movies_test.domain.repository.FavoritesRepository
import com.myarulin.movies_test.domain.repository.MovieRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val movieRepository: MovieRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val submittedQuery: StateFlow<String> = _query
        .debounce(SEARCH_DEBOUNCE_MS)
        .map(String::trim)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val movies = submittedQuery
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(PagingData.empty()) else movieRepository.search(query)
        }
        .cachedIn(viewModelScope)

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun onQueryChanged(query: String) {
        _query.value = query
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch { favoritesRepository.toggle(movie) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 500L
    }
}

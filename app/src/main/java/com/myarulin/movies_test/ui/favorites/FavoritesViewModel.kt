package com.myarulin.movies_test.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myarulin.movies_test.domain.model.Movie
import com.myarulin.movies_test.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val repository: FavoritesRepository,
) : ViewModel() {
    val favorites: StateFlow<List<Movie>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun remove(imdbId: String) {
        viewModelScope.launch { repository.remove(imdbId) }
    }
}

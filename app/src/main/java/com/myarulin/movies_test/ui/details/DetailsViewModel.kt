package com.myarulin.movies_test.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myarulin.movies_test.domain.model.Movie
import com.myarulin.movies_test.domain.model.MovieDetails
import com.myarulin.movies_test.domain.repository.FavoritesRepository
import com.myarulin.movies_test.domain.repository.MovieRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailsUiState {
    data object Loading : DetailsUiState

    data class Content(val movie: MovieDetails) : DetailsUiState

    data class Error(val message: String) : DetailsUiState
}

class DetailsViewModel(
    private val imdbId: String,
    private val movieRepository: MovieRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val state: StateFlow<DetailsUiState> = _state.asStateFlow()

    val isFavorite: StateFlow<Boolean> = favoritesRepository.observeIsFavorite(imdbId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun toggleFavorite() {
        val details = (_state.value as? DetailsUiState.Content)?.movie ?: return
        viewModelScope.launch { favoritesRepository.toggle(details.toMovie()) }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = DetailsUiState.Loading
            _state.value = try {
                DetailsUiState.Content(movieRepository.getDetails(imdbId))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                DetailsUiState.Error(error.message ?: "Unable to load movie details.")
            }
        }
    }
}

private fun MovieDetails.toMovie() = Movie(
    imdbId = imdbId,
    title = title,
    posterUrl = posterUrl,
    type = type,
    year = year,
)

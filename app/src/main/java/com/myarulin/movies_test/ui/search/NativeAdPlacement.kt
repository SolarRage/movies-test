package com.myarulin.movies_test.ui.search

sealed interface SearchRow {
    data class MovieRow(val movieIndex: Int) : SearchRow

    data class NativeAdRow(val slotIndex: Int) : SearchRow
}

fun searchDisplayItemCount(movieCount: Int): Int = when {
    movieCount <= 0 -> 0
    movieCount == 1 -> 2
    else -> movieCount + 2
}

fun searchRowAt(displayIndex: Int, movieCount: Int): SearchRow {
    require(displayIndex in 0 until searchDisplayItemCount(movieCount))
    return when {
        displayIndex == 1 -> SearchRow.NativeAdRow(slotIndex = 1)
        displayIndex == 3 && movieCount >= 2 -> SearchRow.NativeAdRow(slotIndex = 3)
        displayIndex < 1 -> SearchRow.MovieRow(movieIndex = displayIndex)
        displayIndex < 3 -> SearchRow.MovieRow(movieIndex = displayIndex - 1)
        else -> SearchRow.MovieRow(movieIndex = displayIndex - 2)
    }
}

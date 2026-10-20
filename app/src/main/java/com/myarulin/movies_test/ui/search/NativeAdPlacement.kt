package com.myarulin.movies_test.ui.search

sealed interface SearchRow {
    data class MovieRow(val movieIndex: Int) : SearchRow

    data class NativeAdRow(val slotIndex: Int) : SearchRow
}

fun searchDisplayItemCount(movieCount: Int): Int = when {
    movieCount < MIN_MOVIES_FOR_NATIVE_ADS -> movieCount.coerceAtLeast(0)
    else -> movieCount + NATIVE_AD_COUNT
}

fun searchRowAt(displayIndex: Int, movieCount: Int): SearchRow {
    require(displayIndex in 0 until searchDisplayItemCount(movieCount))
    if (movieCount < MIN_MOVIES_FOR_NATIVE_ADS) return SearchRow.MovieRow(displayIndex)
    return when {
        displayIndex == 1 -> SearchRow.NativeAdRow(slotIndex = 1)
        displayIndex == 3 && movieCount >= 2 -> SearchRow.NativeAdRow(slotIndex = 3)
        displayIndex < 1 -> SearchRow.MovieRow(movieIndex = displayIndex)
        displayIndex < 3 -> SearchRow.MovieRow(movieIndex = displayIndex - 1)
        else -> SearchRow.MovieRow(movieIndex = displayIndex - 2)
    }
}

private const val MIN_MOVIES_FOR_NATIVE_ADS = 3
private const val NATIVE_AD_COUNT = 2

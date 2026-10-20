package com.myarulin.movies_test.ui.search

sealed interface SearchRow {
    data class MovieRow(val movieIndex: Int) : SearchRow

    data class NativeAdRow(val slotIndex: Int) : SearchRow
}

internal const val MIN_MOVIES_FOR_NATIVE_ADS = 3
internal val NATIVE_AD_POSITIONS = setOf(1, 3)

fun nativeAdPositions(movieCount: Int): Set<Int> =
    if (movieCount >= MIN_MOVIES_FOR_NATIVE_ADS) NATIVE_AD_POSITIONS else emptySet()

fun searchDisplayItemCount(
    movieCount: Int,
    adPositions: Set<Int> = nativeAdPositions(movieCount),
): Int = movieCount.coerceAtLeast(0) + adPositions.size

fun searchRowAt(
    displayIndex: Int,
    movieCount: Int,
    adPositions: Set<Int> = nativeAdPositions(movieCount),
): SearchRow {
    require(displayIndex in 0 until searchDisplayItemCount(movieCount, adPositions))
    return if (displayIndex in adPositions) {
        SearchRow.NativeAdRow(slotIndex = displayIndex)
    } else {
        val precedingAds = adPositions.count { it < displayIndex }
        SearchRow.MovieRow(movieIndex = displayIndex - precedingAds)
    }
}

package com.myarulin.movies_test.ui.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NativeAdPlacementTest {
    @Test
    fun `empty movie list has no rows`() {
        assertThat(searchDisplayItemCount(0)).isEqualTo(0)
    }

    @Test
    fun `one movie does not add native ads`() {
        assertThat(searchDisplayItemCount(1)).isEqualTo(1)
        assertThat(searchRowAt(0, 1)).isEqualTo(SearchRow.MovieRow(0))
    }

    @Test
    fun `two movies do not add native ads`() {
        assertThat((0 until searchDisplayItemCount(2)).map { searchRowAt(it, 2) })
            .containsExactly(
                SearchRow.MovieRow(0),
                SearchRow.MovieRow(1),
            )
            .inOrder()
    }

    @Test
    fun `three movies add native ads at indexes one and three`() {
        assertThat((0 until searchDisplayItemCount(3)).map { searchRowAt(it, 3) })
            .containsExactly(
                SearchRow.MovieRow(0),
                SearchRow.NativeAdRow(1),
                SearchRow.MovieRow(1),
                SearchRow.NativeAdRow(3),
                SearchRow.MovieRow(2),
            )
            .inOrder()
    }

    @Test
    fun `movie order remains stable after ad insertion`() {
        val movieIndexes = (0 until searchDisplayItemCount(5))
            .map { searchRowAt(it, 5) }
            .filterIsInstance<SearchRow.MovieRow>()
            .map(SearchRow.MovieRow::movieIndex)

        assertThat(movieIndexes).containsExactly(0, 1, 2, 3, 4).inOrder()
    }

    @Test
    fun `failed ad can be removed without leaving an empty row`() {
        val loadedPositions = setOf(3)

        assertThat((0 until searchDisplayItemCount(3, loadedPositions)).map {
            searchRowAt(it, 3, loadedPositions)
        }).containsExactly(
            SearchRow.MovieRow(0),
            SearchRow.MovieRow(1),
            SearchRow.MovieRow(2),
            SearchRow.NativeAdRow(3),
        ).inOrder()
    }
}

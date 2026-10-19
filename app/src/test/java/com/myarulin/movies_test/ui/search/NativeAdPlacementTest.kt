package com.myarulin.movies_test.ui.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NativeAdPlacementTest {
    @Test
    fun `empty movie list has no rows`() {
        assertThat(searchDisplayItemCount(0)).isEqualTo(0)
    }

    @Test
    fun `one movie adds native ad at index one`() {
        assertThat(searchDisplayItemCount(1)).isEqualTo(2)
        assertThat(searchRowAt(0, 1)).isEqualTo(SearchRow.MovieRow(0))
        assertThat(searchRowAt(1, 1)).isEqualTo(SearchRow.NativeAdRow(1))
    }

    @Test
    fun `two movies add native ads at indexes one and three`() {
        assertThat((0 until searchDisplayItemCount(2)).map { searchRowAt(it, 2) })
            .containsExactly(
                SearchRow.MovieRow(0),
                SearchRow.NativeAdRow(1),
                SearchRow.MovieRow(1),
                SearchRow.NativeAdRow(3),
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
}

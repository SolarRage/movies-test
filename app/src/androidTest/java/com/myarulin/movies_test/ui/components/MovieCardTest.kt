package com.myarulin.movies_test.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.myarulin.movies_test.domain.model.Movie
import org.junit.Rule
import org.junit.Test

class MovieCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysMovieAndFavoriteAction() {
        composeRule.setContent {
            MaterialTheme {
                MovieCard(
                    movie = Movie("tt1", "Movie title", null, "movie", "2026"),
                    isFavorite = false,
                    onClick = {},
                    onFavoriteClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Movie title").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add to favorites").assertIsDisplayed()
    }
}

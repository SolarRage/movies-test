package com.myarulin.movies_test.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteMovieDaoTest {
    private lateinit var database: MoviesDatabase
    private lateinit var dao: FavoriteMovieDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MoviesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.favoriteMovieDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun favoritesAreSortedNewestFirst() = runTest {
        dao.upsert(movie("old", addedAt = 1))
        dao.upsert(movie("new", addedAt = 2))

        assertThat(dao.observeAll().first().map { it.imdbId })
            .containsExactly("new", "old")
            .inOrder()
    }

    @Test
    fun twoConcurrentTogglesRestoreOriginalState() = runTest {
        val favorite = movie("same", addedAt = 1)

        coroutineScope {
            List(2) {
                async(Dispatchers.IO) { dao.toggle(favorite) }
            }.awaitAll()
        }

        assertThat(dao.exists(favorite.imdbId)).isFalse()
    }

    private fun movie(id: String, addedAt: Long) = FavoriteMovieEntity(
        imdbId = id,
        title = id,
        posterUrl = null,
        type = "movie",
        year = "2026",
        addedAt = addedAt,
    )
}

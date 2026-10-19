package com.myarulin.movies_test.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_movies")
data class FavoriteMovieEntity(
    @PrimaryKey val imdbId: String,
    val title: String,
    val posterUrl: String?,
    val type: String,
    val year: String,
    val addedAt: Long,
)

package com.myarulin.movies_test.di

import androidx.room.Room
import com.myarulin.movies_test.BuildConfig
import com.myarulin.movies_test.data.local.MoviesDatabase
import com.myarulin.movies_test.data.remote.OmdbApi
import com.myarulin.movies_test.data.repository.FavoritesRepositoryImpl
import com.myarulin.movies_test.data.repository.MovieRepositoryImpl
import com.myarulin.movies_test.domain.repository.FavoritesRepository
import com.myarulin.movies_test.domain.repository.MovieRepository
import com.myarulin.movies_test.ui.details.DetailsViewModel
import com.myarulin.movies_test.ui.favorites.FavoritesViewModel
import com.myarulin.movies_test.ui.search.SearchViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {
    single(named(OMDB_API_KEY)) { BuildConfig.OMDB_API_KEY }

    single {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }
    single {
        Retrofit.Builder()
            .baseUrl(OMDB_BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    single { get<Retrofit>().create(OmdbApi::class.java) }

    single {
        Room.databaseBuilder(
            androidContext(),
            MoviesDatabase::class.java,
            DATABASE_NAME,
        ).build()
    }
    single { get<MoviesDatabase>().favoriteMovieDao() }

    single<MovieRepository> {
        MovieRepositoryImpl(
            api = get(),
            apiKey = get(named(OMDB_API_KEY)),
        )
    }
    single<FavoritesRepository> { FavoritesRepositoryImpl(dao = get()) }

    viewModel { SearchViewModel(movieRepository = get(), favoritesRepository = get()) }
    viewModel { FavoritesViewModel(repository = get()) }
    viewModel { parameters ->
        DetailsViewModel(
            imdbId = parameters.get(),
            movieRepository = get(),
            favoritesRepository = get(),
        )
    }
}

private const val OMDB_API_KEY = "omdbApiKey"
private const val OMDB_BASE_URL = "https://www.omdbapi.com/"
private const val DATABASE_NAME = "movies.db"

package com.myarulin.movies_test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.myarulin.movies_test.ui.theme.MoviestestTheme
import com.myarulin.movies_test.ui.navigation.MovieApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoviestestTheme {
                MovieApp()
            }
        }
    }
}

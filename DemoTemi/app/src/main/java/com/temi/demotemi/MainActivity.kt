package com.temi.demotemi

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.temi.demotemi.ui.theme.DemoTemiTheme
import dagger.hilt.android.AndroidEntryPoint

@Composable
fun Gif(imageId: Int) {
    // Determine the image resource based on shouldPlayGif
    val gifEnabledLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    AsyncImage(
        model = imageId, // Resource or URL of the image/GIF
        contentDescription = "Animated GIF",
        imageLoader = gifEnabledLoader,
        modifier = Modifier
            .fillMaxSize() // Fill the whole screen
            .pointerInput(Unit) {
            },
        contentScale = ContentScale.Crop // Crop to fit the entire screen
    )
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
           override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)

        setContent {
            DemoTemiTheme {
                // Initialize the navigation controller
                val navController = rememberNavController()
                val viewModel: MainViewModel = hiltViewModel()



                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val currentScreen by viewModel.currentScreen.collectAsState()

                    // Navigation Host
                    NavHost(
                        navController = navController,
                        startDestination = currentScreen.name,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // Define the screens (composable destinations)
                        composable(route = Screen.Home.name) {
                            HomeScreen(
                                navController,
                                viewModel
                            )  // Passing viewModel here
                        }
                        composable(route = Screen.Play_music.name) {
                            PlayMusicScreen(
                                navController,
                                viewModel
                            )  // Passing viewModel here
                        }
                    }
                }
            }
        }
    }
 }

@Composable
fun HomeScreen(navController: NavController, viewModel: MainViewModel) {

    Column(
        modifier = Modifier
            .fillMaxSize() // Fill the whole screen
            .wrapContentSize(Alignment.Center) // Center the content
    ) {
        Button(onClick = { viewModel.setCurrentScreen(Screen.Play_music) }) {
            Text(text = "Click Me")
        }
    }
}

@Composable
fun PlayMusicScreen(navController: NavController, viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize() // Fill the whole screen
            .wrapContentSize(Alignment.Center) // Center the content
    ) {
        Button(onClick = { viewModel.themeMusic.play()  }) {
            Text(text = "Click Me!")
        }
    }
}
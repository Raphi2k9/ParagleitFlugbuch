package com.example.flugbuch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.flugbuch.navigation.NavGraph
import com.example.flugbuch.ui.theme.FlugbuchTheme
import com.example.flugbuch.viewmodel.FlightViewModel
import com.example.flugbuch.viewmodel.StatisticsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlugbuchTheme {
                FlugbuchApp()
            }
        }
    }
}

@Composable
fun FlugbuchApp() {
    val navController = rememberNavController()
    val flightViewModel: FlightViewModel = viewModel()
    val statisticsViewModel: StatisticsViewModel = viewModel()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavGraph(
            navController = navController,
            flightViewModel = flightViewModel,
            statisticsViewModel = statisticsViewModel
        )
    }
}

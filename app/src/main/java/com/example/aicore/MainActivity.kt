package com.example.aicore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    // The NavController handles screen swapping
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = TimerScreenDestination
                    ) {

                        // --- SCREEN 1 ROUTE ---
                        composable<TimerScreenDestination> {
                            TimerScreen(
                                onTimerFinished = {
                                    // When the timer finishes, swap to Screen 2
                                    navController.navigate(QuoteScreenDestination)
                                }
                            )
                        }

                        // --- SCREEN 2 ROUTE (The Chat Screen) ---
//                        composable<ChatScreenDestination> {
//                            ChatScreen(
//                                onNavigateBack = {
//                                    navController.popBackStack() // Go back to timer
//                                },
//                                onNavigateToQuote = {
//                                    navController.navigate(QuoteScreenDestination) // Go to Screen 3
//                                }
//                            )
//                        }

                        // --- SCREEN 3 ROUTE (Placeholder for now) ---
                        // --- SCREEN 3 ROUTE (The new Quote Screen) ---
                        composable<QuoteScreenDestination> {
                            QuoteScreen(
                                onNavigateBack = {
                                    // Pops back to the Timer Screen
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
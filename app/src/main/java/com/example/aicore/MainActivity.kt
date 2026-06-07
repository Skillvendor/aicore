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
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = TimerScreenDestination
                    ) {
                        // --- SCREEN 1: TIMER ---
                        composable<TimerScreenDestination> {
                            TimerScreen(
                                onTimerFinished = {
                                    // Timer now goes to Chat Screen!
                                    navController.navigate(ChatScreenDestination) {
                                        // Optional: Clear the timer so we can't 'back' into it
                                        popUpTo(TimerScreenDestination) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // --- SCREEN 2: AI CHAT ---
                        composable<ChatScreenDestination> {
                            ChatScreen(
                                onNavigateToQuote = {
                                    navController.navigate(QuoteScreenDestination)
                                }
                            )
                        }

                        // --- SCREEN 3: QUOTE PERSISTENCE ---
                        composable<QuoteScreenDestination> {
                            QuoteScreen(
                                onNavigateBack = {
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
package com.example.aicore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TimerScreen(onTimerFinished: () -> Unit) {

    // "remember" keeps these values alive across UI redraws (recompositions)
    var timeLeft by remember { mutableIntStateOf(3) }
    var isRunning by remember { mutableStateOf(false) }

    // This gives us a CoroutineScope tied to this specific UI component's lifecycle
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isRunning || timeLeft < 3) "Time left: $timeLeft" else "Ready to start",
            fontSize = 32.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (!isRunning) {
                    // 1. Ensure state is clean at the start
                    timeLeft = 3
                    isRunning = true

                    // We launch the coroutine here! It immediately hops off the Main Thread
                    scope.launch {
                        while (timeLeft > 0) {
                            delay(1000L) // Suspend for 1 second safely
                            timeLeft--   // Update the state (triggers a UI redraw)
                        }

                        // 2. Time is up! Stop running FIRST, so the UI state is stable
                        isRunning = false

                        // 3. Execute the callback to navigate
                        onTimerFinished()
                    }
                }
            },
            enabled = !isRunning // Disable the button while the timer is running
        ) {
            // Update the button text to make sense if they come back to this screen
            Text(if (isRunning) "Counting..." else if (timeLeft == 0) "Restart Timer" else "Start 3s Timer")
        }
    }
}
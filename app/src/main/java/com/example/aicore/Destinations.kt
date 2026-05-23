package com.example.aicore

import kotlinx.serialization.Serializable

/**
 * These represent our 3 screens.
 */

// Screen 1: The Coroutine Timer Screen
@Serializable
object TimerScreenDestination

// Screen 2: The On-Device Gemini Chat Screen
@Serializable
object ChatScreenDestination

// Screen 3: The Cancellable Network Call / Quote Screen
@Serializable
object QuoteScreenDestination
package com.example.aicore

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.platform.LocalDensity

// --- 1. STATE DEFINITIONS ---
sealed class ModelStatus {
    object Checking : ModelStatus()
    data class Downloading(val bytesDownloaded: Long) : ModelStatus()
    object Ready : ModelStatus()
    data class Error(val message: String) : ModelStatus()
}

data class ChatMessage(val text: String, val isUser: Boolean)

// --- 2. VIEWMODEL ---
class ChatViewModel : ViewModel() {

    private val _modelStatus = MutableStateFlow<ModelStatus>(ModelStatus.Checking)
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val generativeModel: GenerativeModel = Generation.getClient()

    init {
        checkAndDownloadModel()
    }

    private fun checkAndDownloadModel() {
        viewModelScope.launch {
            _modelStatus.value = ModelStatus.Checking

            try {
                val status = generativeModel.checkStatus()

                when (status) {
                    FeatureStatus.AVAILABLE -> {
                        _modelStatus.value = ModelStatus.Ready
                        addSystemMessage("ML Kit: Local model is ready.")
                    }
                    FeatureStatus.DOWNLOADABLE -> {
                        generativeModel.download().collect { downloadStatus ->
                            when (downloadStatus) {
                                is DownloadStatus.DownloadStarted -> _modelStatus.value = ModelStatus.Downloading(0L)
                                is DownloadStatus.DownloadProgress -> _modelStatus.value = ModelStatus.Downloading(downloadStatus.totalBytesDownloaded)
                                is DownloadStatus.DownloadCompleted -> {
                                    _modelStatus.value = ModelStatus.Ready
                                    addSystemMessage("ML Kit: Download complete.")
                                }
                                is DownloadStatus.DownloadFailed -> _modelStatus.value = ModelStatus.Error("Download failed.")
                            }
                        }
                    }
                    FeatureStatus.DOWNLOADING -> _modelStatus.value = ModelStatus.Downloading(0L)
                    FeatureStatus.UNAVAILABLE -> _modelStatus.value = ModelStatus.Error("Gemini Nano is unavailable.")
                }
            } catch (e: Exception) {
                _modelStatus.value = ModelStatus.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        viewModelScope.launch {
            _messages.value = _messages.value + ChatMessage(userText, true)
            _isTyping.value = true

            try {
                _messages.value = _messages.value + ChatMessage("", false)

                withContext(Dispatchers.IO) {
                    val stream = generativeModel.generateContentStream(userText)

                    stream.collect { chunk ->
                        _isTyping.value = false
                        val chunkText = chunk.candidates.firstOrNull()?.text ?: ""
                        val currentList = _messages.value.toMutableList()
                        val lastIndex = currentList.lastIndex
                        val currentBotMessage = currentList[lastIndex]

                        currentList[lastIndex] = currentBotMessage.copy(
                            text = currentBotMessage.text + chunkText
                        )
                        _messages.value = currentList
                    }
                }
            } catch (e: Exception) {
                _isTyping.value = false
                _messages.value = _messages.value + ChatMessage("Error: ${e.localizedMessage}", false)
            }
        }
    }

    private fun addSystemMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(text, false)
    }
}

// --- 3. UI COMPONENT ---
@Composable
fun ChatScreen(
    onNavigateToQuote: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val status by viewModel.modelStatus.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("ML Kit Local Chat") },
                actions = {
                    IconButton(onClick = onNavigateToQuote) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Go to Screen 3")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = status,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "ModelStatusAnimation"
            ) { currentState ->
                when (currentState) {
                    is ModelStatus.Checking -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ModelStatus.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            val mbDownloaded = currentState.bytesDownloaded / (1024 * 1024)
                            Text("Downloading: $mbDownloaded MB", color = Color.Gray)
                        }
                    }
                    is ModelStatus.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(currentState.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is ModelStatus.Ready -> {
                        ChatInterface(
                            messages = messages,
                            isTyping = isTyping,
                            onSendMessage = { viewModel.sendMessage(it) }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun ChatInterface(
    messages: List<ChatMessage>,
    isTyping: Boolean,
    onSendMessage: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val density = LocalDensity.current
    val imeHeight = WindowInsets.ime.getBottom(density)

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length, isTyping, imeHeight) {
        if (messages.isNotEmpty() || isTyping) {
            listState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            reverseLayout = true
        ) {
            if (isTyping) {
                item {
                    Text("Model is thinking...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            items(messages.reversed()) { message ->
                ChatBubble(message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask the local model...") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    onSendMessage(textInput)
                    textInput = ""
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .padding(12.dp)
        ) {
            Text(text = message.text, color = textColor)
        }
    }
}
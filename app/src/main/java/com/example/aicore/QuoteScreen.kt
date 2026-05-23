package com.example.aicore

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET

// --- 1. NETWORK LAYER (Retrofit) ---
@Serializable
data class QuoteResponse(val quote: String, val author: String)

interface QuoteApi {
    @GET("quotes/random")
    suspend fun getRandomQuote(): QuoteResponse
}

// We instantiate Retrofit to hit dummyjson.com, which gives fast, reliable JSON data
val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl("https://dummyjson.com/")
    .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
    .build()

val quoteApi: QuoteApi = retrofit.create(QuoteApi::class.java)


// --- 2. PERSISTENCE LAYER (Jetpack DataStore) ---
// This creates a singleton file on disk called "quote_prefs.preferences_pb"
val Context.dataStore by preferencesDataStore(name = "quote_prefs")
val SAVED_QUOTE_KEY = stringPreferencesKey("saved_quote")


// --- 3. VIEWMODEL (State & Logic) ---
// We use AndroidViewModel here instead of regular ViewModel because DataStore requires the App Context to read/write files.
class QuoteViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.dataStore

    // DataStore inherently returns a Kotlin Flow! We map it to extract our string.
    // If the app is opened for the first time, it defaults to the message below.
    val savedQuoteFlow = dataStore.data.map { preferences ->
        preferences[SAVED_QUOTE_KEY] ?: "No quote saved yet. Fetch one!"
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // This Job variable is our "kill switch". We store the active network coroutine here.
    private var fetchJob: Job? = null

    fun fetchNewQuote() {
        // Prevent multiple simultaneous clicks
        if (_isLoading.value) return

        _isLoading.value = true

        // Assign the coroutine to our fetchJob variable
        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // I added a fake 2-second delay so you have enough time to click the "Cancel" button!
                delay(2000)

                // The actual network call
                val response = quoteApi.getRandomQuote()
                val formattedQuote = "\"${response.quote}\" \n— ${response.author}"

                // Save it to persistent disk memory
                dataStore.edit { preferences ->
                    preferences[SAVED_QUOTE_KEY] = formattedQuote
                }
            } catch (e: Exception) {
                // If the user clicked cancel, a CancellationException is thrown here automatically.
                // We just let it swallow silently, or log it.
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelFetch() {
        // Instantly interrupts the delay() or network socket connection!
        fetchJob?.cancel()
        _isLoading.value = false
    }
}


// --- 4. UI LAYER (Compose) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuoteViewModel = viewModel()
) {
    // Observe state
    val savedQuote by viewModel.savedQuoteFlow.collectAsState(initial = "Loading memory...")
    val isLoading by viewModel.isLoading.collectAsState()

    // Grab the system Context so we can use the Clipboard manager
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Quote") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // The Persistent Memory Display
            Text(
                text = "Last Saved Quote:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = savedQuote,
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.cancelFetch() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Interrupt API Call")
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { viewModel.fetchNewQuote() }) {
                        Text("Fetch New Quote")
                    }

                    Button(
                        onClick = {
                            // Copy to clipboard logic
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("quote", savedQuote)
                            clipboardManager.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Copy Quote")
                    }
                }
            }
        }
    }
}
package com.example.network_logger_lib

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.network_logger_lib.api.NewPost
import com.example.network_logger_lib.ui.theme.NetworkloggerlibTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val demoApi = DemoApplication.get(application).demoApi

        setContent {
            NetworkloggerlibTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DemoScreen(
                        modifier = Modifier.padding(innerPadding),
                        onSuccessClick = { demoApi.getSuccess() },
                        onFailureClick = { demoApi.getFailure() },
                        onPostClick = {
                            demoApi.createPost(
                                NewPost(
                                    title = "Network Logger Demo",
                                    body = "POST body captured by the interceptor",
                                    userId = 1,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoScreen(
    modifier: Modifier = Modifier,
    onSuccessClick: suspend () -> Unit,
    onFailureClick: suspend () -> Unit,
    onPostClick: suspend () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf("Tap a button to fire a network request") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Network Logger Demo",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Open the web dashboard (localhost:8080 via ADB) to inspect traffic in real time.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(text = statusMessage, style = MaterialTheme.typography.bodySmall)

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    runCatching { onSuccessClick() }
                        .onSuccess { statusMessage = "GET /posts/1 succeeded" }
                        .onFailure { statusMessage = "Error: ${it.message}" }
                }
            },
        ) {
            Text("GET – Success (200)")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    runCatching { onFailureClick() }
                        .onSuccess { statusMessage = "Unexpected success" }
                        .onFailure {
                            statusMessage = "GET /posts/99999 failed (expected 404)"
                            Toast.makeText(context, it.message ?: "Request failed", Toast.LENGTH_SHORT).show()
                        }
                }
            },
        ) {
            Text("GET – Failure (404)")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    runCatching { onPostClick() }
                        .onSuccess { statusMessage = "POST /posts succeeded" }
                        .onFailure { statusMessage = "Error: ${it.message}" }
                }
            },
        ) {
            Text("POST – Create Post")
        }
    }
}

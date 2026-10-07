package com.allcalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allcalc.app.ui.navigation.AllCalcApp
import com.allcalc.app.ui.theme.AllCalcTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as AllCalcApplication).container
        val initialTheme =
            runCatching { runBlocking { container.preferences.theme.first() } }.getOrDefault(0)

        setContent {
            val theme by container.preferences.theme.collectAsStateWithLifecycle(initialValue = initialTheme)
            val darkTheme = when (theme) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            AllCalcTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AllCalcApp()
                        CrashReporter()
                    }
                }
            }
        }
    }
}

@Composable
private fun CrashReporter() {
    val context = LocalContext.current
    val crashFile = remember { File(context.applicationContext.filesDir, "crash.txt") }
    var trace by remember { mutableStateOf(if (crashFile.exists()) crashFile.readText() else null) }
    if (trace != null) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "Last crash",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = trace.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        crashFile.delete()
                        trace = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Clear and continue") }
            }
        }
    }
}
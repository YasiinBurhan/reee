package com.equinox.virtual.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equinox.virtual.BcoreDiagnosticsRunner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit
) {
    var runResult by remember { mutableStateOf<BcoreDiagnosticsRunner.DiagnosticRunResult?>(null) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isRunning = true
        runResult = BcoreDiagnosticsRunner.runDiagnostics()
        isRunning = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BCORE Runtime Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isRunning = true
                            runResult = BcoreDiagnosticsRunner.runDiagnostics()
                            isRunning = false
                        },
                        enabled = !isRunning
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run Diagnostics")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val logs = runResult?.logs ?: emptyList()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(logs) { line ->
                        val isHeader = line.startsWith("[") && line.contains("]") && !line.contains("|")
                        val isFail = line.contains("FAIL") || line.contains("Exception") || line.contains("ERROR")
                        val isPass = line.contains("PASS") || line.contains("SUCCESS")

                        val color = when {
                            isFail -> Color(0xFFE53935)
                            isPass -> Color(0xFF43A047)
                            isHeader -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onBackground
                        }

                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                            color = color
                        )
                    }
                }
            }
        }
    }
}

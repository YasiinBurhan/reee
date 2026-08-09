package com.equinox.virtual

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.equinox.virtual.ui.BlackBoxMainScreen
import com.equinox.virtual.ui.theme.MyApplicationTheme
import com.equinox.virtual.viewmodel.BlackBoxViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BlackBoxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkThemePref by viewModel.isDarkTheme.collectAsState()
            val useDarkTheme = isDarkThemePref ?: androidx.compose.foundation.isSystemInDarkTheme()
            val currentUserSession by viewModel.currentUserSession.collectAsState()
            val isCheckingSession by viewModel.isCheckingSession.collectAsState()

            val isTampered = androidx.compose.runtime.remember { com.equinox.virtual.helper.AntiTamper.isAppTampered(applicationContext) }
            if (isTampered) {
                throw SecurityException("App integrity check failed!")
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                if (isCheckingSession) {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxSize()
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                } else if (currentUserSession == null) {
                    com.equinox.virtual.ui.LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = {}
                    )
                } else {
                    BlackBoxMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

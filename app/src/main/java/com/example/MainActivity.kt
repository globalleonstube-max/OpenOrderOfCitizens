package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.DiagnosticsTracker
import com.example.presentation.OpenOrderSocialOSApp
import com.example.presentation.SuiteViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SuiteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize persistent error diagnostics tracking tool
        DiagnosticsTracker.initialize(applicationContext)
        DiagnosticsTracker.logInfo("MainActivity", "App started up successfully on native runtime.")

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                 OpenOrderSocialOSApp(viewModel = viewModel)
            }
        }
    }
}

package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.MMAssistantScreen
import com.example.ui.theme.MMAssistantTheme
import com.example.viewmodel.MMAssistantViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MMAssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val sassyMood by viewModel.currentSassyMood.collectAsState()

            MMAssistantTheme(sassyMood = sassyMood) {
                val permissionsToRequest = buildList {
                    add(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    add(Manifest.permission.READ_CONTACTS)
                    add(Manifest.permission.CALL_PHONE)
                    add(Manifest.permission.READ_PHONE_STATE)
                    add(Manifest.permission.READ_CALL_LOG)
                }.toTypedArray()

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    val micGranted = results[Manifest.permission.RECORD_AUDIO] ?: false
                    if (micGranted) {
                        viewModel.startBackgroundService()
                    }
                }

                LaunchedEffect(Unit) {
                    val micGranted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!micGranted) {
                        permissionLauncher.launch(permissionsToRequest)
                    } else {
                        viewModel.startBackgroundService()
                    }
                }

                MMAssistantScreen(
                    viewModel = viewModel,
                    onRequestPermissions = {
                        permissionLauncher.launch(permissionsToRequest)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkBatteryOptimizationStatus()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val wasWakeTriggered = intent.getBooleanExtra("WAKE_TRIGGERED", false)
        if (wasWakeTriggered) {
            viewModel.startBackgroundService()
        }
    }
}

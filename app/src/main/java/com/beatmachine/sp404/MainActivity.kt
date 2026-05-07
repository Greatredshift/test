package com.beatmachine.sp404

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.beatmachine.sp404.ui.screens.MainScreen
import com.beatmachine.sp404.ui.theme.BeatMachineTheme
import com.beatmachine.sp404.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on and full brightness
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        requestAudioPermissions()

        setContent {
            BeatMachineTheme {
                SampleLoaderBridge(viewModel)
                MainScreen(viewModel = viewModel)
            }
        }
    }

    private fun requestAudioPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 1)
        }
    }
}

@Composable
fun SampleLoaderBridge(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val samplePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val padIndex = uiState.selectedPad ?: 0
            viewModel.loadSample(padIndex, it)
        }
    }

    // Expose launcher so pads can trigger it via long-press → a dedicated Load button
    LaunchedEffect(Unit) {
        viewModel.setSamplePickerLauncher { samplePickerLauncher.launch(arrayOf("audio/*")) }
    }
}

package com.example.econoise.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.econoise.NoiseRecorder

@Composable
fun RecordScreen() {
    val context = LocalContext.current

    var currentDb by rememberSaveable { mutableDoubleStateOf(0.0) }
    var isCalibrated by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        val recorder = NoiseRecorder(context)
        while (!recorder.calibrate()) {
            // repeat until calibrated
        }
        isCalibrated = true

        while(true) {
            val amp = recorder.recordInterval()
            currentDb = amp
        }
    }

    Column {
        Text(
            text = if (isCalibrated) "Calibrated" else "Not calibrated"
        )

        Text(
            text = "$currentDb",
            style = MaterialTheme.typography.headlineLarge
        )
    }
}
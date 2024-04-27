package com.example.econoise

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.MediaRecorder.AudioEncoder
import android.media.MediaRecorder.AudioSource
import android.media.MediaRecorder.OutputFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.example.econoise.ui.components.NoiseMap
import com.example.econoise.ui.theme.EcoNoiseTheme
import kotlinx.coroutines.delay
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.log10
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recorder = NoiseRecorder(this)

        setContent {
            val context = LocalContext.current
            var currentDb by rememberSaveable { mutableDoubleStateOf(0.0) }

//            LaunchedEffect(key1 = true) {
//                val recorder = MediaRecorder(context)
//
//                recorder.setAudioSource(AudioSource.MIC)
//                recorder.setOutputFormat(OutputFormat.THREE_GPP)
//                recorder.setAudioEncoder(AudioEncoder.AMR_NB)
//                recorder.setOutputFile("${externalCacheDir!!.absolutePath}/output.3gp")
//                recorder.prepare()
//
//                recorder.start()
//                recorder.maxAmplitude
//                while (true) {
//                    val amp = recorder.maxAmplitude
//                    val db = 20.0 * log10(amp / 2700.0)
//                    currentDb = db
//                    delay(1000)
//                }
//
//                recorder.stop()
//                recorder.reset()
//                recorder.release()
//            }

            LaunchedEffect(key1 = true) {
                while(true) {
                    val amps = recorder.recordAmplitudes(1)
                    currentDb = amps[0]
                }
            }

            EcoNoiseTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Text(
                        text = "$currentDb",
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }
        }
    }
}
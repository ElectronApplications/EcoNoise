package com.example.econoise

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.core.app.ActivityCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class NoiseRecorder(
    private val context: Context
) {
    private var recorder: AudioRecord

    init {
        recorder = if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw RuntimeException()
        } else {
            AudioRecord(
                MediaRecorder.AudioSource.UNPROCESSED,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_24BIT_PACKED,
                3 * BUFFER_SIZE
            )
        }
    }

    private suspend fun recordRaw(): IntArray {
        val buffer = ByteArray(3 * BUFFER_SIZE)

        recorder.startRecording()
        delay(SAMPLE_INTERVAL_MS.toLong())
        recorder.stop()

        val bytes = recorder.read(buffer, 0, BUFFER_SIZE, AudioRecord.READ_NON_BLOCKING)
        val amps = mutableListOf<Int>()
        for (i in 0..<(bytes / 3)) {
            val index = i * 3
            val value = buffer[index].toInt() + (buffer[index + 1].toInt() shl 8) + (buffer[index + 2].toInt() shl 16)
            amps.add(value)
        }

        return amps.toIntArray()
    }

    suspend fun calibrate(): Boolean {
        val intervals = 5 * 1000 / SAMPLE_INTERVAL_MS

        val maxAmps = (0..<intervals).map { recordRaw().max() }

        val mean = maxAmps.sum().toDouble() / maxAmps.size
        val std = sqrt(maxAmps.sumOf { (mean - it).pow(2) } / maxAmps.size)
        Log.d("hmm", "$std")
        return if (std < 1000) {
            context.dataStore.edit {
                it[intPreferencesKey("calibration")] = maxAmps.min()
            }
            true
        } else {
            false
        }
    }

    suspend fun recordInterval(): Double {
        val calibration = context.dataStore.data.map {
            it[intPreferencesKey("calibration")]
        }.first() ?: throw RuntimeException()

        val amps = recordRaw()

        val maxAmp = amps.max()
        return 20.0 * log10(maxAmp.toDouble() / calibration)
    }

    companion object {
        const val SAMPLE_RATE = 44100
        const val SAMPLE_INTERVAL_MS = 250
        const val BUFFER_SIZE = SAMPLE_RATE * SAMPLE_INTERVAL_MS / 1000
    }

}
package com.example.econoise

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.delay
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

class NoiseRecorder(
    context: Context,
    private var calibrated: Int? = null
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
        val intervals = 10 * 1000 / SAMPLE_INTERVAL_MS

        val maxAmps = (0..<intervals).map { recordRaw().max() }

        val mean = maxAmps.sum().toDouble() / maxAmps.size
        val std = sqrt(maxAmps.sumOf { (mean - it).pow(2) } / maxAmps.size)

        return if (std < 1000) {
            calibrated = maxAmps.min()
            true
        } else {
            false
        }
    }

    suspend fun recordInterval(): Double {
        if (calibrated == null)
            throw RuntimeException()

        val amps = recordRaw()

        val maxAmp = amps.max()
        return 20.0 * log10(maxAmp.toDouble() / calibrated!!)
    }

    companion object {
        const val SAMPLE_RATE = 44100
        const val SAMPLE_INTERVAL_MS = 250
        const val BUFFER_SIZE = SAMPLE_RATE * SAMPLE_INTERVAL_MS / 1000
    }

}
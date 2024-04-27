package com.example.econoise

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.delay
import kotlin.math.log10

class NoiseRecorder(
    context: Context
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
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE * MAX_INTERVAL
            )
        }
    }

    suspend fun recordAmplitudes(amount: Long): List<Double> {
        if (amount > MAX_INTERVAL)
            throw RuntimeException()

        recorder.startRecording()
        delay(amount * SAMPLE_INTERVAL * 1000)
        recorder.stop()

        return (0..<amount).map {
            val buffer = ShortArray(BUFFER_SIZE)
            recorder.read(buffer, 0, BUFFER_SIZE)
            val maxAmp = buffer.max()
            val logScale = 20.0 * log10(maxAmp.toDouble() / 2700.0) // 2700 - просто референс
            logScale
        }.toList()
    }

    companion object {
        const val SAMPLE_RATE = 8000
        const val SAMPLE_INTERVAL = 1
        const val BUFFER_SIZE = 2 * SAMPLE_RATE * SAMPLE_INTERVAL
        const val MAX_INTERVAL = 10
    }

}
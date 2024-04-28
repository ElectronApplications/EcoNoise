package com.example.econoise.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.econoise.NoiseRecorder
import com.example.econoise.api.uploadNoise
import com.example.econoise.dataStore
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun RecordScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val calibration by context.dataStore.data.map {
        it[intPreferencesKey("calibration")]
    }.collectAsState(initial = null)

    LaunchedEffect(key1 = true) {

    }

    var calibrateDialogShown by rememberSaveable { mutableStateOf(false) }
    var calibrateDialogFailed by rememberSaveable { mutableStateOf(false) }

    if (calibrateDialogShown) {
        Dialog(
            onDismissRequest = { /* Ignore */ }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = if (calibrateDialogFailed) "Калибровка не удалась. Попробуйте еще раз" else
                        "Проводится калибровка... Пожалуйста, не шумите!",
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }

    var analysisDialogShown by rememberSaveable { mutableStateOf(false) }
    var analysisCurrentData by rememberSaveable { mutableDoubleStateOf(0.0) }
    if (analysisDialogShown) {
        Dialog(
            onDismissRequest = { /* Ignore */ }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Проводятся замеры...",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = when (analysisCurrentData) {
                            in 0.0..<7.0 -> "Тихо"
                            in 7.0..<20.0 -> "Средне"
                            else -> "Громко"
                        },
                        color = when (analysisCurrentData) {
                            in 0.0..<7.0 -> Color.Green
                            in 7.0..<20.0 -> Color.Yellow
                            else -> Color.Red
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (calibration != null) "Откалибровано" else "Не откалибровано",
            style = MaterialTheme.typography.headlineLarge
        )

        if (calibration == null) {
            Text(text = "Пожалуйста, проведите калибровку в тихом месте")
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    calibrateDialogShown = true
                    calibrateDialogFailed = false

                    val recorder = NoiseRecorder(context)
                    val result = recorder.calibrate()
                    if (!result) {
                        calibrateDialogFailed = true
                        delay(2000)
                    }

                    calibrateDialogShown = false
                }
            }
        ) {
            Text("Откалибровать")
        }

        Button(
            modifier = Modifier.padding(top = 16.dp),
            enabled  = calibration != null,
            onClick = {
                coroutineScope.launch {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        analysisDialogShown = true

                        var location: Location? = null
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

                        val locationRequest = LocationRequest.create()
                            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                            .setInterval(1000)
                            .setFastestInterval(100)

                        fusedLocationClient.requestLocationUpdates(locationRequest, object: LocationCallback() {
                            override fun onLocationResult(loc: LocationResult?) {
                                location = loc?.lastLocation
                            }
                        }, Looper.getMainLooper())

                        val recorder = NoiseRecorder(context)

                        val intervals = 10 * 1000 / NoiseRecorder.SAMPLE_INTERVAL_MS
                        val data = (0..<intervals).map {
                            val current = recorder.recordInterval()
                            analysisCurrentData = current
                            current
                        }.toList()

                        val avg = data.sum() / data.size

                        while (location == null) {
                            delay(1000)
                        }

                        uploadNoise(
                            latitude = location!!.latitude,
                            longitude = location!!.longitude,
                            decibels = avg,
                            frequency = 0.0
                        )

                        analysisDialogShown = false
                    }
                }
            }
        ) {
            Text("Сделать замеры")
        }
    }
}
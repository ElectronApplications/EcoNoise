package com.example.econoise.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetNoiseAtApiResponse(
    @SerialName("average_decibels") val averageDecibels: Double,
    @SerialName("search_radius") val searchRadius: Double
)

@Serializable
data class GetNoisesListApiResponse(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val decibels: Double,
    val frequency: Double,
    val datetime: Instant
)

suspend fun getNoiseAt(latitude: Double, longitude: Double): GetNoiseAtApiResponse {
    val result = httpClient.get {
        url {
            appendPathSegments("api", "noise")
            parameters.append("latitude", "$latitude")
            parameters.append("longitude", "$longitude")
        }
    }

    return result.body()
}

suspend fun getNoisesList(): List<GetNoisesListApiResponse> {
    val result = httpClient.get {
        url {
            appendPathSegments("api", "noises")
        }
    }

    return result.body()
}
package com.example.econoise.api

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class UploadNoiseApiRequest(
    val latitude: Double,
    val longitude: Double,
    val decibels: Double,
    val frequency: Double,
    val datetime: Instant
)

suspend fun uploadNoise(latitude: Double, longitude: Double, decibels: Double, frequency: Double) {
    httpClient.post {
        url {
            appendPathSegments("api", "noises/")
        }
        contentType(ContentType.Application.Json)
        setBody(
            UploadNoiseApiRequest(
                latitude = latitude,
                longitude = longitude,
                decibels = decibels,
                frequency = frequency,
                datetime = Clock.System.now().toLocalDateTime(TimeZone.UTC).toInstant(TimeZone.UTC)
            )
        )
    }
}
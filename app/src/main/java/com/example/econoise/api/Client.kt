package com.example.econoise.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json

val httpClient = HttpClient(CIO) {
    defaultRequest {
        url("https://2106-92-51-45-202.ngrok-free.app")
    }
    install(ContentNegotiation) {
        json()
    }
}
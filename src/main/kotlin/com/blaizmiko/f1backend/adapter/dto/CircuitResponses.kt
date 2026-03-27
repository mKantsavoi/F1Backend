package com.blaizmiko.f1backend.adapter.dto

import kotlinx.serialization.Serializable

@Serializable
data class CircuitsResponse(
    val circuits: List<CircuitDto>,
)

@Serializable
data class CircuitDto(
    val circuitId: String,
    val name: String,
    val locality: String,
    val country: String,
    val lat: Double,
    val lng: Double,
    val url: String,
)

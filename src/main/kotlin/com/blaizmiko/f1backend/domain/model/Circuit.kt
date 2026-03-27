package com.blaizmiko.f1backend.domain.model

data class Circuit(
    val id: String,
    val name: String,
    val locality: String,
    val country: String,
    val lat: Double,
    val lng: Double,
    val url: String,
)

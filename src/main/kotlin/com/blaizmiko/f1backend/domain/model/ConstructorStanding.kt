package com.blaizmiko.f1backend.domain.model

data class ConstructorStanding(
    val position: Int,
    val teamId: String,
    val teamName: String,
    val nationality: String,
    val points: Double,
    val wins: Int,
)

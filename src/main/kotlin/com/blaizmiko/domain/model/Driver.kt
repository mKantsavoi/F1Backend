package com.blaizmiko.domain.model

import java.time.LocalDate

data class Driver(
    val id: String,
    val number: Int,
    val code: String,
    val firstName: String,
    val lastName: String,
    val nationality: String,
    val dateOfBirth: LocalDate?,
)

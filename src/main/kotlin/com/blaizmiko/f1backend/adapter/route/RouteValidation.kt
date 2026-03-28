package com.blaizmiko.f1backend.adapter.route

import com.blaizmiko.f1backend.domain.model.FIRST_F1_SEASON
import com.blaizmiko.f1backend.domain.model.ValidationException
import java.time.Year

fun validateSeason(season: String) {
    if (season == "current") return
    val year = season.toIntOrNull()
    val currentYear = Year.now().value
    if (year == null || year < FIRST_F1_SEASON || year > currentYear) {
        throw ValidationException("Invalid season parameter: must be a year between $FIRST_F1_SEASON and $currentYear")
    }
}

private const val MAX_ROUNDS_PER_SEASON = 99

fun validateRound(round: String) {
    val r = round.toIntOrNull()
    if (r == null || r < 1 || r > MAX_ROUNDS_PER_SEASON) {
        throw ValidationException(
            "Invalid round parameter: must be a positive integer between 1 and $MAX_ROUNDS_PER_SEASON",
        )
    }
}

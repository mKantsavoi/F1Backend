package com.blaizmiko.f1backend.adapter.route

import com.blaizmiko.f1backend.domain.model.FIRST_F1_SEASON
import com.blaizmiko.f1backend.domain.model.ValidationException
import java.time.Year

fun validateSeason(season: String) {
    val year = season.toIntOrNull()
    val currentYear = Year.now().value
    if (year == null || year < FIRST_F1_SEASON || year > currentYear) {
        throw ValidationException("Invalid season parameter: must be a year between $FIRST_F1_SEASON and $currentYear")
    }
}

package com.blaizmiko.f1backend.infrastructure.external

import com.blaizmiko.f1backend.domain.model.Circuit
import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.model.FastestLap
import com.blaizmiko.f1backend.domain.model.QualifyingResult
import com.blaizmiko.f1backend.domain.model.RaceResult
import com.blaizmiko.f1backend.domain.model.RaceWeekend
import com.blaizmiko.f1backend.domain.model.Sessions
import com.blaizmiko.f1backend.domain.model.Team
import java.time.LocalDate

internal fun JolpicaDriver.toDomain(): Driver =
    Driver(
        id = driverId,
        number = permanentNumber.toIntOrNull() ?: 0,
        code = code,
        firstName = givenName,
        lastName = familyName,
        nationality = nationality,
        dateOfBirth = if (dateOfBirth.isNotBlank()) LocalDate.parse(dateOfBirth) else null,
    )

internal fun JolpicaConstructor.toDomain(): Team =
    Team(
        id = constructorId,
        name = name,
        nationality = nationality,
    )

internal fun JolpicaCircuit.toDomain(): Circuit =
    Circuit(
        id = circuitId,
        name = circuitName,
        locality = location.locality,
        country = location.country,
        lat = location.lat.toDoubleOrNull() ?: 0.0,
        lng = location.long.toDoubleOrNull() ?: 0.0,
        url = url,
    )

internal fun JolpicaRace.toDomainRaceWeekend(): RaceWeekend =
    RaceWeekend(
        round = round.toIntOrNull() ?: 0,
        raceName = raceName,
        circuitId = circuit.circuitId,
        circuitName = circuit.circuitName,
        country = circuit.location.country,
        date = date,
        time = time.ifBlank { null },
        sessions =
            Sessions(
                fp1 = firstPractice?.toIso(),
                fp2 = secondPractice?.toIso(),
                fp3 = thirdPractice?.toIso(),
                qualifying = qualifying?.toIso(),
                sprint = sprint?.toIso(),
                race = if (date.isNotBlank() && time.isNotBlank()) "${date}T$time" else null,
            ),
    )

internal fun JolpicaSessionTime.toIso(): String? {
    if (date.isBlank() || time.isBlank()) return null
    return "${date}T$time"
}

internal fun JolpicaRaceResult.toDomainRaceResult(): RaceResult =
    RaceResult(
        position = position.toIntOrNull() ?: 0,
        driverId = driver.driverId,
        driverCode = driver.code,
        driverName = "${driver.givenName} ${driver.familyName}",
        teamId = constructor.constructorId,
        teamName = constructor.name,
        grid = grid.toIntOrNull() ?: 0,
        laps = laps.toIntOrNull() ?: 0,
        time = time?.time?.ifBlank { null },
        points = points.toDoubleOrNull() ?: 0.0,
        status = status,
        fastestLap =
            fastestLap?.let {
                FastestLap(
                    rank = it.rank.toIntOrNull() ?: 0,
                    lap = it.lap.toIntOrNull() ?: 0,
                    time = it.time?.time ?: "",
                    avgSpeed = it.averageSpeed?.speed ?: "",
                )
            },
    )

internal fun JolpicaQualifyingResult.toDomainQualifyingResult(): QualifyingResult =
    QualifyingResult(
        position = position.toIntOrNull() ?: 0,
        driverId = driver.driverId,
        driverCode = driver.code,
        driverName = "${driver.givenName} ${driver.familyName}",
        teamId = constructor.constructorId,
        teamName = constructor.name,
        q1 = q1.ifBlank { null },
        q2 = q2.ifBlank { null },
        q3 = q3.ifBlank { null },
    )

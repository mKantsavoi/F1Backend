package com.blaizmiko.adapter.route

import com.blaizmiko.adapter.dto.DriverDto
import com.blaizmiko.adapter.dto.DriversResponse
import com.blaizmiko.domain.model.ValidationException
import com.blaizmiko.usecase.GetDrivers
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Year

fun Route.driverRoutes(getDrivers: GetDrivers) {
    get("/drivers") {
        val season = call.queryParameters["season"]?.also { validateSeason(it) } ?: "current"
        val result = getDrivers.execute(season)

        if (result.isStale) {
            call.response.header(HttpHeaders.Warning, """110 - "Response is stale"""")
        }

        call.respond(DriversResponse(
            season = result.season,
            drivers = result.drivers.map { driver ->
                DriverDto(
                    id = driver.id,
                    number = driver.number,
                    code = driver.code,
                    firstName = driver.firstName,
                    lastName = driver.lastName,
                    nationality = driver.nationality,
                    dateOfBirth = driver.dateOfBirth?.toString(),
                )
            },
        ))
    }
}

private fun validateSeason(season: String) {
    val year = season.toIntOrNull()
    val currentYear = Year.now().value
    if (year == null || year < 1950 || year > currentYear) {
        throw ValidationException("Invalid season parameter: must be a year between 1950 and $currentYear")
    }
}

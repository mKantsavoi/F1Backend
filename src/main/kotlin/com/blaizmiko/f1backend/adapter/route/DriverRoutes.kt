package com.blaizmiko.f1backend.adapter.route

import com.blaizmiko.f1backend.adapter.dto.DriverDto
import com.blaizmiko.f1backend.adapter.dto.DriversResponse
import com.blaizmiko.f1backend.domain.model.FIRST_F1_SEASON
import com.blaizmiko.f1backend.domain.model.ValidationException
import com.blaizmiko.f1backend.usecase.GetDrivers
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject
import java.time.Year

fun Route.driverRoutes() {
    val getDrivers by inject<GetDrivers>()

    get("/drivers") {
        val season = call.queryParameters["season"]?.also { validateSeason(it) } ?: "current"
        val result = getDrivers.execute(season)

        if (result.isStale) {
            call.response.header(HttpHeaders.Warning, """110 - "Response is stale"""")
        }

        call.respond(
            DriversResponse(
                season = result.season,
                drivers =
                    result.drivers.map { driver ->
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
            ),
        )
    }
}

private fun validateSeason(season: String) {
    val year = season.toIntOrNull()
    val currentYear = Year.now().value
    if (year == null || year < FIRST_F1_SEASON || year > currentYear) {
        throw ValidationException("Invalid season parameter: must be a year between $FIRST_F1_SEASON and $currentYear")
    }
}

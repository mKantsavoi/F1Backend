package com.blaizmiko.f1backend.adapter.route

import com.blaizmiko.f1backend.adapter.dto.DriverDetailResponse
import com.blaizmiko.f1backend.adapter.dto.DriverDto
import com.blaizmiko.f1backend.adapter.dto.DriverTeamDto
import com.blaizmiko.f1backend.adapter.dto.DriversResponse
import com.blaizmiko.f1backend.usecase.GetDriverDetail
import com.blaizmiko.f1backend.usecase.GetDrivers
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject

fun Route.driverRoutes() {
    val getDrivers by inject<GetDrivers>()
    val getDriverDetail by inject<GetDriverDetail>()

    get("/drivers") {
        val drivers = getDrivers.execute()

        call.respond(
            DriversResponse(
                season = "current",
                drivers =
                    drivers.map { driver ->
                        DriverDto(
                            id = driver.id,
                            number = driver.number,
                            code = driver.code,
                            firstName = driver.firstName,
                            lastName = driver.lastName,
                            nationality = driver.nationality,
                            dateOfBirth = driver.dateOfBirth?.toString(),
                            photoUrl = driver.photoUrl,
                        )
                    },
            ),
        )
    }

    get("/drivers/{driverId}") {
        val driverId =
            call.parameters["driverId"]
                ?: throw com.blaizmiko.f1backend.domain.model
                    .ValidationException("Missing driverId")
        val detail = getDriverDetail.execute(driverId)

        call.respond(
            DriverDetailResponse(
                driverId = detail.driverId,
                number = detail.number,
                code = detail.code,
                firstName = detail.firstName,
                lastName = detail.lastName,
                nationality = detail.nationality,
                dateOfBirth = detail.dateOfBirth?.toString(),
                photoUrl = detail.photoUrl,
                team =
                    if (detail.teamId != null && detail.teamName != null) {
                        DriverTeamDto(teamId = detail.teamId, name = detail.teamName)
                    } else {
                        null
                    },
                biography = detail.biography,
            ),
        )
    }
}

package com.blaizmiko.f1backend.adapter.route

import com.blaizmiko.f1backend.adapter.dto.ConstructorStandingDto
import com.blaizmiko.f1backend.adapter.dto.ConstructorStandingsResponse
import com.blaizmiko.f1backend.adapter.dto.DriverStandingDto
import com.blaizmiko.f1backend.adapter.dto.DriverStandingsResponse
import com.blaizmiko.f1backend.usecase.GetConstructorStandings
import com.blaizmiko.f1backend.usecase.GetDriverStandings
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.standingsRoutes() {
    val getDriverStandings by inject<GetDriverStandings>()
    val getConstructorStandings by inject<GetConstructorStandings>()

    route("/standings") {
        get("/drivers") {
            val season = call.queryParameters["season"]?.also { validateSeason(it) } ?: "current"
            val result = getDriverStandings.execute(season)

            if (result.isStale) {
                call.response.header(HttpHeaders.Warning, """110 - "Response is stale"""")
            }

            call.respond(
                DriverStandingsResponse(
                    season = result.season,
                    round = result.round,
                    standings =
                        result.standings.map { s ->
                            DriverStandingDto(
                                position = s.position,
                                driverId = s.driverId,
                                driverCode = s.driverCode,
                                driverName = s.driverName,
                                nationality = s.nationality,
                                teamId = s.teamId,
                                teamName = s.teamName,
                                points = s.points,
                                wins = s.wins,
                            )
                        },
                ),
            )
        }

        get("/constructors") {
            val season = call.queryParameters["season"]?.also { validateSeason(it) } ?: "current"
            val result = getConstructorStandings.execute(season)

            if (result.isStale) {
                call.response.header(HttpHeaders.Warning, """110 - "Response is stale"""")
            }

            call.respond(
                ConstructorStandingsResponse(
                    season = result.season,
                    round = result.round,
                    standings =
                        result.standings.map { s ->
                            ConstructorStandingDto(
                                position = s.position,
                                teamId = s.teamId,
                                teamName = s.teamName,
                                nationality = s.nationality,
                                points = s.points,
                                wins = s.wins,
                            )
                        },
                ),
            )
        }
    }
}

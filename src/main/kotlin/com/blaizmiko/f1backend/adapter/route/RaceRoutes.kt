package com.blaizmiko.f1backend.adapter.route

import com.blaizmiko.f1backend.adapter.dto.FastestLapDto
import com.blaizmiko.f1backend.adapter.dto.QualifyingResultDto
import com.blaizmiko.f1backend.adapter.dto.QualifyingResultsResponse
import com.blaizmiko.f1backend.adapter.dto.RaceResultDto
import com.blaizmiko.f1backend.adapter.dto.RaceResultsResponse
import com.blaizmiko.f1backend.domain.model.RaceResult
import com.blaizmiko.f1backend.usecase.GetQualifyingResults
import com.blaizmiko.f1backend.usecase.GetRaceResults
import com.blaizmiko.f1backend.usecase.GetSprintResults
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.raceRoutes() {
    val getRaceResults by inject<GetRaceResults>()
    val getQualifyingResults by inject<GetQualifyingResults>()
    val getSprintResults by inject<GetSprintResults>()

    route("/races/{season}/{round}") {
        get("/results") {
            val season = call.pathParameters["season"]!!.also { validateSeason(it) }
            val round = call.pathParameters["round"]!!.also { validateRound(it) }
            val result = getRaceResults.execute(season, round.toInt())

            if (result.isStale) {
                call.response.header(HttpHeaders.Warning, """110 - "Response is stale"""")
            }

            call.respond(
                RaceResultsResponse(
                    season = result.season,
                    round = result.round,
                    raceName = result.raceName,
                    results = result.results.map { it.toDto() },
                ),
            )
        }

        get("/qualifying") {
            val season = call.pathParameters["season"]!!.also { validateSeason(it) }
            val round = call.pathParameters["round"]!!.also { validateRound(it) }
            val result = getQualifyingResults.execute(season, round.toInt())

            if (result.isStale) {
                call.response.header(HttpHeaders.Warning, """110 - "Response is stale"""")
            }

            call.respond(
                QualifyingResultsResponse(
                    season = result.season,
                    round = result.round,
                    raceName = result.raceName,
                    qualifying =
                        result.qualifying.map { q ->
                            QualifyingResultDto(
                                position = q.position,
                                driverId = q.driverId,
                                driverCode = q.driverCode,
                                driverName = q.driverName,
                                teamId = q.teamId,
                                teamName = q.teamName,
                                q1 = q.q1,
                                q2 = q.q2,
                                q3 = q.q3,
                            )
                        },
                ),
            )
        }

        get("/sprint") {
            val season = call.pathParameters["season"]!!.also { validateSeason(it) }
            val round = call.pathParameters["round"]!!.also { validateRound(it) }
            val result = getSprintResults.execute(season, round.toInt())

            if (result.isStale) {
                call.response.header(HttpHeaders.Warning, """110 - "Response is stale"""")
            }

            call.respond(
                RaceResultsResponse(
                    season = result.season,
                    round = result.round,
                    raceName = result.raceName,
                    results = result.results.map { it.toDto() },
                ),
            )
        }
    }
}

private fun RaceResult.toDto(): RaceResultDto =
    RaceResultDto(
        position = position,
        driverId = driverId,
        driverCode = driverCode,
        driverName = driverName,
        teamId = teamId,
        teamName = teamName,
        grid = grid,
        laps = laps,
        time = time,
        points = points,
        status = status,
        fastestLap =
            fastestLap?.let {
                FastestLapDto(
                    rank = it.rank,
                    lap = it.lap,
                    time = it.time,
                    avgSpeed = it.avgSpeed,
                )
            },
    )

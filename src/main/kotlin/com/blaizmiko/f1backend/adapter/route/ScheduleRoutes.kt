package com.blaizmiko.f1backend.adapter.route

import com.blaizmiko.f1backend.adapter.dto.NextRaceResponse
import com.blaizmiko.f1backend.adapter.dto.RaceWeekendDto
import com.blaizmiko.f1backend.adapter.dto.ScheduleResponse
import com.blaizmiko.f1backend.adapter.dto.SessionsDto
import com.blaizmiko.f1backend.domain.model.RaceWeekend
import com.blaizmiko.f1backend.usecase.GetNextRace
import com.blaizmiko.f1backend.usecase.GetSchedule
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.scheduleRoutes() {
    val getSchedule by inject<GetSchedule>()
    val getNextRace by inject<GetNextRace>()

    route("/schedule") {
        get {
            val season = call.queryParameters["season"]?.also { validateSeason(it) } ?: "current"
            val result = getSchedule.execute(season)

            if (result.isStale) {
                call.response.header(HttpHeaders.Warning, """110 - "Response is stale"""")
            }

            call.respond(
                ScheduleResponse(
                    season = result.season,
                    races = result.races.map { it.toDto() },
                ),
            )
        }

        get("/next") {
            val result = getNextRace.execute()

            if (result.isStale) {
                call.response.header(HttpHeaders.Warning, """110 - "Response is stale"""")
            }

            call.respond(
                NextRaceResponse(
                    season = result.season,
                    race = result.race?.toDto(),
                ),
            )
        }
    }
}

private fun RaceWeekend.toDto(): RaceWeekendDto =
    RaceWeekendDto(
        round = round,
        raceName = raceName,
        circuitId = circuitId,
        circuitName = circuitName,
        country = country,
        date = date,
        time = time,
        sessions =
            SessionsDto(
                fp1 = sessions.fp1,
                fp2 = sessions.fp2,
                fp3 = sessions.fp3,
                qualifying = sessions.qualifying,
                sprint = sessions.sprint,
                race = sessions.race,
            ),
    )

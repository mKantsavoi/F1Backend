package com.blaizmiko.f1backend.adapter.route

import com.blaizmiko.f1backend.adapter.dto.TeamDto
import com.blaizmiko.f1backend.adapter.dto.TeamsResponse
import com.blaizmiko.f1backend.usecase.GetTeams
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject

fun Route.teamRoutes() {
    val getTeams by inject<GetTeams>()

    get("/teams") {
        val season = call.queryParameters["season"]?.also { validateSeason(it) } ?: "current"
        val result = getTeams.execute(season)

        if (result.isStale) {
            call.response.header(HttpHeaders.Warning, """110 - "Response is stale"""")
        }

        call.respond(
            TeamsResponse(
                season = result.season,
                teams =
                    result.teams.map { team ->
                        TeamDto(
                            teamId = team.id,
                            name = team.name,
                            nationality = team.nationality,
                        )
                    },
            ),
        )
    }
}

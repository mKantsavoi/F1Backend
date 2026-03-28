package com.blaizmiko.f1backend.adapter.route

import com.blaizmiko.f1backend.adapter.dto.TeamDto
import com.blaizmiko.f1backend.adapter.dto.TeamsResponse
import com.blaizmiko.f1backend.usecase.GetTeams
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject

fun Route.teamRoutes() {
    val getTeams by inject<GetTeams>()

    get("/teams") {
        val teams = getTeams.execute()

        call.respond(
            TeamsResponse(
                season = "current",
                teams =
                    teams.map { team ->
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

package com.blaizmiko.f1backend.adapter.route

import com.blaizmiko.f1backend.adapter.dto.FavoriteDriverActionResponse
import com.blaizmiko.f1backend.adapter.dto.FavoriteDriverDto
import com.blaizmiko.f1backend.adapter.dto.FavoriteDriversResponse
import com.blaizmiko.f1backend.adapter.dto.FavoriteStatusResponse
import com.blaizmiko.f1backend.adapter.dto.FavoriteTeamActionResponse
import com.blaizmiko.f1backend.adapter.dto.FavoriteTeamDto
import com.blaizmiko.f1backend.adapter.dto.FavoriteTeamsResponse
import com.blaizmiko.f1backend.adapter.dto.FeedDriverDto
import com.blaizmiko.f1backend.adapter.dto.FeedTeamDto
import com.blaizmiko.f1backend.adapter.dto.LastRaceDto
import com.blaizmiko.f1backend.adapter.dto.PersonalizedFeedResponse
import com.blaizmiko.f1backend.adapter.dto.TeamDriverDto
import com.blaizmiko.f1backend.domain.model.ValidationException
import com.blaizmiko.f1backend.usecase.AddFavoriteDriver
import com.blaizmiko.f1backend.usecase.AddFavoriteTeam
import com.blaizmiko.f1backend.usecase.CheckDriverFavorite
import com.blaizmiko.f1backend.usecase.CheckTeamFavorite
import com.blaizmiko.f1backend.usecase.GetFavoriteDrivers
import com.blaizmiko.f1backend.usecase.GetFavoriteTeams
import com.blaizmiko.f1backend.usecase.GetPersonalizedFeed
import com.blaizmiko.f1backend.usecase.RemoveFavoriteDriver
import com.blaizmiko.f1backend.usecase.RemoveFavoriteTeam
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.favoritesRoutes() {
    route("/favorites") {
        driverFavoritesRoutes()
        teamFavoritesRoutes()
        feedRoutes()
    }
}

private fun Route.driverFavoritesRoutes() {
    val addFavoriteDriver by inject<AddFavoriteDriver>()
    val removeFavoriteDriver by inject<RemoveFavoriteDriver>()
    val getFavoriteDrivers by inject<GetFavoriteDrivers>()
    val checkDriverFavorite by inject<CheckDriverFavorite>()

    get("/drivers") {
        val userId = call.userId()
        val favorites = getFavoriteDrivers.execute(userId)
        call.respond(
            FavoriteDriversResponse(
                drivers =
                    favorites.map { fav ->
                        FavoriteDriverDto(
                            driverId = fav.driver.id,
                            number = fav.driver.number.takeIf { it != 0 },
                            code = fav.driver.code,
                            firstName = fav.driver.firstName,
                            lastName = fav.driver.lastName,
                            photoUrl = fav.driver.photoUrl,
                            teamName = fav.team?.name,
                            teamColor = null,
                            addedAt = fav.addedAt.toString(),
                        )
                    },
            ),
        )
    }

    post("/drivers/{driverId}") {
        val userId = call.userId()
        val driverId =
            call.parameters["driverId"]
                ?: throw ValidationException("Missing driverId")
        val (created, addedAt) = addFavoriteDriver.execute(userId, driverId)
        val status = if (created) HttpStatusCode.Created else HttpStatusCode.OK
        call.respond(status, FavoriteDriverActionResponse(driverId, addedAt.toString()))
    }

    delete("/drivers/{driverId}") {
        val userId = call.userId()
        val driverId =
            call.parameters["driverId"]
                ?: throw ValidationException("Missing driverId")
        removeFavoriteDriver.execute(userId, driverId)
        call.respond(HttpStatusCode.NoContent)
    }

    get("/drivers/check/{driverId}") {
        val userId = call.userId()
        val driverId =
            call.parameters["driverId"]
                ?: throw ValidationException("Missing driverId")
        val isFavorite = checkDriverFavorite.execute(userId, driverId)
        call.respond(FavoriteStatusResponse(isFavorite))
    }
}

private fun Route.teamFavoritesRoutes() {
    val addFavoriteTeam by inject<AddFavoriteTeam>()
    val removeFavoriteTeam by inject<RemoveFavoriteTeam>()
    val getFavoriteTeams by inject<GetFavoriteTeams>()
    val checkTeamFavorite by inject<CheckTeamFavorite>()

    get("/teams") {
        val userId = call.userId()
        val favorites = getFavoriteTeams.execute(userId)
        call.respond(
            FavoriteTeamsResponse(
                teams =
                    favorites.map { fav ->
                        FavoriteTeamDto(
                            teamId = fav.team.id,
                            name = fav.team.name,
                            nationality = fav.team.nationality,
                            drivers =
                                fav.drivers.map { driver ->
                                    TeamDriverDto(
                                        driverId = driver.id,
                                        code = driver.code,
                                        number = driver.number.takeIf { it != 0 },
                                    )
                                },
                            addedAt = fav.addedAt.toString(),
                        )
                    },
            ),
        )
    }

    post("/teams/{teamId}") {
        val userId = call.userId()
        val teamId =
            call.parameters["teamId"]
                ?: throw ValidationException("Missing teamId")
        val (created, addedAt) = addFavoriteTeam.execute(userId, teamId)
        val status = if (created) HttpStatusCode.Created else HttpStatusCode.OK
        call.respond(status, FavoriteTeamActionResponse(teamId, addedAt.toString()))
    }

    delete("/teams/{teamId}") {
        val userId = call.userId()
        val teamId =
            call.parameters["teamId"]
                ?: throw ValidationException("Missing teamId")
        removeFavoriteTeam.execute(userId, teamId)
        call.respond(HttpStatusCode.NoContent)
    }

    get("/teams/check/{teamId}") {
        val userId = call.userId()
        val teamId =
            call.parameters["teamId"]
                ?: throw ValidationException("Missing teamId")
        val isFavorite = checkTeamFavorite.execute(userId, teamId)
        call.respond(FavoriteStatusResponse(isFavorite))
    }
}

private fun Route.feedRoutes() {
    val getPersonalizedFeed by inject<GetPersonalizedFeed>()

    get("/feed") {
        val userId = call.userId()
        val feed = getPersonalizedFeed.execute(userId)
        call.respond(
            PersonalizedFeedResponse(
                favoriteDrivers =
                    feed.favoriteDrivers.map { d ->
                        FeedDriverDto(
                            driverId = d.driverId,
                            code = d.code,
                            photoUrl = d.photoUrl,
                            championshipPosition = d.championshipPosition,
                            championshipPoints = d.championshipPoints,
                            lastRace =
                                if (d.lastRacePosition != null && d.lastRaceName != null) {
                                    LastRaceDto(
                                        name = d.lastRaceName,
                                        position = d.lastRacePosition,
                                        points = d.lastRacePoints ?: 0.0,
                                    )
                                } else {
                                    null
                                },
                        )
                    },
                favoriteTeams =
                    feed.favoriteTeams.map { t ->
                        FeedTeamDto(
                            teamId = t.teamId,
                            name = t.name,
                            championshipPosition = t.championshipPosition,
                            championshipPoints = t.championshipPoints,
                        )
                    },
                relevantNews = feed.relevantNews,
            ),
        )
    }
}

private fun io.ktor.server.routing.RoutingCall.userId(): UUID = UUID.fromString(principal<JWTPrincipal>()!!.subject)

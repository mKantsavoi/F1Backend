package com.blaizmiko.f1backend.domain.repository

import java.time.Instant
import java.util.UUID

interface FavoriteRepository {
    suspend fun addFavoriteDriver(
        userId: UUID,
        driverId: String,
    ): Pair<Boolean, Instant>

    suspend fun removeFavoriteDriver(
        userId: UUID,
        driverId: String,
    )

    suspend fun getFavoriteDriverIds(userId: UUID): List<Pair<String, Instant>>

    suspend fun isDriverFavorite(
        userId: UUID,
        driverId: String,
    ): Boolean

    suspend fun addFavoriteTeam(
        userId: UUID,
        teamId: String,
    ): Pair<Boolean, Instant>

    suspend fun removeFavoriteTeam(
        userId: UUID,
        teamId: String,
    )

    suspend fun getFavoriteTeamIds(userId: UUID): List<Pair<String, Instant>>

    suspend fun isTeamFavorite(
        userId: UUID,
        teamId: String,
    ): Boolean
}

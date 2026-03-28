package com.blaizmiko.f1backend.infrastructure.persistence.repository

import com.blaizmiko.f1backend.domain.repository.FavoriteRepository
import com.blaizmiko.f1backend.infrastructure.persistence.DatabaseFactory.dbQuery
import com.blaizmiko.f1backend.infrastructure.persistence.table.FavoriteDriversTable
import com.blaizmiko.f1backend.infrastructure.persistence.table.FavoriteTeamsTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

class ExposedFavoriteRepository : FavoriteRepository {
    private fun UUID.toKUuid(): Uuid = this.toKotlinUuid()

    override suspend fun addFavoriteDriver(
        userId: UUID,
        driverId: String,
    ): Pair<Boolean, Instant> =
        dbQuery {
            val existing =
                FavoriteDriversTable
                    .selectAll()
                    .where {
                        (FavoriteDriversTable.userId eq userId.toKUuid()) and
                            (FavoriteDriversTable.driverId eq driverId)
                    }.singleOrNull()

            if (existing != null) {
                Pair(false, existing[FavoriteDriversTable.createdAt])
            } else {
                val now = Instant.now()
                FavoriteDriversTable.insert {
                    it[FavoriteDriversTable.id] = Uuid.random()
                    it[FavoriteDriversTable.userId] = userId.toKUuid()
                    it[FavoriteDriversTable.driverId] = driverId
                    it[FavoriteDriversTable.createdAt] = now
                }
                Pair(true, now)
            }
        }

    override suspend fun removeFavoriteDriver(
        userId: UUID,
        driverId: String,
    ) = dbQuery {
        FavoriteDriversTable.deleteWhere {
            (FavoriteDriversTable.userId eq userId.toKUuid()) and
                (FavoriteDriversTable.driverId eq driverId)
        }
        Unit
    }

    override suspend fun getFavoriteDriverIds(userId: UUID): List<Pair<String, Instant>> =
        dbQuery {
            FavoriteDriversTable
                .selectAll()
                .where { FavoriteDriversTable.userId eq userId.toKUuid() }
                .map { Pair(it[FavoriteDriversTable.driverId], it[FavoriteDriversTable.createdAt]) }
        }

    override suspend fun isDriverFavorite(
        userId: UUID,
        driverId: String,
    ): Boolean =
        dbQuery {
            FavoriteDriversTable
                .selectAll()
                .where {
                    (FavoriteDriversTable.userId eq userId.toKUuid()) and
                        (FavoriteDriversTable.driverId eq driverId)
                }.count() > 0
        }

    override suspend fun addFavoriteTeam(
        userId: UUID,
        teamId: String,
    ): Pair<Boolean, Instant> =
        dbQuery {
            val existing =
                FavoriteTeamsTable
                    .selectAll()
                    .where {
                        (FavoriteTeamsTable.userId eq userId.toKUuid()) and
                            (FavoriteTeamsTable.teamId eq teamId)
                    }.singleOrNull()

            if (existing != null) {
                Pair(false, existing[FavoriteTeamsTable.createdAt])
            } else {
                val now = Instant.now()
                FavoriteTeamsTable.insert {
                    it[FavoriteTeamsTable.id] = Uuid.random()
                    it[FavoriteTeamsTable.userId] = userId.toKUuid()
                    it[FavoriteTeamsTable.teamId] = teamId
                    it[FavoriteTeamsTable.createdAt] = now
                }
                Pair(true, now)
            }
        }

    override suspend fun removeFavoriteTeam(
        userId: UUID,
        teamId: String,
    ) = dbQuery {
        FavoriteTeamsTable.deleteWhere {
            (FavoriteTeamsTable.userId eq userId.toKUuid()) and
                (FavoriteTeamsTable.teamId eq teamId)
        }
        Unit
    }

    override suspend fun getFavoriteTeamIds(userId: UUID): List<Pair<String, Instant>> =
        dbQuery {
            FavoriteTeamsTable
                .selectAll()
                .where { FavoriteTeamsTable.userId eq userId.toKUuid() }
                .map { Pair(it[FavoriteTeamsTable.teamId], it[FavoriteTeamsTable.createdAt]) }
        }

    override suspend fun isTeamFavorite(
        userId: UUID,
        teamId: String,
    ): Boolean =
        dbQuery {
            FavoriteTeamsTable
                .selectAll()
                .where {
                    (FavoriteTeamsTable.userId eq userId.toKUuid()) and
                        (FavoriteTeamsTable.teamId eq teamId)
                }.count() > 0
        }
}

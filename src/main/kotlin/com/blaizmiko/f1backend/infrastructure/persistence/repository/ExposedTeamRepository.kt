package com.blaizmiko.f1backend.infrastructure.persistence.repository

import com.blaizmiko.f1backend.domain.model.Team
import com.blaizmiko.f1backend.domain.repository.TeamRepository
import com.blaizmiko.f1backend.infrastructure.persistence.DatabaseFactory.dbQuery
import com.blaizmiko.f1backend.infrastructure.persistence.table.TeamsTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant
import kotlin.uuid.Uuid

class ExposedTeamRepository : TeamRepository {
    override suspend fun findAll(): List<Team> =
        dbQuery {
            TeamsTable
                .selectAll()
                .map { it.toTeam() }
        }

    override suspend fun findByTeamId(teamId: String): Team? =
        dbQuery {
            TeamsTable
                .selectAll()
                .where { TeamsTable.teamId eq teamId }
                .singleOrNull()
                ?.toTeam()
        }

    override suspend fun insertAll(teams: List<Team>) =
        dbQuery {
            val now = Instant.now()
            TeamsTable.batchInsert(teams) { team ->
                this[TeamsTable.id] = Uuid.random()
                this[TeamsTable.teamId] = team.id
                this[TeamsTable.name] = team.name
                this[TeamsTable.nationality] = team.nationality
                this[TeamsTable.createdAt] = now
                this[TeamsTable.updatedAt] = now
            }
            Unit
        }

    override suspend fun count(): Long =
        dbQuery {
            TeamsTable.selectAll().count()
        }

    private fun ResultRow.toTeam() =
        Team(
            id = this[TeamsTable.teamId],
            name = this[TeamsTable.name],
            nationality = this[TeamsTable.nationality],
        )
}

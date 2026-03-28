package com.blaizmiko.f1backend.infrastructure.persistence.repository

import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.model.DriverWithTeam
import com.blaizmiko.f1backend.domain.repository.DriverRepository
import com.blaizmiko.f1backend.infrastructure.persistence.DatabaseFactory.dbQuery
import com.blaizmiko.f1backend.infrastructure.persistence.table.DriversTable
import com.blaizmiko.f1backend.infrastructure.persistence.table.TeamsTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant
import kotlin.uuid.Uuid

class ExposedDriverRepository : DriverRepository {
    override suspend fun findAll(): List<Driver> =
        dbQuery {
            DriversTable
                .selectAll()
                .map { it.toDriver() }
        }

    override suspend fun findByDriverId(driverId: String): DriverWithTeam? =
        dbQuery {
            (DriversTable leftJoin TeamsTable)
                .selectAll()
                .where { DriversTable.driverId eq driverId }
                .singleOrNull()
                ?.toDriverWithTeam()
        }

    override suspend fun insertAll(drivers: List<Driver>) =
        dbQuery {
            val now = Instant.now()
            DriversTable.batchInsert(drivers) { driver ->
                this[DriversTable.id] = Uuid.random()
                this[DriversTable.driverId] = driver.id
                this[DriversTable.number] = driver.number.takeIf { it != 0 }
                this[DriversTable.code] = driver.code
                this[DriversTable.firstName] = driver.firstName
                this[DriversTable.lastName] = driver.lastName
                this[DriversTable.nationality] = driver.nationality
                this[DriversTable.dateOfBirth] = driver.dateOfBirth
                this[DriversTable.photoUrl] = driver.photoUrl
                this[DriversTable.teamId] = driver.teamId
                this[DriversTable.biography] = driver.biography
                this[DriversTable.createdAt] = now
                this[DriversTable.updatedAt] = now
            }
            Unit
        }

    override suspend fun count(): Long =
        dbQuery {
            DriversTable.selectAll().count()
        }

    private fun ResultRow.toDriver() =
        Driver(
            id = this[DriversTable.driverId],
            number = this[DriversTable.number] ?: 0,
            code = this[DriversTable.code],
            firstName = this[DriversTable.firstName],
            lastName = this[DriversTable.lastName],
            nationality = this[DriversTable.nationality],
            dateOfBirth = this[DriversTable.dateOfBirth],
            photoUrl = this[DriversTable.photoUrl],
            teamId = this[DriversTable.teamId],
            biography = this[DriversTable.biography],
        )

    private fun ResultRow.toDriverWithTeam() =
        DriverWithTeam(
            driverId = this[DriversTable.driverId],
            number = this[DriversTable.number],
            code = this[DriversTable.code],
            firstName = this[DriversTable.firstName],
            lastName = this[DriversTable.lastName],
            nationality = this[DriversTable.nationality],
            dateOfBirth = this[DriversTable.dateOfBirth],
            photoUrl = this[DriversTable.photoUrl],
            biography = this[DriversTable.biography],
            teamId = this.getOrNull(TeamsTable.teamId),
            teamName = this.getOrNull(TeamsTable.name),
        )
}

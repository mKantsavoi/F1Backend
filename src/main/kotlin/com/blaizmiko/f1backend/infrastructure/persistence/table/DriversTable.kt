package com.blaizmiko.f1backend.infrastructure.persistence.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp

object DriversTable : Table("drivers") {
    val id = uuid("id")
    val driverId = varchar("driver_id", ColumnLengths.DRIVER_ID).uniqueIndex()
    val number = integer("number").nullable()
    val code = varchar("code", ColumnLengths.DRIVER_CODE).default("")
    val firstName = varchar("first_name", ColumnLengths.FIRST_NAME)
    val lastName = varchar("last_name", ColumnLengths.LAST_NAME)
    val nationality = varchar("nationality", ColumnLengths.NATIONALITY).default("")
    val dateOfBirth = date("date_of_birth").nullable()
    val photoUrl = varchar("photo_url", ColumnLengths.PHOTO_URL).nullable()
    val teamId = varchar("team_id", ColumnLengths.TEAM_ID).references(TeamsTable.teamId).nullable()
    val biography = text("biography").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

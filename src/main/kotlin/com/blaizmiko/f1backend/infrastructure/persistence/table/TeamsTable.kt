package com.blaizmiko.f1backend.infrastructure.persistence.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object TeamsTable : Table("teams") {
    val id = uuid("id")
    val teamId = varchar("team_id", ColumnLengths.TEAM_ID).uniqueIndex()
    val name = varchar("name", ColumnLengths.TEAM_NAME)
    val nationality = varchar("nationality", ColumnLengths.NATIONALITY).default("")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

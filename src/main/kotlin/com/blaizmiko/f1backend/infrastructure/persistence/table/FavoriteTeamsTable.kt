package com.blaizmiko.f1backend.infrastructure.persistence.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object FavoriteTeamsTable : Table("favorite_teams") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE).index()
    val teamId = varchar("team_id", ColumnLengths.TEAM_ID)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(userId, teamId)
    }
}

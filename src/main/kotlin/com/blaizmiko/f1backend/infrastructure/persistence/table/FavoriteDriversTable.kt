package com.blaizmiko.f1backend.infrastructure.persistence.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object FavoriteDriversTable : Table("favorite_drivers") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE).index()
    val driverId = varchar("driver_id", ColumnLengths.DRIVER_ID)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(userId, driverId)
    }
}

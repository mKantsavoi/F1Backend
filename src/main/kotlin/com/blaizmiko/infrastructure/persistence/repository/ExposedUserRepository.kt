package com.blaizmiko.infrastructure.persistence.repository

import com.blaizmiko.domain.model.Role
import com.blaizmiko.domain.model.User
import com.blaizmiko.domain.repository.UserRepository
import com.blaizmiko.infrastructure.persistence.DatabaseFactory.dbQuery
import com.blaizmiko.infrastructure.persistence.table.UsersTable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import java.time.Instant
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class ExposedUserRepository : UserRepository {

    private fun UUID.toKUuid(): Uuid = this.toKotlinUuid()
    private fun Uuid.toJUuid(): UUID = this.toJavaUuid()

    override suspend fun findById(id: UUID): User? = dbQuery {
        UsersTable.selectAll().where { UsersTable.id eq id.toKUuid() }.singleOrNull()?.toUser()
    }

    override suspend fun findByEmail(email: String): User? = dbQuery {
        UsersTable.selectAll().where { UsersTable.email eq email.lowercase() }.singleOrNull()?.toUser()
    }

    override suspend fun create(email: String, username: String, passwordHash: String): User = dbQuery {
        val now = Instant.now()
        val id = UUID.randomUUID()
        UsersTable.insert {
            it[UsersTable.id] = id.toKUuid()
            it[UsersTable.email] = email.lowercase()
            it[UsersTable.username] = username
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.role] = Role.USER.name.lowercase()
            it[UsersTable.createdAt] = now
            it[UsersTable.updatedAt] = now
        }
        User(id, email.lowercase(), username, passwordHash, Role.USER, now, now)
    }

    override suspend fun updateUsername(id: UUID, username: String): User? = dbQuery {
        val now = Instant.now()
        val updated = UsersTable.update({ UsersTable.id eq id.toKUuid() }) {
            it[UsersTable.username] = username
            it[UsersTable.updatedAt] = now
        }
        if (updated > 0) {
            UsersTable.selectAll().where { UsersTable.id eq id.toKUuid() }.singleOrNull()?.toUser()
        } else null
    }

    override suspend fun updatePasswordHash(id: UUID, passwordHash: String): User? = dbQuery {
        val now = Instant.now()
        val updated = UsersTable.update({ UsersTable.id eq id.toKUuid() }) {
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.updatedAt] = now
        }
        if (updated > 0) {
            UsersTable.selectAll().where { UsersTable.id eq id.toKUuid() }.singleOrNull()?.toUser()
        } else null
    }

    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id].toJUuid(),
        email = this[UsersTable.email],
        username = this[UsersTable.username],
        passwordHash = this[UsersTable.passwordHash],
        role = Role.valueOf(this[UsersTable.role].uppercase()),
        createdAt = this[UsersTable.createdAt],
        updatedAt = this[UsersTable.updatedAt],
    )
}

package com.blaizmiko.infrastructure.persistence.repository

import com.blaizmiko.domain.model.RefreshToken
import com.blaizmiko.domain.repository.RefreshTokenRepository
import com.blaizmiko.infrastructure.persistence.DatabaseFactory.dbQuery
import com.blaizmiko.infrastructure.persistence.table.RefreshTokensTable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import java.time.Instant
import java.util.UUID
import kotlin.uuid.Uuid

class ExposedRefreshTokenRepository : RefreshTokenRepository {

    private fun UUID.toKUuid(): Uuid = Uuid.parse(this.toString())
    private fun Uuid.toJUuid(): UUID = UUID.fromString(this.toString())

    override suspend fun findByTokenHash(tokenHash: String): RefreshToken? = dbQuery {
        RefreshTokensTable.selectAll()
            .where { RefreshTokensTable.tokenHash eq tokenHash }
            .singleOrNull()
            ?.toRefreshToken()
    }

    override suspend fun create(userId: UUID, tokenHash: String, expiresAt: Instant): RefreshToken = dbQuery {
        val now = Instant.now()
        val id = UUID.randomUUID()
        RefreshTokensTable.insert {
            it[RefreshTokensTable.id] = id.toKUuid()
            it[RefreshTokensTable.userId] = userId.toKUuid()
            it[RefreshTokensTable.tokenHash] = tokenHash
            it[RefreshTokensTable.expiresAt] = expiresAt
            it[RefreshTokensTable.revoked] = false
            it[RefreshTokensTable.createdAt] = now
        }
        RefreshToken(id, userId, tokenHash, expiresAt, false, now)
    }

    override suspend fun revokeByTokenHash(tokenHash: String): Boolean = dbQuery {
        val updated = RefreshTokensTable.update({ RefreshTokensTable.tokenHash eq tokenHash }) {
            it[RefreshTokensTable.revoked] = true
        }
        updated > 0
    }

    override suspend fun revokeAllForUser(userId: UUID): Int = dbQuery {
        RefreshTokensTable.update({ RefreshTokensTable.userId eq userId.toKUuid() }) {
            it[RefreshTokensTable.revoked] = true
        }
    }

    private fun ResultRow.toRefreshToken() = RefreshToken(
        id = this[RefreshTokensTable.id].toJUuid(),
        userId = this[RefreshTokensTable.userId].toJUuid(),
        tokenHash = this[RefreshTokensTable.tokenHash],
        expiresAt = this[RefreshTokensTable.expiresAt],
        revoked = this[RefreshTokensTable.revoked],
        createdAt = this[RefreshTokensTable.createdAt],
    )
}

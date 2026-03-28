package com.blaizmiko.f1backend.domain.repository

import com.blaizmiko.f1backend.domain.model.Team

interface TeamRepository {
    suspend fun findAll(): List<Team>

    suspend fun findByTeamId(teamId: String): Team?

    suspend fun insertAll(teams: List<Team>)

    suspend fun count(): Long
}

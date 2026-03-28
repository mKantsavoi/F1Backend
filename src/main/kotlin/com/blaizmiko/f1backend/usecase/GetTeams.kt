package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.domain.model.Team
import com.blaizmiko.f1backend.domain.repository.TeamRepository

class GetTeams(
    private val repository: TeamRepository,
) {
    suspend fun execute(): List<Team> = repository.findAll()
}

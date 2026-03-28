package com.blaizmiko.f1backend.domain.port

import com.blaizmiko.f1backend.domain.model.Team

interface TeamDataSource {
    suspend fun fetchTeams(season: String): Pair<String, List<Team>>
}

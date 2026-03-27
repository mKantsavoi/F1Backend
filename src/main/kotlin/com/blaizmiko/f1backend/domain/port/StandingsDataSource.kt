package com.blaizmiko.f1backend.domain.port

import com.blaizmiko.f1backend.domain.model.ConstructorStanding
import com.blaizmiko.f1backend.domain.model.DriverStanding
import com.blaizmiko.f1backend.domain.model.StandingsData

interface StandingsDataSource {
    suspend fun fetchDriverStandings(season: String): StandingsData<DriverStanding>

    suspend fun fetchConstructorStandings(season: String): StandingsData<ConstructorStanding>
}

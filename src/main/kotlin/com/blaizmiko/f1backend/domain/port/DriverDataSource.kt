package com.blaizmiko.f1backend.domain.port

import com.blaizmiko.f1backend.domain.model.Driver

interface DriverDataSource {
    suspend fun fetchDrivers(season: String): Pair<String, List<Driver>>
}

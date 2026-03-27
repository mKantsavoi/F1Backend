package com.blaizmiko.domain.port

import com.blaizmiko.domain.model.Driver

interface DriverDataSource {
    suspend fun fetchDrivers(season: String): Pair<String, List<Driver>>
}

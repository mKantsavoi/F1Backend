package com.blaizmiko.f1backend.domain.repository

import com.blaizmiko.f1backend.domain.model.Driver
import com.blaizmiko.f1backend.domain.model.DriverWithTeam

interface DriverRepository {
    suspend fun findAll(): List<Driver>

    suspend fun findByDriverId(driverId: String): DriverWithTeam?

    suspend fun insertAll(drivers: List<Driver>)

    suspend fun count(): Long
}

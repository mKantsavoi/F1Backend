package com.blaizmiko.f1backend.domain.port

import com.blaizmiko.f1backend.domain.model.Circuit

interface CircuitDataSource {
    suspend fun fetchCircuits(): List<Circuit>
}

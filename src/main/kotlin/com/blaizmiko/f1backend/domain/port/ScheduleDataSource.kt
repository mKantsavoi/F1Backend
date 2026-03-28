package com.blaizmiko.f1backend.domain.port

import com.blaizmiko.f1backend.domain.model.RaceWeekend

interface ScheduleDataSource {
    suspend fun fetchSchedule(season: String): Pair<String, List<RaceWeekend>>

    suspend fun fetchNextRace(): Pair<String, RaceWeekend?>
}

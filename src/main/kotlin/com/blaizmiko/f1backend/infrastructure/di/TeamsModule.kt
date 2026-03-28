package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.domain.repository.TeamRepository
import com.blaizmiko.f1backend.infrastructure.persistence.repository.ExposedTeamRepository
import com.blaizmiko.f1backend.usecase.GetTeams
import org.koin.dsl.bind
import org.koin.dsl.module

val teamsModule =
    module {
        single { ExposedTeamRepository() } bind TeamRepository::class
        single { GetTeams(get()) }
    }

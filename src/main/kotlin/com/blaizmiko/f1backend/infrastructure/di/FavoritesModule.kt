package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.domain.repository.FavoriteRepository
import com.blaizmiko.f1backend.infrastructure.persistence.repository.ExposedFavoriteRepository
import com.blaizmiko.f1backend.usecase.AddFavoriteDriver
import com.blaizmiko.f1backend.usecase.AddFavoriteTeam
import com.blaizmiko.f1backend.usecase.CheckDriverFavorite
import com.blaizmiko.f1backend.usecase.CheckTeamFavorite
import com.blaizmiko.f1backend.usecase.GetFavoriteDrivers
import com.blaizmiko.f1backend.usecase.GetFavoriteTeams
import com.blaizmiko.f1backend.usecase.GetPersonalizedFeed
import com.blaizmiko.f1backend.usecase.RemoveFavoriteDriver
import com.blaizmiko.f1backend.usecase.RemoveFavoriteTeam
import org.koin.dsl.bind
import org.koin.dsl.module

val favoritesModule =
    module {
        single { ExposedFavoriteRepository() } bind FavoriteRepository::class
        single { AddFavoriteDriver(get(), get()) }
        single { RemoveFavoriteDriver(get()) }
        single { AddFavoriteTeam(get(), get()) }
        single { RemoveFavoriteTeam(get()) }
        single { GetFavoriteDrivers(get(), get()) }
        single { GetFavoriteTeams(get(), get(), get()) }
        single { CheckDriverFavorite(get()) }
        single { CheckTeamFavorite(get()) }
        single { GetPersonalizedFeed(get(), get(), get(), get()) }
    }

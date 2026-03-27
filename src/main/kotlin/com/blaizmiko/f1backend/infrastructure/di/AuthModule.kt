package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.domain.repository.RefreshTokenRepository
import com.blaizmiko.f1backend.domain.repository.UserRepository
import com.blaizmiko.f1backend.infrastructure.persistence.repository.ExposedRefreshTokenRepository
import com.blaizmiko.f1backend.infrastructure.persistence.repository.ExposedUserRepository
import com.blaizmiko.f1backend.usecase.ChangePassword
import com.blaizmiko.f1backend.usecase.GetProfile
import com.blaizmiko.f1backend.usecase.LoginUser
import com.blaizmiko.f1backend.usecase.LogoutUser
import com.blaizmiko.f1backend.usecase.RefreshTokens
import com.blaizmiko.f1backend.usecase.RegisterUser
import com.blaizmiko.f1backend.usecase.UpdateProfile
import org.koin.dsl.bind
import org.koin.dsl.module

// Authentication feature: repositories and use cases.
// Add new auth-related components here (e.g., OAuth providers, MFA service).
val authModule = module {
    single { ExposedUserRepository() } bind UserRepository::class
    single { ExposedRefreshTokenRepository() } bind RefreshTokenRepository::class

    single { RegisterUser(get(), get(), get(), get()) }
    single { LoginUser(get(), get(), get(), get()) }
    single { RefreshTokens(get(), get(), get(), get()) }
    single { GetProfile(get()) }
    single { UpdateProfile(get()) }
    single { ChangePassword(get()) }
    single { LogoutUser(get()) }
}

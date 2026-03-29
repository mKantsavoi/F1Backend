package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.adapter.port.CacheProvider
import com.blaizmiko.f1backend.infrastructure.cache.CacheRegistry
import org.koin.dsl.bind
import org.koin.dsl.module

val cacheModule =
    module {
        single { CacheRegistry() } bind CacheProvider::class
    }

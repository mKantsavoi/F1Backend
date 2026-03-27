# Data Model: Koin Dependency Injection

**Feature**: 003-koin-dependency-injection
**Date**: 2026-03-27

## Overview

This feature introduces no new data entities. It is a pure internal refactoring of dependency wiring. All existing domain models, database tables, and DTOs remain unchanged.

## Dependency Graph (Koin Declarations)

The following represents the complete object graph managed by Koin after migration. This replaces the manual wiring currently in `Application.kt` and `Routing.kt`.

### Singletons

| Component | Interface Binding | Dependencies | Module |
|-----------|------------------|--------------|--------|
| AppConfig | — | (external: Ktor environment) | core |
| JwtConfig | — | extracted from AppConfig | core |
| JolpicaConfig | — | extracted from AppConfig | core |
| DatabaseConfig | — | extracted from AppConfig | core |
| JwtProvider | — | JwtConfig | core |
| JolpicaClient | DriverDataSource | JolpicaConfig | client |
| InMemoryDriverCache | DriverCache | — | drivers |
| ExposedUserRepository | UserRepository | — | auth |
| ExposedRefreshTokenRepository | RefreshTokenRepository | — | auth |
| GetDrivers | — | DriverCache, DriverDataSource, JolpicaConfig.cacheTtlHours | drivers |

### Factories (new instance per injection)

| Component | Dependencies | Module |
|-----------|--------------|--------|
| RegisterUser | UserRepository, RefreshTokenRepository, JwtProvider, JwtConfig | auth |
| LoginUser | UserRepository, RefreshTokenRepository, JwtProvider, JwtConfig | auth |
| RefreshTokens | UserRepository, RefreshTokenRepository, JwtProvider, JwtConfig | auth |
| GetProfile | UserRepository | auth |
| UpdateProfile | UserRepository | auth |
| ChangePassword | UserRepository | auth |
| LogoutUser | RefreshTokenRepository | auth |

### Objects (not managed by Koin — already singletons)

| Component | Reason |
|-----------|--------|
| DatabaseFactory | Kotlin `object`, called via `DatabaseFactory.init()` and `DatabaseFactory.dbQuery()` |
| PasswordHasher | Kotlin `object`, stateless utility |
| TokenHasher | Kotlin `object`, stateless utility |

## Module Composition

```
Application.module()
  └─ install(Koin)
       └─ modules(
            coreModule(appConfig),  // config + JWT
            clientModule,           // HTTP clients
            authModule,             // repositories + auth use cases
            driversModule           // cache + GetDrivers
          )
```

## No Schema Changes

- No database migrations required
- No new tables or columns
- No changes to existing entity classes

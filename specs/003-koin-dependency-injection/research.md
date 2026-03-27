# Research: Koin Dependency Injection

**Feature**: 003-koin-dependency-injection
**Date**: 2026-03-27

## R1: Koin Version & Compatibility

**Decision**: Use Koin BOM 4.2.0 (latest stable as of 2026-03-27, verified via Context7)

**Rationale**: Koin 4.x is the current major release line with full Ktor 3.x support. The BOM approach centralizes version management — individual Koin artifacts don't need explicit versions.

**Alternatives considered**:
- Koin 3.x: Older line, no reason to use when 4.x is stable and compatible
- Kodein: Less ecosystem adoption for Ktor; Koin is the de facto standard for Ktor DI
- Manual wiring (status quo): Does not scale as described in the spec

## R2: Required Koin Dependencies

**Decision**: Three Koin artifacts needed:

| Artifact | Scope | Purpose |
|----------|-------|---------|
| `io.insert-koin:koin-bom` (platform) | implementation | Version alignment |
| `io.insert-koin:koin-ktor` | implementation | Ktor plugin + route injection (`inject()`, `get()`) |
| `io.insert-koin:koin-logger-slf4j` | implementation | Logging via existing Logback |
| `io.insert-koin:koin-test` | testImplementation | `KoinTest`, `checkModules()` |
| `io.insert-koin:koin-test-junit5` | testImplementation | JUnit5 extension (Kotest uses JUnit5 platform) |

**Rationale**: `koin-ktor` provides the `Koin` plugin and `inject()`/`get()` extensions for `Application`, `Route`, and `Routing`. The SLF4J logger integrates with the existing Logback setup. Test artifacts enable module verification.

## R3: Injection Pattern in Ktor Routes

**Decision**: Use `inject()` (lazy delegate) at the `Route` level for obtaining dependencies in route handlers.

**Pattern**:
```kotlin
fun Route.authRoutes() {
    val registerUser by inject<RegisterUser>()
    val loginUser by inject<LoginUser>()
    // ... route definitions using these
}
```

**Rationale**: `inject()` is available as an extension on `Route` via `koin-ktor`. It provides lazy resolution, matching the current behavior where use cases are created once when routes are registered. The `get()` eager alternative also works but `inject()` is more idiomatic for property-style declarations.

**Alternative considered**: Constructor injection via Koin annotations — adds complexity (KSP processor) with no benefit for this project size.

## R4: Component Lifecycle Scoping

**Decision**: Use `single {}` for infrastructure singletons, `factory {}` for use cases.

| Component Type | Koin Scope | Rationale |
|---------------|------------|-----------|
| AppConfig | single | Loaded once from environment, immutable |
| JwtProvider | single | Stateless, holds HMAC algorithm |
| JolpicaClient | single | Manages HTTP client lifecycle |
| InMemoryDriverCache | single | Shared mutable state (ConcurrentHashMap) |
| ExposedUserRepository | single | Stateless, uses DatabaseFactory object |
| ExposedRefreshTokenRepository | single | Stateless, uses DatabaseFactory object |
| GetDrivers | single | Holds per-season mutexes and state maps — must be singleton |
| RegisterUser | factory | Stateless use case, cheap to create |
| LoginUser | factory | Stateless use case |
| RefreshTokens | factory | Stateless use case |
| GetProfile | factory | Stateless use case |
| UpdateProfile | factory | Stateless use case |
| ChangePassword | factory | Stateless use case |
| LogoutUser | factory | Stateless use case |

**Rationale**: Infrastructure components are singletons matching current behavior (created once in `configureRouting`). Use cases are stateless and could be either, but `factory` keeps them lightweight and aligns with the principle that they don't hold state. **Exception**: `GetDrivers` must be `single` because it holds mutex maps and cache state internally.

## R5: Koin Module Organization

**Decision**: Four Koin modules organized by domain:

1. **coreModule** — AppConfig, DatabaseFactory init, JwtProvider
2. **clientModule** — JolpicaClient (bound to DriverDataSource interface)
3. **authModule** — Repositories (UserRepository, RefreshTokenRepository), auth use cases (RegisterUser, LoginUser, RefreshTokens, GetProfile, UpdateProfile, ChangePassword, LogoutUser)
4. **driversModule** — InMemoryDriverCache (bound to DriverCache interface), GetDrivers

**Rationale**: Follows FR-005. Core infrastructure is separated from feature-specific components. Each future feature adds its own module file. Repositories live in authModule because they're currently only used by auth use cases; they can be extracted to a shared `persistenceModule` later if needed by multiple features.

## R6: AppConfig Loading in Koin

**Decision**: Load AppConfig outside Koin (in `Application.module()`) and pass it into the Koin module via a parameter or direct `single { }` declaration.

**Pattern**:
```kotlin
fun Application.module() {
    val appConfig = loadAppConfig()
    install(Koin) {
        slf4jLogger()
        modules(coreModule(appConfig), clientModule, authModule, driversModule)
    }
}
```

Where `coreModule` is a function:
```kotlin
fun coreModule(appConfig: AppConfig) = module {
    single { appConfig }
    single { appConfig.jwt }
    single { appConfig.jolpica }
    single { appConfig.database }
    single { JwtProvider(get()) }
}
```

**Rationale**: `loadAppConfig()` is an `Application` extension that reads from `environment.config`. It cannot run inside a Koin module definition because there's no `Application` context there. Passing the loaded config into the module factory function is the cleanest approach.

## R7: Database Initialization

**Decision**: Call `DatabaseFactory.init(config)` inside the `coreModule` single declaration or as an `Application.module()` call before/after Koin install.

**Pattern**: Initialize database as a side effect within a Koin single:
```kotlin
single {
    DatabaseFactory.init(get<DatabaseConfig>())
    DatabaseFactory  // Return the object for potential injection
}
```

Or keep `DatabaseFactory.init()` as an explicit call in `Application.module()` after Koin install, since it's a one-time initialization with side effects (schema creation).

**Decision**: Keep it as an explicit call after `install(Koin)` for clarity — side effects in Koin declarations are an anti-pattern.

## R8: Resource Cleanup (JolpicaClient)

**Decision**: Register the `ApplicationStopped` lifecycle hook in `Application.module()` after Koin install, retrieving `JolpicaClient` from Koin.

**Pattern**:
```kotlin
environment.monitor.subscribe(ApplicationStopped) {
    get<JolpicaClient>().close()
}
```

**Rationale**: Koin doesn't have built-in lifecycle callbacks for cleanup. The existing pattern of subscribing to `ApplicationStopped` is the correct Ktor approach. The only change is obtaining the client via `get()` instead of a local variable.

## R9: Authentication Plugin Integration

**Decision**: Install `Authentication` plugin in `Application.module()` with `JwtProvider` obtained from Koin.

**Pattern**:
```kotlin
fun Application.module() {
    val appConfig = loadAppConfig()
    install(Koin) { ... }

    val jwtProvider by inject<JwtProvider>()
    install(Authentication) {
        jwtProvider.configureAuth(this)
    }
    // ...
}
```

**Rationale**: Authentication must be installed before routing. Koin is installed first, then `JwtProvider` is injected and used to configure auth. This preserves the current initialization order.

## R10: Test Strategy

**Decision**: Tests continue using `testApplication {}` with module configuration. Koin starts/stops automatically with the Ktor test application since it's installed as a plugin.

**Rationale**: `testApplication { application { module() } }` already starts the full application module including Koin. No special Koin test setup needed for integration tests. The `koin-test` dependency enables optional `checkModules()` verification but existing tests work as-is since they test via HTTP endpoints, not direct DI resolution.

**Key insight**: The DriversEndpointTest uses a custom `testModule()` that overrides the `DriverDataSource` with a `FakeDataSource`. This will need to either:
- Override the Koin module in the test (using Koin's `allowOverride` or module loading), or
- Continue with the current approach if the test sets up its own application module.

After reviewing the test code, both tests define their own `Application.testModule()` that duplicates the wiring. With Koin, these tests should install Koin with modified modules that substitute fakes for real implementations.

# Quickstart: Favorites System

**Feature**: 009-favorites-system

## Prerequisites

- JDK 21
- Docker (for PostgreSQL via testcontainers in tests, or local dev)
- Existing f1backend project building successfully (`./gradlew build`)

## Key Files to Create

| File | Layer | Purpose |
|------|-------|---------|
| `infrastructure/persistence/table/FavoriteDriversTable.kt` | Infrastructure | Exposed DSL table |
| `infrastructure/persistence/table/FavoriteTeamsTable.kt` | Infrastructure | Exposed DSL table |
| `domain/repository/FavoriteRepository.kt` | Domain | Repository interface |
| `infrastructure/persistence/repository/ExposedFavoriteRepository.kt` | Infrastructure | Repository implementation |
| `usecase/AddFavoriteDriver.kt` | Use Case | Add driver favorite |
| `usecase/RemoveFavoriteDriver.kt` | Use Case | Remove driver favorite |
| `usecase/GetFavoriteDrivers.kt` | Use Case | List favorite drivers |
| `usecase/CheckDriverFavorite.kt` | Use Case | Check if driver is favorite |
| `usecase/AddFavoriteTeam.kt` | Use Case | Add team favorite |
| `usecase/RemoveFavoriteTeam.kt` | Use Case | Remove team favorite |
| `usecase/GetFavoriteTeams.kt` | Use Case | List favorite teams |
| `usecase/CheckTeamFavorite.kt` | Use Case | Check if team is favorite |
| `usecase/GetPersonalizedFeed.kt` | Use Case | Aggregated feed |
| `adapter/dto/FavoritesResponses.kt` | Adapter | Response DTOs |
| `adapter/route/FavoritesRoutes.kt` | Adapter | Route handlers |
| `infrastructure/di/FavoritesModule.kt` | Infrastructure | Koin DI module |
| `integration/FavoritesEndpointTest.kt` | Test | Integration tests |

## Key Files to Modify

| File | Change |
|------|--------|
| `infrastructure/persistence/DatabaseFactory.kt` | Add `FavoriteDriversTable`, `FavoriteTeamsTable` to `SchemaUtils.create()` |
| `Application.kt` | Import and register `favoritesModule` in Koin modules list |
| `Routing.kt` | Add `favoritesRoutes()` inside the `authenticate` block |

## Implementation Order

1. **Tables** → `FavoriteDriversTable`, `FavoriteTeamsTable`
2. **Repository** → Interface + Exposed implementation
3. **Use Cases** → CRUD use cases (add, remove, get, check) for both drivers and teams
4. **DTOs** → Response data classes
5. **Routes** → `FavoritesRoutes.kt` with all 9 endpoints
6. **DI** → `FavoritesModule.kt`
7. **Wiring** → Update `DatabaseFactory`, `Application.kt`, `Routing.kt`
8. **Feed** → `GetPersonalizedFeed` use case (depends on standings/race caches)
9. **Tests** → Integration tests covering all scenarios

## Build & Test

```bash
./gradlew ktlintCheck detekt test
```

## Test Locally

```bash
# Start with Docker Compose
docker compose up -d

# Register + login to get JWT
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","username":"tester","password":"Test1234!"}'

# Add a favorite driver
curl -X POST http://localhost:8080/api/v1/favorites/drivers/max_verstappen \
  -H "Authorization: Bearer <token>"

# List favorites
curl http://localhost:8080/api/v1/favorites/drivers \
  -H "Authorization: Bearer <token>"

# Get feed
curl http://localhost:8080/api/v1/favorites/feed \
  -H "Authorization: Bearer <token>"
```

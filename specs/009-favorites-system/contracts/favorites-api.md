# API Contract: Favorites System

**Base path**: `/api/v1/favorites`
**Authentication**: All endpoints require JWT Bearer token (`Authorization: Bearer <token>`)

---

## Favorite Drivers

### GET /favorites/drivers

List all favorite drivers for the authenticated user with full driver details.

**Response 200**:
```json
{
  "drivers": [
    {
      "driverId": "max_verstappen",
      "number": 1,
      "code": "VER",
      "firstName": "Max",
      "lastName": "Verstappen",
      "photoUrl": "https://media.formula1.com/...",
      "teamName": "Red Bull Racing",
      "teamColor": "3671C6",
      "addedAt": "2026-03-15T12:00:00Z"
    }
  ]
}
```

Empty state: `{ "drivers": [] }`

---

### POST /favorites/drivers/{driverId}

Add a driver to the authenticated user's favorites. Idempotent.

**Path parameter**: `driverId` (string) -- e.g., `max_verstappen`

**Response 201** (newly added):
```json
{ "status": "created" }
```

**Response 200** (already existed):
```json
{ "status": "already_exists" }
```

**Response 404** (driver not in system):
```json
{ "error": "not_found", "message": "Driver 'nonexistent' not found" }
```

---

### DELETE /favorites/drivers/{driverId}

Remove a driver from favorites. Idempotent -- returns 204 even if not favorited.

**Response 204**: No content.

---

### GET /favorites/drivers/check/{driverId}

Lightweight check if a driver is in the user's favorites.

**Response 200**:
```json
{ "isFavorite": true }
```

---

## Favorite Teams

### GET /favorites/teams

List all favorite teams with full team details.

**Response 200**:
```json
{
  "teams": [
    {
      "teamId": "red_bull",
      "name": "Red Bull Racing",
      "nationality": "Austrian",
      "drivers": [
        { "driverId": "max_verstappen", "code": "VER", "number": 1 },
        { "driverId": "hadjar", "code": "HAD", "number": 20 }
      ],
      "addedAt": "2026-03-10T08:00:00Z"
    }
  ]
}
```

Empty state: `{ "teams": [] }`

---

### POST /favorites/teams/{teamId}

Add a team to favorites. Idempotent.

**Response 201** / **200** / **404**: Same pattern as driver POST.

---

### DELETE /favorites/teams/{teamId}

Remove a team from favorites. Idempotent.

**Response 204**: No content.

---

### GET /favorites/teams/check/{teamId}

Lightweight check if a team is in the user's favorites.

**Response 200**:
```json
{ "isFavorite": false }
```

---

## Personalized Feed

### GET /favorites/feed

Aggregated feed for home screen combining standings and last race data for the user's favorites.

**Response 200**:
```json
{
  "favoriteDrivers": [
    {
      "driverId": "max_verstappen",
      "code": "VER",
      "photoUrl": "https://...",
      "championshipPosition": 1,
      "championshipPoints": 156.0,
      "lastRace": {
        "name": "Australian GP",
        "position": 1,
        "points": 25.0
      }
    }
  ],
  "favoriteTeams": [
    {
      "teamId": "red_bull",
      "name": "Red Bull Racing",
      "championshipPosition": 2,
      "championshipPoints": 280.0
    }
  ],
  "relevantNews": []
}
```

**Notes**:
- `lastRace` may be `null` if no race results are available yet.
- `championshipPosition` / `championshipPoints` may be `null` if standings data is unavailable.
- `relevantNews` is always an empty array (placeholder for future news feature).
- Empty favorites state: `{ "favoriteDrivers": [], "favoriteTeams": [], "relevantNews": [] }`

---

## Common Error Responses

**401 Unauthorized** (missing/invalid JWT):
```json
{ "error": "unauthorized", "message": "Token is invalid or has expired" }
```

## kotlinx.serialization DTOs

```kotlin
// --- Request/Response DTOs ---

@Serializable
data class FavoriteDriversResponse(val drivers: List<FavoriteDriverDto>)

@Serializable
data class FavoriteDriverDto(
    val driverId: String,
    val number: Int?,
    val code: String,
    val firstName: String,
    val lastName: String,
    val photoUrl: String?,
    val teamName: String?,
    val teamColor: String?,
    val addedAt: String,
)

@Serializable
data class FavoriteTeamsResponse(val teams: List<FavoriteTeamDto>)

@Serializable
data class FavoriteTeamDto(
    val teamId: String,
    val name: String,
    val nationality: String,
    val drivers: List<TeamDriverDto>,
    val addedAt: String,
)

@Serializable
data class TeamDriverDto(
    val driverId: String,
    val code: String,
    val number: Int?,
)

@Serializable
data class FavoriteStatusResponse(val isFavorite: Boolean)

@Serializable
data class FavoriteActionResponse(val status: String)

@Serializable
data class PersonalizedFeedResponse(
    val favoriteDrivers: List<FeedDriverDto>,
    val favoriteTeams: List<FeedTeamDto>,
    val relevantNews: List<String>,  // empty placeholder
)

@Serializable
data class FeedDriverDto(
    val driverId: String,
    val code: String,
    val photoUrl: String?,
    val championshipPosition: Int?,
    val championshipPoints: Double?,
    val lastRace: LastRaceDto?,
)

@Serializable
data class LastRaceDto(
    val name: String,
    val position: Int,
    val points: Double,
)

@Serializable
data class FeedTeamDto(
    val teamId: String,
    val name: String,
    val championshipPosition: Int?,
    val championshipPoints: Double?,
)
```

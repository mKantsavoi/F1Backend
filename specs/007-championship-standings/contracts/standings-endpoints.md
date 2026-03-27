# API Contract: Standings Endpoints

**Base path**: `/api/v1` (JWT-protected)

---

## GET /standings/drivers

Driver championship standings for a given season.

**Query Parameters**:

| Parameter | Type | Required | Default | Validation |
|-----------|------|----------|---------|------------|
| season | string | No | current year | "current" or integer 1950–current year |

**Success Response** (200):

```json
{
  "season": "2026",
  "round": 3,
  "standings": [
    {
      "position": 1,
      "driverId": "max_verstappen",
      "driverCode": "VER",
      "driverName": "Max Verstappen",
      "nationality": "Dutch",
      "teamId": "red_bull",
      "teamName": "Red Bull Racing",
      "points": 77.0,
      "wins": 2
    }
  ]
}
```

**Notes**:
- `season` is the resolved season year (even if "current" was requested)
- `round` indicates through which race round the standings are current
- `points` is a decimal (can be fractional from sprint races)
- `standings` is ordered by championship position
- Empty `standings` array if no races have been completed yet in the season

**Headers**:
- `Warning: 110 - "Response is stale"` — present when serving cached data after upstream failure

**Error Responses**:

| Status | Condition |
|--------|-----------|
| 400 | Invalid season parameter (non-numeric, future year, before 1950) |
| 401 | Missing or invalid JWT |
| 502 | Jolpica unavailable, no cache |

**Cache**: 1 hour if season == current year; forever otherwise.

---

## GET /standings/constructors

Constructor championship standings for a given season.

**Query Parameters**:

| Parameter | Type | Required | Default | Validation |
|-----------|------|----------|---------|------------|
| season | string | No | current year | "current" or integer 1950–current year |

**Success Response** (200):

```json
{
  "season": "2026",
  "round": 3,
  "standings": [
    {
      "position": 1,
      "teamId": "red_bull",
      "teamName": "Red Bull Racing",
      "nationality": "Austrian",
      "points": 156.0,
      "wins": 3
    }
  ]
}
```

**Notes**:
- Same semantics as driver standings for `season`, `round`, and `standings` ordering
- `points` is a decimal

**Headers**:
- `Warning: 110 - "Response is stale"` — present when serving cached data after upstream failure

**Error Responses**:

| Status | Condition |
|--------|-----------|
| 400 | Invalid season parameter |
| 401 | Missing or invalid JWT |
| 502 | Jolpica unavailable, no cache |

**Cache**: 1 hour if season == current year; forever otherwise.

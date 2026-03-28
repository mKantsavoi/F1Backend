# API Contract: Races Endpoints

**Base path**: `/api/v1` (JWT-protected)

---

## GET /races/{season}/{round}/results

Race finishing order for a specific Grand Prix.

**Path Parameters**:

| Parameter | Type | Required | Validation |
|-----------|------|----------|------------|
| season | string | Yes | Integer 1950–current year |
| round | string | Yes | Integer 1–99 |

**Success Response** (200):

```json
{
  "season": "2026",
  "round": 1,
  "raceName": "Bahrain Grand Prix",
  "results": [
    {
      "position": 1,
      "driverId": "max_verstappen",
      "driverCode": "VER",
      "driverName": "Max Verstappen",
      "teamId": "red_bull",
      "teamName": "Red Bull Racing",
      "grid": 1,
      "laps": 57,
      "time": "1:31:44.742",
      "points": 25.0,
      "status": "Finished",
      "fastestLap": {
        "rank": 1,
        "lap": 44,
        "time": "1:32.608",
        "avgSpeed": "206.018"
      }
    }
  ]
}
```

**Notes**:
- `time` is null for drivers who did not finish (DNF, DNS, DSQ)
- `fastestLap` is null for drivers without a recorded fastest lap
- `points` is a decimal (e.g., 25.0, 12.5 for sprint)
- Empty `results` array for races not yet completed

**Error Responses**:

| Status | Condition |
|--------|-----------|
| 400 | Invalid season or round |
| 401 | Missing or invalid JWT |
| 502 | Jolpica unavailable, no cache |

**Cache**: 5 minutes if season == current year; forever otherwise.

---

## GET /races/{season}/{round}/qualifying

Qualifying results with Q1, Q2, Q3 times.

**Path Parameters**: Same as results endpoint.

**Success Response** (200):

```json
{
  "season": "2026",
  "round": 1,
  "raceName": "Bahrain Grand Prix",
  "qualifying": [
    {
      "position": 1,
      "driverId": "max_verstappen",
      "driverCode": "VER",
      "driverName": "Max Verstappen",
      "teamId": "red_bull",
      "teamName": "Red Bull Racing",
      "q1": "1:30.558",
      "q2": "1:29.998",
      "q3": "1:29.179"
    }
  ]
}
```

**Notes**:
- `q2` is null for drivers eliminated in Q1
- `q3` is null for drivers eliminated in Q2
- `q1` may be null if driver did not set a time (e.g., mechanical failure)

**Error Responses**: Same as results endpoint.

**Cache**: Forever (qualifying results do not change after the session).

---

## GET /races/{season}/{round}/sprint

Sprint race results (same structure as race results).

**Path Parameters**: Same as results endpoint.

**Success Response** (200): Same JSON structure as `/results` endpoint.

**Not Found Response** (404):

```json
{
  "error": "not_found",
  "message": "No sprint results found for season 2026 round 1"
}
```

**Notes**:
- Returns 404 when the round did not have a sprint race
- Points values differ from main race (e.g., 8 for winner)

**Error Responses**:

| Status | Condition |
|--------|-----------|
| 400 | Invalid season or round |
| 401 | Missing or invalid JWT |
| 404 | Round did not have a sprint race |
| 502 | Jolpica unavailable, no cache |

**Cache**: Forever (sprint results do not change after the session).

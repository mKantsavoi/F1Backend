# API Contract: Drivers Endpoints

**Version**: v1 | **Date**: 2026-03-28

## GET /api/v1/drivers

Returns all current-season drivers from the database.

**Authentication**: Bearer JWT (required)

**Query Parameters**: None used (season parameter accepted but ignored — always returns current season)

**Response 200**:
```json
{
  "season": "2026",
  "drivers": [
    {
      "id": "max_verstappen",
      "number": 1,
      "code": "VER",
      "firstName": "Max",
      "lastName": "Verstappen",
      "nationality": "Dutch",
      "dateOfBirth": "1997-09-30",
      "photoUrl": "https://media.formula1.com/image/upload/c_fill,w_720/q_auto/v1740000001/common/f1/2026/redbullracing/maxver01/2026redbullracingmaxver01right.webp"
    }
  ]
}
```

**Field notes**:
- `photoUrl`: nullable (null if driver not in photo mapping)
- `dateOfBirth`: nullable, ISO-8601 date string
- `number`: integer (positive)
- `season`: hardcoded to current season string

**Response 401**: Missing or invalid JWT token
```json
{
  "error": "unauthorized",
  "message": "Token is missing or invalid"
}
```

**Breaking change note**: `photoUrl` is a NEW field added to the existing response. This is an additive, backward-compatible change.

---

## GET /api/v1/drivers/{driverId}

Returns the full driver detail card with team and biography.

**Authentication**: Bearer JWT (required)

**Path Parameters**:
- `driverId` (string, required): Jolpica canonical driver identifier (e.g., "max_verstappen")

**Response 200**:
```json
{
  "driverId": "max_verstappen",
  "number": 1,
  "code": "VER",
  "firstName": "Max",
  "lastName": "Verstappen",
  "nationality": "Dutch",
  "dateOfBirth": "1997-09-30",
  "photoUrl": "https://media.formula1.com/image/upload/c_fill,w_720/q_auto/v1740000001/common/f1/2026/redbullracing/maxver01/2026redbullracingmaxver01right.webp",
  "team": {
    "teamId": "red_bull",
    "name": "Red Bull Racing"
  },
  "biography": "Max Verstappen is a Dutch racing driver competing in Formula 1 for Red Bull Racing..."
}
```

**Field notes**:
- `photoUrl`: nullable
- `biography`: nullable (null if AI generation was skipped)
- `team`: nullable object (null if driver has no team assignment)
- `team.teamId`: Jolpica canonical constructor ID
- `team.name`: Resolved via database JOIN from teams table
- `dateOfBirth`: nullable, ISO-8601 date string

**Response 401**: Missing or invalid JWT token
```json
{
  "error": "unauthorized",
  "message": "Token is missing or invalid"
}
```

**Response 404**: Driver not found
```json
{
  "error": "not_found",
  "message": "Driver not found"
}
```

---

## GET /api/v1/teams (updated source)

Same response contract as before — only the data source changes from in-memory cache to database.

**Authentication**: Bearer JWT (required)

**Response 200** (unchanged format):
```json
{
  "season": "2026",
  "teams": [
    {
      "teamId": "red_bull",
      "name": "Red Bull Racing",
      "nationality": "Austrian"
    }
  ]
}
```

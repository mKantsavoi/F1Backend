# API Contract: Teams Endpoint

## GET /api/v1/teams

**Authentication**: Required (JWT Bearer token)

### Request

| Parameter | Location | Type   | Required | Default  | Description                          |
|-----------|----------|--------|----------|----------|--------------------------------------|
| season    | query    | string | No       | current  | Season year (e.g., "2024", "2026")   |

**Season validation**: Must be an integer between 1950 and the current year (inclusive). Invalid values return 422.

### Response — 200 OK

```json
{
  "season": "2026",
  "teams": [
    {
      "teamId": "red_bull",
      "name": "Red Bull Racing",
      "nationality": "Austrian"
    },
    {
      "teamId": "mercedes",
      "name": "Mercedes-AMG Petronas",
      "nationality": "German"
    }
  ]
}
```

| Field            | Type   | Description                                |
|------------------|--------|--------------------------------------------|
| season           | string | The resolved season year                   |
| teams            | array  | List of constructor objects                |
| teams[].teamId   | string | Stable constructor identifier              |
| teams[].name     | string | Constructor display name                   |
| teams[].nationality | string | Constructor nationality                 |

### Response — 200 OK (stale cache)

Same body as above, with additional header:

```
Warning: 110 - "Response is stale"
```

Returned when upstream is unavailable but cached data exists.

### Response — 401 Unauthorized

```json
{
  "error": "Unauthorized",
  "message": "Token is not valid or has expired"
}
```

### Response — 422 Unprocessable Entity

```json
{
  "error": "Validation Error",
  "message": "Invalid season parameter: must be a year between 1950 and 2026"
}
```

### Response — 502 Bad Gateway

```json
{
  "error": "External Service Error",
  "message": "Unable to fetch team data. Please try again later."
}
```

Returned when upstream is unavailable and no cached data exists.

# API Contract: Drivers Endpoint

**Feature**: 002-f1-drivers-endpoint
**Date**: 2026-03-27

## GET /api/v1/drivers

Returns all drivers for a given F1 season.

### Authentication

Required. Bearer JWT token in `Authorization` header.

### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| season | String | No | "current" | Season year (e.g., "2024") or omitted for current season |

**Validation**:
- If provided, must be a 4-digit year between 1950 and the current year (inclusive)
- Invalid values return 400

### Success Response

**Status**: `200 OK`
**Content-Type**: `application/json`

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
      "dateOfBirth": "1997-09-30"
    },
    {
      "id": "hamilton",
      "number": 44,
      "code": "HAM",
      "firstName": "Lewis",
      "lastName": "Hamilton",
      "nationality": "British",
      "dateOfBirth": "1985-01-07"
    }
  ]
}
```

### Stale Cache Response

**Status**: `200 OK`
**Headers**: `Warning: 110 - "Response is stale"`
**Body**: Same structure as success response.

Returned when the external data source is unavailable but cached data exists (even if expired).

### Error Responses

#### 401 Unauthorized

Missing or invalid JWT token.

```json
{
  "error": "unauthorized",
  "message": "Token is missing or invalid"
}
```

#### 400 Bad Request

Invalid season parameter.

```json
{
  "error": "validation_error",
  "message": "Invalid season parameter: must be a year between 1950 and 2026"
}
```

#### 502 Bad Gateway

External data source unavailable and no cached data exists.

```json
{
  "error": "external_service_unavailable",
  "message": "Unable to fetch driver data. Please try again later."
}
```

### Response Headers

| Header | When Present | Value |
|--------|-------------|-------|
| Content-Type | Always | `application/json` |
| Warning | Stale cache served | `110 - "Response is stale"` |

### Rate Limiting Notes

- Backend caches responses per-season with 24h TTL
- External API is called at most once per season per 24h period
- Concurrent requests for the same season are coalesced (single external fetch)

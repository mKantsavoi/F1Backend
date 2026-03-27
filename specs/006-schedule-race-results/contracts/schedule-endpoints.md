# API Contract: Schedule Endpoints

**Base path**: `/api/v1` (JWT-protected)

---

## GET /schedule

Full season schedule with all race weekends.

**Query Parameters**:

| Parameter | Type | Required | Default | Validation |
|-----------|------|----------|---------|------------|
| season | string | No | current year | Must be "current" or integer 1950–current year |

**Success Response** (200):

```json
{
  "season": "2026",
  "races": [
    {
      "round": 1,
      "raceName": "Bahrain Grand Prix",
      "circuitId": "bahrain",
      "circuitName": "Bahrain International Circuit",
      "country": "Bahrain",
      "date": "2026-03-15",
      "time": "15:00:00Z",
      "sessions": {
        "fp1": "2026-03-13T11:30:00Z",
        "fp2": "2026-03-13T15:00:00Z",
        "fp3": "2026-03-14T12:30:00Z",
        "qualifying": "2026-03-14T16:00:00Z",
        "race": "2026-03-15T15:00:00Z"
      }
    }
  ]
}
```

**Notes**:
- `time` may be null if not yet announced
- `sessions` fields are null/omitted when not scheduled (e.g., `sprint` only present on sprint weekends)
- Empty `races` array for seasons with no data yet

**Error Responses**:

| Status | Condition |
|--------|-----------|
| 400 | Invalid season parameter |
| 401 | Missing or invalid JWT |
| 502 | Jolpica unavailable, no cache |

**Stale Cache**: Returns 200 with `Warning: 110 - "Response is stale"` header when serving expired cache during Jolpica outage.

**Cache**: 6 hours per season key.

---

## GET /schedule/next

The next upcoming race only.

**Parameters**: None.

**Success Response** (200):

```json
{
  "season": "2026",
  "race": {
    "round": 3,
    "raceName": "Australian Grand Prix",
    "circuitId": "albert_park",
    "circuitName": "Albert Park Grand Prix Circuit",
    "country": "Australia",
    "date": "2026-03-29",
    "time": "14:00:00Z",
    "sessions": {
      "fp1": "2026-03-27T01:30:00Z",
      "fp2": "2026-03-27T05:00:00Z",
      "fp3": "2026-03-28T01:30:00Z",
      "qualifying": "2026-03-28T05:00:00Z",
      "race": "2026-03-29T14:00:00Z"
    }
  }
}
```

**Notes**:
- `race` is null when the season has ended and no next race exists
- Response wraps a single race object (not an array)

**Error Responses**:

| Status | Condition |
|--------|-----------|
| 401 | Missing or invalid JWT |
| 502 | Jolpica unavailable, no cache |

**Cache**: 1 hour, single key.

# API Contract: Circuits Endpoint

## GET /api/v1/circuits

**Authentication**: Required (JWT Bearer token)

### Request

No query parameters. Returns the full historical circuit catalog.

### Response — 200 OK

```json
{
  "circuits": [
    {
      "circuitId": "monza",
      "name": "Autodromo Nazionale di Monza",
      "locality": "Monza",
      "country": "Italy",
      "lat": 45.6156,
      "lng": 9.2811,
      "url": "https://en.wikipedia.org/wiki/Monza_Circuit"
    },
    {
      "circuitId": "silverstone",
      "name": "Silverstone Circuit",
      "locality": "Silverstone",
      "country": "UK",
      "lat": 52.0786,
      "lng": -1.0169,
      "url": "https://en.wikipedia.org/wiki/Silverstone_Circuit"
    }
  ]
}
```

| Field               | Type   | Description                                |
|---------------------|--------|--------------------------------------------|
| circuits            | array  | List of circuit objects                    |
| circuits[].circuitId | string | Stable circuit identifier                 |
| circuits[].name     | string | Circuit display name                       |
| circuits[].locality | string | City where the circuit is located          |
| circuits[].country  | string | Country where the circuit is located       |
| circuits[].lat      | double | Latitude coordinate                        |
| circuits[].lng      | double | Longitude coordinate                       |
| circuits[].url      | string | Wikipedia URL for the circuit              |

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

### Response — 502 Bad Gateway

```json
{
  "error": "External Service Error",
  "message": "Unable to fetch circuit data. Please try again later."
}
```

Returned when upstream is unavailable and no cached data exists.

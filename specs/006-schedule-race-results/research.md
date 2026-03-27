# Research: Schedule, Race Results & Qualifying Endpoints

**Date**: 2026-03-27
**Feature**: 006-schedule-race-results

## No NEEDS CLARIFICATION Items

All technical context was resolved from the existing codebase. No unknowns required external research.

## Decision Log

### D-001: Cache Architecture for Results (keyed by season+round)

**Decision**: Use a single generic `RaceResultCache` interface with string keys (format: `"type:season:round"`) rather than three separate cache interfaces for results, qualifying, and sprint.

**Rationale**: Results, qualifying, and sprint all share the same key shape (season + round) and similar access patterns. A single keyed cache with prefixed keys avoids three nearly-identical interface/implementation pairs while maintaining type safety through the use case layer. This follows the YAGNI principle.

**Alternatives considered**:
- Three separate cache interfaces (ResultsCache, QualifyingCache, SprintCache) — rejected: excessive duplication for identical key/value patterns
- Single generic `Map<String, CacheEntry<*>>` — rejected: loses type safety

### D-002: Cache TTL Strategy for Current vs Past Season Results

**Decision**: Determine "current season" by comparing the requested season string against `Year.now().value.toString()`. If they match, use 5-minute TTL. Otherwise, use `Instant.MAX` (effectively forever).

**Rationale**: Simplest possible implementation. The use case calculates expiration at cache-write time based on this comparison. No need for a separate "season resolver" — the existing `SeasonCache` pattern already stores the resolved season.

**Alternatives considered**:
- Config-driven TTL per season — rejected: overengineering for a binary current/past distinction
- Time-based heuristic (e.g., cache longer if race was >24h ago) — rejected: more complex, marginal benefit

### D-003: Next Race Cache — Single-Key vs Season-Keyed

**Decision**: Use a single-value cache (AtomicReference) for next race, similar to the existing CircuitCache pattern.

**Rationale**: There's only ever one "next race" at a time. No season parameter. AtomicReference is the simplest correct choice — mirrors the proven CircuitCache approach.

**Alternatives considered**:
- ConcurrentHashMap with a fixed "next" key — rejected: unnecessary overhead for single value

### D-004: Schedule Cache — Reuse SeasonCache<T> Pattern

**Decision**: Reuse the existing `SeasonCache<T>` wrapper for schedule data, with `T = RaceWeekend`. Cache is keyed by season string (same as drivers/teams).

**Rationale**: The schedule endpoint has the same semantics as drivers/teams: keyed by season, supports "current" as a parameter, needs to store the resolved season. Reusing `SeasonCache` avoids inventing a new cache wrapper.

### D-005: Sprint 404 Handling

**Decision**: When Jolpica returns an empty sprint results list for a round, the use case throws a domain-specific exception (e.g., `NotFoundException`) that maps to HTTP 404 via StatusPages.

**Rationale**: This follows the existing exception-to-HTTP mapping pattern. The route handler doesn't need special 404 logic — the use case determines "no sprint data = not found" and throws.

**Alternatives considered**:
- Return empty list with 200 — rejected: spec explicitly requires 404 for non-sprint rounds
- Check in route handler — rejected: business logic belongs in use case layer

### D-006: JolpicaClient Extension Strategy

**Decision**: Add two new domain port interfaces (`ScheduleDataSource`, `RaceDataSource`) and have `JolpicaClient` implement them alongside the existing three interfaces. Register additional interface bindings in `ClientModule`.

**Rationale**: Follows the established pattern where `JolpicaClient` is a single instance implementing multiple port interfaces. Keeps HTTP client logic centralized. New bindings in `ClientModule` via `single<ScheduleDataSource> { get<JolpicaClient>() }`.

### D-007: Round Validation

**Decision**: Add a `validateRound()` function to the existing `RouteValidation.kt` that ensures the round is a positive integer (1-99 range is sufficient for any F1 season).

**Rationale**: Keeps all validation in the adapter layer per constitution principle IV. Reuses the existing validation file.

### D-008: New Exception — NotFoundException

**Decision**: Add `NotFoundException` to `Exceptions.kt` with a StatusPages mapping to HTTP 404.

**Rationale**: Currently the project has no 404 exception. Sprint results for non-sprint rounds need one. This is a small, justified addition — not speculative.

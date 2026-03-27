# Quickstart: ktlint and detekt Static Analysis

**Date**: 2026-03-27
**Feature**: 004-ktlint-detekt-setup

## Prerequisites

- JDK 21 (already required by the project)
- Gradle wrapper (already present)

## After Implementation

### Check code formatting
```bash
./gradlew ktlintCheck
```

### Auto-fix formatting issues
```bash
./gradlew ktlintFormat
```

### Run static analysis
```bash
./gradlew detekt
```

### Run everything (build + tests)
```bash
./gradlew build
```

## Verification

All three linting commands should exit with zero violations on a clean checkout of this branch.

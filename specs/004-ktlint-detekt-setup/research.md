# Research: ktlint and detekt Static Analysis Setup

**Date**: 2026-03-27
**Feature**: 004-ktlint-detekt-setup

## ktlint Gradle Plugin

- **Decision**: Use `org.jlleitschuh.gradle.ktlint` plugin version `14.0.1`
- **Rationale**: Latest stable release. Most widely adopted ktlint Gradle integration. Automatically provides `ktlintCheck` and `ktlintFormat` tasks for all Kotlin source sets. Requires `mavenCentral()` repository (already configured).
- **Alternatives considered**:
  - Pinterest ktlint CLI (manual setup, no Gradle integration) — rejected for lack of build system integration
  - Kotlinter plugin — less popular, fewer features

## detekt Gradle Plugin

- **Decision**: Use `io.gitlab.arturbosch.detekt` plugin version `1.23.7`
- **Rationale**: Latest stable 1.x release. The 2.x line (`dev.detekt`) is still in alpha (`2.0.0-alpha.2`) and not suitable for a production project. The 1.x plugin ID is well-established and widely documented.
- **Alternatives considered**:
  - detekt 2.0.0-alpha.2 (`dev.detekt`) — rejected as alpha/pre-release
  - SonarQube — too heavy for a small backend project

## Plugin Configuration Approach

- **Decision**: Add both plugins to `gradle/libs.versions.toml` version catalog and reference via `alias()` in `build.gradle.kts`
- **Rationale**: Consistent with existing project convention — all dependencies use the version catalog pattern.
- **Alternatives considered**:
  - Inline plugin versions in `build.gradle.kts` — rejected for inconsistency with project patterns

## detekt Configuration

- **Decision**: Generate a default `detekt.yml` via `./gradlew detektGenerateConfig` and keep reasonable defaults. Suppress only rules that produce false positives on the existing clean codebase.
- **Rationale**: Starting from defaults ensures comprehensive coverage. Suppressing only verified false positives avoids masking real issues while keeping the baseline clean.
- **Alternatives considered**:
  - Hand-written minimal config — rejected as error-prone and likely to miss important rules
  - No config file (pure defaults) — rejected because we need to tune thresholds for the existing codebase

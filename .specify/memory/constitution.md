<!--
  Sync Impact Report
  ==================
  Version change: 1.3.0 → 1.5.0 (MINOR: two new principles added)
  Modified principles: None
  Added sections:
    - VII. Dependency Injection via Koin
    - VIII. Static Analysis & Code Style
  Added to Technology Stack table:
    - Dependency injection row (Koin)
  Removed sections: None
  Templates requiring updates:
    - .specify/templates/plan-template.md — ✅ no updates needed
      (Constitution Check section is generic; picks up new principles)
    - .specify/templates/spec-template.md — ✅ no updates needed
    - .specify/templates/tasks-template.md — ✅ no updates needed
  Follow-up TODOs: None
-->

# F1 Backend Constitution

## Core Principles

### I. Clean Architecture

All code MUST follow Clean Architecture layering:
**domain → use-cases → adapters → infrastructure**.

- **Domain layer**: Pure Kotlin entities and value objects.
  No framework imports allowed.
- **Use-cases layer**: Application business rules.
  Depends only on domain. No HTTP, database, or
  serialization concerns.
- **Adapters layer**: Ktor routes, request/response DTOs,
  serialization mappings. Translates between external
  formats and use-cases.
- **Infrastructure layer**: Database repositories (Exposed),
  external API clients, configuration. Implements
  interfaces defined in domain/use-cases.

Dependency rule: inner layers MUST NOT reference outer
layers. Violations are blocking in code review.

### II. API-First Design

Every feature MUST begin with its API contract before
implementation starts.

- All endpoints follow REST conventions with URL-based
  versioning (`/v1/`).
- OpenAPI / Swagger documentation MUST be generated via
  the Ktor OpenAPI plugin for every public endpoint.
- Request/response schemas MUST use kotlinx.serialization
  data classes.
- Breaking changes MUST follow the versioning policy
  defined in this constitution.

### III. Test Coverage (NON-NEGOTIABLE)

Tests MUST accompany every feature. No feature is
considered complete without test coverage.

- Tests MAY be written before or alongside implementation,
  but MUST exist before a PR is merged.
- Testing stack: **kotest** for assertions and test
  structure, **ktor-server-test-host** for route testing,
  **testcontainers** for PostgreSQL integration tests.
- No PR MUST be merged without all tests passing.
- Integration tests MUST use real PostgreSQL via
  testcontainers — database mocks are prohibited for
  integration-level tests.

### IV. Security & Input Validation

All external input MUST be validated at system boundaries.

- Authentication and authorization MUST be enforced on
  every non-public endpoint.
- Input validation MUST occur in the adapters layer before
  data reaches use-cases.
- OWASP Top 10 vulnerabilities MUST be actively prevented:
  SQL injection (use Exposed parameterized queries),
  XSS (irrelevant for API-only but sanitize where needed),
  broken authentication, sensitive data exposure.
- Secrets MUST NOT be committed to the repository.
  Use environment variables or secret management tools.

### V. Simplicity & Established Libraries

Start simple. Every abstraction MUST justify its existence.

- YAGNI: Do not build for hypothetical future requirements.
- Prefer well-established libraries over custom
  implementations. Reinventing solved problems is
  prohibited unless no suitable library exists.
- Ktor ONLY — Spring MUST NOT be used anywhere in
  this project.
- Every added dependency MUST have a clear, documented
  reason. Avoid transitive dependency bloat.

### VI. Dependency Verification via Context7

Before using or upgrading any third-party library, the
AI agent MUST query the Context7 MCP server to verify
the latest stable version and review official documentation
for correct API usage.

- The agent MUST NOT rely on training data for library
  versions or API signatures. Context7 MUST be consulted
  first.
- This applies to ALL dependencies: Ktor, Exposed,
  kotlinx-serialization, BCrypt, testcontainers, kotest,
  and any future additions.
- When adding a new dependency, the agent MUST:
  1. Resolve the library ID via Context7
     (`resolve-library-id`).
  2. Query the official documentation (`query-docs`) for
     the specific API surface being used.
  3. Verify the latest stable version before pinning it
     in `gradle/libs.versions.toml`.
- When upgrading an existing dependency, the agent MUST:
  1. Check Context7 for the current latest stable version.
  2. Review migration guides or changelog entries for
     breaking changes between the current and target
     versions.
  3. Update all call sites if API signatures have changed.
- If Context7 is unavailable or returns no results for a
  library, the agent MUST flag this to the user and await
  explicit approval before proceeding with training-data
  assumptions.

### VII. Dependency Injection via Koin

All dependency wiring MUST use Koin. Manual construction
of services in routes or application modules is prohibited.

- **One Koin module per feature**: Each feature domain
  (auth, drivers, teams, circuits, etc.) MUST have its own
  Koin module file in `infrastructure/di/`. Mixing
  unrelated bindings in a single module is prohibited.
- **Shared infrastructure modules**: Cross-cutting clients
  (e.g., `clientModule` for HTTP clients) and core config
  (`coreModule`) MUST remain in dedicated modules, separate
  from feature modules.
- **Single scope for use cases**: Use cases MUST be
  registered as `single` (singleton) scope, not `factory`.
  Use cases hold caches and mutexes that MUST persist
  across requests.
- **Interface binding**: Infrastructure implementations
  MUST be bound to their domain port interfaces using
  Koin's `bind` operator (e.g.,
  `single { InMemoryDriverCache() } bind DriverCache::class`).
  Routes and use cases MUST depend on port interfaces,
  never on concrete implementations.
- **Resource cleanup**: Clients that hold resources
  (HTTP connections, database pools) MUST register an
  `onClose` hook in their Koin module to ensure proper
  shutdown.
- **Module registration**: All Koin modules MUST be
  registered in `Application.kt` via the `modules()`
  call inside `install(Koin)`. No lazy or conditional
  module loading.
- **Test isolation**: Integration tests MUST create their
  own Koin module with test doubles (fakes, not mocks)
  rather than reusing production modules. This ensures
  tests are deterministic and do not depend on external
  services.

### VIII. Static Analysis & Code Style

All code MUST pass **ktlint** and **detekt** with zero
violations before being committed or merged.

- **ktlint** enforces the Kotlin coding conventions and
  project-specific formatting rules. The canonical
  configuration is managed via the ktlint Gradle plugin
  (`org.jlleitschuh.gradle.ktlint`).
- **detekt** enforces static analysis rules for code
  complexity, potential bugs, and style. The default
  detekt rule set applies unless explicitly overridden
  in `detekt.yml`.
- Developers MUST run `./gradlew ktlintCheck detekt`
  locally before pushing. CI pipelines MUST also enforce
  both checks as blocking gates.
- `@Suppress` annotations for detekt rules are permitted
  only when the suppression is justified by a comment
  explaining why the rule does not apply (e.g.,
  `@Suppress("TooGenericExceptionCaught")` with a
  rationale for catching broad exceptions in retry logic).
  Blanket suppressions of entire files or rule sets are
  prohibited.
- **Auto-formatting**: `./gradlew ktlintFormat` MAY be
  used to auto-fix formatting issues, but the developer
  MUST review the changes before committing. Blind
  auto-format commits are discouraged.
- New custom detekt rules or ktlint overrides MUST be
  proposed as a constitution amendment if they relax
  existing standards.

## Technology Stack

The following technology choices are non-negotiable for
this project:

| Concern            | Choice                              |
|--------------------|-------------------------------------|
| Language           | Kotlin 2.x                          |
| Framework          | Ktor (latest stable)                |
| Build system       | Gradle Kotlin DSL                   |
| Dependency inject. | Koin (latest stable)                |
| Database           | PostgreSQL via Exposed ORM          |
| Serialization      | kotlinx.serialization               |
| Testing            | kotest + ktor-server-test-host      |
| Integration tests  | testcontainers (PostgreSQL)         |
| API docs           | OpenAPI / Swagger (Ktor plugin)     |
| Code style         | ktlint + detekt                     |
| Containerization   | Docker + Docker Compose             |
| API style          | REST with URL versioning (`/v1/`)   |

Deviations from this stack require explicit justification
and amendment of this constitution.

## Development Workflow

### Code Quality Gates

1. **ktlint** and **detekt** MUST pass with zero violations
   before a PR is opened.
2. All tests MUST pass (unit, integration, contract).
3. Code review MUST verify Clean Architecture compliance
   (no layer violations).
4. OpenAPI spec MUST be updated for any endpoint changes.

### Versioning & Breaking Changes

- The API MUST follow semantic versioning (MAJOR.MINOR.PATCH).
- Breaking changes to public API endpoints MUST:
  1. Increment the URL version (e.g., `/v1/` → `/v2/`).
  2. Include a migration guide in the PR description.
  3. Maintain the previous version for a documented
     deprecation period.
- Internal library versioning follows SemVer for the
  project artifact.

### Performance Standards (Future Goal)

The following targets are aspirational for future phases
and MUST NOT drive implementation decisions in the
current phase:

- API response time target: < 200ms at p95 for standard
  CRUD operations.
- N+1 queries are prohibited (enforced via code review,
  not via tooling in this phase).
- No caching, benchmarks, or load testing infrastructure
  is required until core CRUD functionality is complete
  and stable.

Current phase priority: correctness and simplicity over
performance optimization.

## Governance

This constitution is the highest authority for development
decisions in this project. It supersedes informal
agreements, personal preferences, and ad-hoc practices.

**Amendment procedure**:

1. Propose the change as a PR modifying this file.
2. Document the rationale and impact assessment.
3. All active contributors MUST review and approve.
4. Update the version according to SemVer rules:
   - MAJOR: Principle removal or incompatible redefinition.
   - MINOR: New principle or materially expanded guidance.
   - PATCH: Clarification, wording, or typo fixes.

**Compliance review**: Every PR and code review MUST verify
adherence to these principles. Constitution violations are
blocking issues.

**Version**: 1.5.0 | **Ratified**: 2026-03-26 | **Last Amended**: 2026-03-27

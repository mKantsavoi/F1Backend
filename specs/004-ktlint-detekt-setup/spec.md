# Feature Specification: Add ktlint and detekt Static Analysis

**Feature Branch**: `004-ktlint-detekt-setup`
**Created**: 2026-03-27
**Status**: Draft
**Input**: User description: "Add ktlint and detekt static analysis plugins to the project build. The constitution requires these linters to pass before any PR, but they are not currently installed."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Run Code Formatting Checks (Priority: P1)

A developer runs the code formatting check command to verify that all Kotlin source files in the project conform to the project's code style conventions. The command succeeds with zero violations, giving the developer confidence that their code meets formatting standards before opening a PR.

**Why this priority**: Code formatting is the most fundamental quality gate. Without it, PRs cannot satisfy the constitution's linter requirement.

**Independent Test**: Can be fully tested by running the formatting check against the codebase and verifying it exits with success and zero violations.

**Acceptance Scenarios**:

1. **Given** the formatting check tool is configured in the build, **When** a developer runs the formatting check, **Then** the task completes successfully with zero violations on all existing source files.
2. **Given** a source file with a formatting violation (e.g., incorrect indentation), **When** a developer runs the formatting check, **Then** the task fails and reports the specific file and line with the violation.

---

### User Story 2 - Run Static Code Analysis (Priority: P1)

A developer runs the static analysis check to analyze all Kotlin source files. The command succeeds with zero violations, confirming the code meets quality standards for complexity, naming, and common pitfalls.

**Why this priority**: Static analysis catches bugs and code smells that formatting checks miss. Both tools together form the constitution's linter requirement.

**Independent Test**: Can be fully tested by running the static analysis check against the codebase and verifying it exits with success and zero violations.

**Acceptance Scenarios**:

1. **Given** the static analysis tool is configured with a default configuration, **When** a developer runs the analysis, **Then** the task completes successfully with zero violations on all existing source files.
2. **Given** a source file with a code smell (e.g., an overly complex function), **When** a developer runs the analysis, **Then** the task fails and reports the specific issue, file, and line.

---

### User Story 3 - Auto-Fix Formatting Issues (Priority: P2)

A developer runs the auto-format command to automatically fix formatting issues in Kotlin source files, reducing manual effort when addressing style violations.

**Why this priority**: Auto-fixing is a productivity enhancement that builds on the core formatting check. It is not required for the quality gate but makes compliance easier.

**Independent Test**: Can be fully tested by introducing a formatting violation in a source file, running the auto-formatter, and verifying the file is corrected.

**Acceptance Scenarios**:

1. **Given** a source file with auto-fixable formatting issues, **When** a developer runs the auto-format command, **Then** the file is reformatted to comply with the project's code style.
2. **Given** all source files already comply with formatting rules, **When** a developer runs the auto-format command, **Then** no files are modified.

---

### User Story 4 - Build Remains Unaffected (Priority: P1)

The existing project build continues to compile, run tests, and succeed without being affected by the addition of the linting plugins.

**Why this priority**: The new tooling must not break existing functionality. This is a non-negotiable constraint.

**Independent Test**: Can be fully tested by running the full build and verifying all compilation and test tasks pass.

**Acceptance Scenarios**:

1. **Given** the linting plugins are added to the build, **When** a developer runs the full build, **Then** the build succeeds and all existing tests pass.
2. **Given** the linting plugins are added to the build, **When** a developer runs the full build, **Then** no existing endpoints or services have changed behavior.

---

### Edge Cases

- What happens when a new Kotlin file is added without running the formatter? The formatting check should catch the violations and fail.
- How does the static analyzer handle generated code or build output? Only main and test source sets should be analyzed, not generated files.
- What happens if a developer has a different tooling version locally? The plugin versions should be pinned in the build configuration to ensure consistency.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The build system MUST include a code formatting plugin that provides check and format tasks for all Kotlin source files.
- **FR-002**: The build system MUST include a static analysis plugin that provides an analysis task for all Kotlin source files.
- **FR-003**: The formatting check MUST analyze all Kotlin source files in the project (main and test source sets).
- **FR-004**: The static analysis MUST use a project-level configuration file with reasonable defaults for a small backend project.
- **FR-005**: The static analysis configuration MUST NOT enable every possible rule; it should focus on practical, high-value checks.
- **FR-006**: All existing source files MUST pass both the formatting check and static analysis after setup, with any violations fixed as part of this work.
- **FR-007**: The existing build task MUST continue to work without changes to its behavior.
- **FR-008**: All existing tests MUST continue to pass.
- **FR-009**: No functional changes to any endpoint or service are permitted.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The formatting check completes with zero violations across all source files.
- **SC-002**: The static analysis check completes with zero violations across all source files.
- **SC-003**: The full project build succeeds with all tests passing.
- **SC-004**: An auto-format command is available and can fix formatting issues automatically.
- **SC-005**: The project's linter requirement (as stated in the constitution) is now enforceable via build tasks that any developer can run locally.

## Assumptions

- The project uses Gradle as its build system.
- The formatting tool will use default Kotlin coding conventions without custom overrides.
- The static analysis configuration will be stored as a configuration file within the project.
- Existing code violations (if any) will be fixed as formatting/style changes only, with no functional modifications.
- The linting tasks are run explicitly by developers or CI; they are not automatically wired into the main build task.

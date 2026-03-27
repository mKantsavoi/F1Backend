# Tasks: Add ktlint and detekt Static Analysis

**Input**: Design documents from `/specs/004-ktlint-detekt-setup/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md

**Tests**: Not requested in the feature specification. No test tasks included.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add plugin declarations and version catalog entries

- [x] T001 [P] Add ktlint and detekt version entries and plugin declarations to gradle/libs.versions.toml
- [x] T002 [P] Apply ktlint and detekt plugins via version catalog aliases in build.gradle.kts

**Checkpoint**: Gradle sync succeeds with both plugins recognized. `./gradlew tasks` lists `ktlintCheck`, `ktlintFormat`, and `detekt` tasks.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Generate and configure detekt baseline configuration

**WARNING**: No user story validation can pass until this phase is complete

- [x] T003 Generate default detekt configuration file via `./gradlew detektGenerateConfig` and save as detekt.yml at project root
- [x] T004 Review and tune detekt.yml to use reasonable defaults for a small backend project (disable overly strict or irrelevant rules while keeping high-value checks)

**Checkpoint**: `./gradlew detekt --dry-run` and `./gradlew ktlintCheck --dry-run` both resolve without configuration errors.

---

## Phase 3: User Story 1 - Run Code Formatting Checks (Priority: P1) MVP

**Goal**: `./gradlew ktlintCheck` passes with zero violations on all existing source files.

**Independent Test**: Run `./gradlew ktlintCheck` and verify exit code 0 with no violations reported.

### Implementation for User Story 1

- [x] T005 [US1] Run `./gradlew ktlintCheck` to identify all formatting violations in existing source files
- [x] T006 [US1] Run `./gradlew ktlintFormat` to auto-fix all fixable formatting violations across src/main/kotlin/ and src/test/kotlin/
- [x] T007 [US1] Manually fix any remaining ktlint violations that cannot be auto-fixed in affected source files
- [x] T008 [US1] Verify `./gradlew ktlintCheck` passes with zero violations

**Checkpoint**: `./gradlew ktlintCheck` exits with zero violations. All formatting issues resolved.

---

## Phase 4: User Story 2 - Run Static Code Analysis (Priority: P1)

**Goal**: `./gradlew detekt` passes with zero violations on all existing source files.

**Independent Test**: Run `./gradlew detekt` and verify exit code 0 with no violations reported.

### Implementation for User Story 2

- [x] T009 [US2] Run `./gradlew detekt` to identify all static analysis violations in existing source files
- [x] T010 [US2] Fix detekt violations in source files under src/main/kotlin/ (style fixes only, no functional changes)
- [x] T011 [US2] Fix detekt violations in test files under src/test/kotlin/ (style fixes only, no functional changes)
- [x] T012 [US2] If any violations cannot be fixed without functional changes, add targeted @Suppress annotations or adjust detekt.yml thresholds with justification
- [x] T013 [US2] Verify `./gradlew detekt` passes with zero violations

**Checkpoint**: `./gradlew detekt` exits with zero violations. All static analysis issues resolved.

---

## Phase 5: User Story 3 - Auto-Fix Formatting Issues (Priority: P2)

**Goal**: `./gradlew ktlintFormat` is available and correctly auto-fixes formatting issues.

**Independent Test**: Introduce a deliberate formatting violation in a source file, run `./gradlew ktlintFormat`, verify the file is corrected, then revert.

### Implementation for User Story 3

- [x] T014 [US3] Verify `./gradlew ktlintFormat` task is registered and runs without errors (provided by ktlint plugin — no additional configuration needed)

**Checkpoint**: `ktlintFormat` task works. This is automatically provided by the ktlint plugin from Phase 1.

---

## Phase 6: User Story 4 - Build Remains Unaffected (Priority: P1)

**Goal**: `./gradlew build` succeeds with all existing tests passing after all linting changes.

**Independent Test**: Run `./gradlew build` and verify all compilation and test tasks pass.

### Implementation for User Story 4

- [x] T015 [US4] Run `./gradlew build` and verify all compilation tasks succeed
- [x] T016 [US4] Verify all existing tests pass (no test failures introduced by formatting/style fixes)

**Checkpoint**: Full build succeeds. No functional regressions.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation that all success criteria are met

- [x] T017 Run `./gradlew ktlintCheck` one final time to confirm zero violations
- [x] T018 Run `./gradlew detekt` one final time to confirm zero violations
- [x] T019 Run `./gradlew build` one final time to confirm full build passes with all tests

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately. T001 and T002 can run in parallel.
- **Foundational (Phase 2)**: Depends on Phase 1 completion. T003 then T004 sequentially.
- **User Story 1 (Phase 3)**: Depends on Phase 2 completion.
- **User Story 2 (Phase 4)**: Depends on Phase 2 completion. Can run in parallel with Phase 3.
- **User Story 3 (Phase 5)**: Depends on Phase 1 completion (plugin provides the task). Can validate after Phase 3.
- **User Story 4 (Phase 6)**: Depends on Phase 3 and Phase 4 completion (all fixes must be applied first).
- **Polish (Phase 7)**: Depends on all previous phases.

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Foundational (Phase 2) only
- **User Story 2 (P1)**: Depends on Foundational (Phase 2) only — can run in parallel with US1
- **User Story 3 (P2)**: Depends on Phase 1 only (plugin provides ktlintFormat automatically)
- **User Story 4 (P1)**: Depends on US1 + US2 completion (all code changes must be done)

### Parallel Opportunities

- T001 and T002 can run in parallel (different files)
- Phase 3 (US1 - ktlint fixes) and Phase 4 (US2 - detekt fixes) can run in parallel since they modify code for different concerns

---

## Parallel Example: Setup Phase

```bash
# Launch both setup tasks together (different files):
Task: "Add versions to gradle/libs.versions.toml"
Task: "Apply plugins in build.gradle.kts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (add plugins)
2. Complete Phase 2: Foundational (configure detekt)
3. Complete Phase 3: User Story 1 (ktlintCheck passes)
4. **STOP and VALIDATE**: `./gradlew ktlintCheck` passes

### Incremental Delivery

1. Setup + Foundational → Plugins configured
2. User Story 1 → ktlintCheck passes (MVP!)
3. User Story 2 → detekt passes
4. User Story 3 → ktlintFormat verified
5. User Story 4 → Full build verified
6. Polish → Final validation of all success criteria

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- All code fixes in Phases 3-4 are formatting/style only — no functional changes permitted (FR-009)
- Commit after each phase for clean git history
- If detekt violations require @Suppress annotations, document justification in the task

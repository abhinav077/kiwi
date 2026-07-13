# Kiwi — Progress Tracker

## Status values

- `NOT_STARTED`
- `IN_PROGRESS`
- `BLOCKED`
- `COMPLETE`
- `NEEDS_REVIEW`

## Current checkpoint

- Current phase: Phase 1 — Local persistence and sync foundation
- Current task: 1.5 Create restore flow
- Status: COMPLETE
- Active model: Sol for authenticated transactional restoration

## Setup completed

| Item | Status | Notes |
|---|---|---|
| Android Studio | COMPLETE | Quail 1 installed |
| Android SDK | COMPLETE | SDK/platform tools/emulator available |
| Emulator acceleration | COMPLETE | KVM available; physical tablet preferred |
| Physical tablet run | COMPLETE | Kiwi runs on connected tablet |
| Android project | COMPLETE | Kotlin + Compose, package `com.abhinavsirohi.kiwi` |
| Git/GitHub | COMPLETE | Private repository connected |
| Supabase project | COMPLETE | Project created |
| Google OAuth | COMPLETE | Credentials configured; final approved email pending |
| `allowed_users` and `profiles` | COMPLETE | RLS enabled |
| Private diary photo bucket | COMPLETE | `diary-photos` policies created |
| Debug SHA-1 | COMPLETE | Recorded in setup notes, never hard-code secrets |
| Gradle memory tuning | COMPLETE | Reduced for 8 GB laptop |

## Task board

| ID | Task | Status | Model | Verification |
|---|---|---|---|---|
| 0.1 | Audit repository and generated template | COMPLETE | Terra | Package, Gradle, manifest, source tree, and `assembleDebug` audited |
| 0.2 | Stabilize Gradle and dependency strategy | COMPLETE | Sol | `testDebugUnitTest assembleDebug` passed |
| 0.3 | Create architecture package skeleton | COMPLETE | Sol | Package review + `testDebugUnitTest assembleDebug` passed |
| 0.4 | Create design tokens and shared theme | NEEDS_REVIEW | Luna | Build/previews compile; tablet run blocked by ADB environment |
| 0.5 | Create navigation shell | NEEDS_REVIEW | Luna | Build passes; destination runtime check blocked by ADB environment |
| 1.1 | Create Room database foundation | COMPLETE | Sol | Unit/build checks and 3 instrumented tests passed on physical tablet |
| 1.2 | Create repositories and use cases | COMPLETE | Sol | Domain boundary unit tests and debug build passed |
| 1.3 | Create sync queue and worker | COMPLETE | Sol | Unit/build checks and 7 instrumented tests passed on physical tablet |
| 1.4 | Configure Supabase client | NEEDS_REVIEW | Sol | Unit/build and regression tests pass; live authenticated request pending local public configuration and sign-in session |
| 1.5 | Create restore flow | COMPLETE | Sol | Unit/build checks and 9 instrumented tests passed on physical tablet |

## Update protocol

After every task, record:

- Date
- Task ID
- Model used
- Files changed
- Verification commands
- Result
- Known issues
- Next task

## Task log

### 2026-07-13 — Setup checkpoint

- Android project runs on physical tablet.
- GitHub and Supabase foundations created.
- The project documentation set is being added before feature coding.
- Remaining decision: move local project editing to Codex in VS Code or continue manual patching.

### 2026-07-13 — Phase 0, Task 0.1 repository audit

- Model: Terra.
- Files changed: `context/PROGRESS_TRACKER.md`.
- Repository structure: one `app` module with Kotlin/Compose; package, namespace, and application ID consistently use `com.abhinavsirohi.kiwi`.
- Current application code and resources are the generated Compose template (`Hello Android` plus default theme, launcher, backup, and test assets); this is expected before later foundation and UI tasks.
- Gradle: wrapper uses Gradle 9.4.1; AGP 9.2.1 and Kotlin 2.2.10 build successfully with the current constrained Gradle settings. The catalog contains Navigation, WorkManager, DataStore, and Biometric versions while the module declares those dependencies directly; defer dependency-centralization decisions to Task 0.2.
- Verification: `./gradlew assembleDebug` — PASS; debug APK generated at `app/build/outputs/apk/debug/app-debug.apk`.
- Conflict/known issue: existing user-owned `.gitignore` change has a trailing blank line reported by `git diff --check`; it was not modified. No tracked credential, keystore, or local-properties file was found.
- Next unblocked task: 0.2 Stabilize Gradle and dependency strategy (Sol).

### 2026-07-13 — Phase 0, Task 0.2 build stabilization

- Model: Sol.
- Files changed: `app/build.gradle.kts`, `gradle/libs.versions.toml`, and `context/PROGRESS_TRACKER.md`.
- Memory configuration: retained the existing constrained settings (`-Xmx1024m`, daemon and parallel execution disabled, one Gradle worker, in-process Kotlin compilation); no memory increase was required.
- Dependency strategy: moved Navigation Compose, WorkManager, DataStore Preferences, and Biometric module declarations to the existing version catalog; all declared app libraries now use catalog aliases.
- Compatibility: explicitly set Kotlin bytecode to JVM 11 to match Java source and target compatibility. Gradle 9.4.1 runs on JDK 21.0.10 with AGP 9.2.1.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; 42 actionable tasks, exit code 0.
- Known issue: Gradle could not strip `libandroidx.graphics.path.so` and `libdatastore_shared_counter.so`; it packaged them unchanged and the build succeeded. This is non-blocking for the debug build.
- Next unblocked task: 0.3 Create architecture package skeleton (Sol).

### 2026-07-13 — Phase 0, Task 0.3 architecture package skeleton

- Model: Sol.
- Files changed: `AndroidManifest.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `KiwiApplication.kt`, foundational files under `core/common/` and `domain/usecase/`, documented package-directory markers, and `context/PROGRESS_TRACKER.md`.
- Application foundation: added and registered `KiwiApplication` without initialization or feature logic.
- Core contracts: added immutable `AppResult` and `UiState` types, injectable coroutine dispatcher boundaries with Android defaults, and a suspend `UseCase` contract.
- Package structure: created the documented empty `core`, `data`, `domain`, and `feature` package paths with `.gitkeep` markers so they remain durable in Git.
- Dependency boundary: declared `kotlinx-coroutines-android` 1.9.0 directly through the version catalog, matching the version already selected by the existing dependency graph.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; 42 actionable tasks, exit code 0.
- Known issue: the existing debug-build warning for unstripped `libandroidx.graphics.path.so` and `libdatastore_shared_counter.so` remains non-blocking. The unrelated user-owned `.gitignore` trailing blank line remains unchanged.
- Next unblocked task: 0.4 Create design tokens and shared theme (Luna).

### 2026-07-13 — Phase 0, Task 0.4 design foundation

- Model: Luna.
- Files changed: `ui/theme/Color.kt`, `ui/theme/Type.kt`, `ui/theme/Dimensions.kt`, `ui/theme/Theme.kt`, shared design files under `core/design/`, and `context/PROGRESS_TRACKER.md`.
- Design foundation: replaced generated purple Material values with the documented Kiwi palette, typography, 4dp spacing grid, dimensions, light/dark color schemes, and rounded shapes.
- Shared UI: added the layered organic `KiwiBackground`, reusable `KiwiButton`, `KiwiCard`, `KiwiChip`, and a static Compose preview catalog.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; 42 actionable tasks, exit code 0.
- Known limitations: the approved `paper-grain.svg` uses `feTurbulence`, which Android’s native vector pipeline cannot render directly; no replacement decoration was invented. Tablet verification is blocked by the environment: `adb devices` failed because `/dev/bus/usb` is unavailable and ADB could not install its socket listener (`Operation not permitted`).
- Status: NEEDS_REVIEW pending tablet verification.

### 2026-07-13 — Phase 0, Task 0.5 navigation shell

- Model: Luna.
- Files changed: `MainActivity.kt`, `core/navigation/KiwiDestination.kt`, `core/navigation/KiwiNavigation.kt`, and `context/PROGRESS_TRACKER.md`.
- Navigation: added the root graph with Today, Calendar, Diary, Ask Kiwi, and More placeholder destinations; bottom-dock selection preserves single-top navigation and saved state.
- Shell: added edge-to-edge safe-area handling, floating 72dp dock treatment, accessible destination semantics, and a centered 720dp maximum content width for tablets.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; 42 actionable tasks, exit code 0.
- Known limitation: runtime destination verification could not run. `adb devices` failed because `/dev/bus/usb` is unavailable and ADB could not install its socket listener (`Operation not permitted`).
- Status: NEEDS_REVIEW pending tablet/runtime navigation verification.

### 2026-07-13 — Phase 1, Task 1.1 Room database foundation

- Model: Sol.
- Reading mode: only the approved `PROJECT_OVERVIEW.md`, Task 1.1 section of `BUILD_PLAN.md`, `ARCHITECTURE.md`, `CODE_STANDARDS.md`, and `PROGRESS_TRACKER.md`.
- Files changed: `app/build.gradle.kts`, `gradle/libs.versions.toml`, exported Room schema under `app/schemas/`, database foundation under `core/database/`, task persistence files under `data/local/`, database instrumentation tests, and `context/PROGRESS_TRACKER.md`.
- Database foundation: added Room 2.8.4 with AGP built-in-Kotlin-compatible KSP 2.3.4, schema export, `KiwiDatabase`, converters, a non-destructive migration registry, and the initial task/subtask entities and DAO.
- Local-first metadata: both mutable entities include stable local IDs, optional remote IDs, user/device ownership, created/updated/deleted timestamps, sync status, and the last sync error. Tombstoned records remain stored for synchronization while active queries hide them.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; `./gradlew connectedDebugAndroidTest --console=plain` — PASS, 3 tests on physical tablet `SM-X115` (Android 16), including 2 Room database tests.
- Known limitation: schema version 1 has no historical schema to migrate from; the explicit migration registry is empty until the first schema change. Destructive migration is not enabled.
- Preserved unrelated changes: existing `.idea/misc.xml` and `gradle/gradle-daemon-jvm.properties` modifications were not changed.
- Next unblocked task: 1.2 Create repositories and use cases (Sol), pending separate user approval.

### 2026-07-13 — Phase 1, Task 1.2 repository contracts and use cases

- Model: Sol.
- Reading mode: only the approved Task 1.2 section of `BUILD_PLAN.md`, `ARCHITECTURE.md`, `CODE_STANDARDS.md`, `PROGRESS_TRACKER.md`, and the approved Task 1.1/core-domain source paths.
- Files changed: domain task/subtask and record-metadata models, `TaskRepository`, task use cases, task use-case unit tests, and `context/PROGRESS_TRACKER.md`.
- Domain boundary: added immutable Room-independent models and a repository contract for observing, saving, and tombstoning tasks and subtasks. Repository operations expose explicit `AppResult` values.
- Use cases: added focused observe, save, and tombstone entry points so future presentation code can depend on domain APIs instead of Room or Supabase.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; 44 actionable tasks, including 4 new task use-case tests. Domain source/test boundary scan found no Room, local-data, or Supabase imports; diff and secret checks passed.
- Known limitation: this task intentionally defines contracts only. A concrete Room-backed repository, entity/domain mappers, dependency injection, and sync behavior are not implemented because they were outside Task 1.2's approved scope.
- Preserved unrelated changes: existing `.idea/misc.xml` and `gradle/gradle-daemon-jvm.properties` modifications, plus completed Task 1.1 changes, were not altered outside the approved tracker update.
- Next unblocked task: 1.3 Create sync queue and worker (Sol), pending separate user approval.

### 2026-07-13 — Phase 1, Task 1.3 sync queue and worker

- Model: Sol.
- Reading mode: only the approved Task 1.3 documentation sections, build configuration, Task 1.1 Room foundation/schema/tests, and Task 1.2 domain contracts/tests.
- Implemented so far: version-2 pending-change queue schema and DAO, non-destructive `MIGRATION_1_2`, queue enum converters, retry/backoff and last-write-wins rules, injectable sync processor/worker/factory, connected-network work request, JVM tests, queue database tests, and migration test.
- Dependency alignment: explicitly aligned `kotlinx-serialization-json` to 1.8.1, matching Room 2.8.4's migration serializer. Dependency insight confirmed serialization JSON, core, and BOM resolve to 1.8.1 for Android tests.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; `./gradlew connectedDebugAndroidTest --console=plain` — PASS, 7 tests on physical tablet `SM-X115` (Android 16), including queue durability and version 1→2 migration preservation.
- Known limitation: the worker uses an injected `SyncProcessor`; the production Supabase processor and worker-factory registration remain intentionally deferred to Task 1.4.
- Preserved unrelated changes: existing `.idea/misc.xml` and `gradle/gradle-daemon-jvm.properties` modifications and completed earlier-task changes remain untouched.
- Next unblocked task: 1.4 Configure Supabase client (Sol), pending separate user approval.

### 2026-07-13 — Phase 1, Task 1.4 Supabase client

- Model: Sol.
- Reading mode: only the approved Task 1.4 documentation sections, build/application configuration, Task 1.3 sync/database/local foundation, relevant domain contracts/tests/schemas, `.gitignore`, and current official Supabase Kotlin installation/initialization/auth references.
- Files changed: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`, `KiwiApplication.kt`, remote configuration/client/session/error files under `data/remote/`, focused JVM tests, and `context/PROGRESS_TRACKER.md`.
- Client foundation: added Supabase Kotlin 3.6.0 through its BOM, Auth and PostgREST modules, the compatible Ktor Android engine, and internet permission. `KiwiApplication` exposes lazy client creation without crashing when configuration is absent.
- Configuration and security: loads only `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` from ignored `local.properties` into generated `BuildConfig`; no service-role key, OAuth secret, token, or credential value is logged or committed.
- Session and errors: added authenticated/unauthenticated session mapping that exposes user ID/email but not tokens, plus calm error categories for missing configuration, authentication, access denial, network failure, service failure, and unknown errors.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; `./gradlew connectedDebugAndroidTest --console=plain` — PASS, 7 existing tests on physical tablet `SM-X115` (Android 16). New configuration, safe-error, and session unit tests pass.
- Pending verification: `local.properties` currently contains neither required Supabase property name, and no authenticated app session is available. A live authenticated request was therefore not run. Task remains `NEEDS_REVIEW` rather than being reported complete.
- Known limitation: Google sign-in UI, OAuth deep-link handling, approved-account interaction, production sync processor, and worker-factory registration are outside the explicit Task 1.4 build-plan scope.
- Preserved unrelated changes: existing `.idea/misc.xml` and `gradle/gradle-daemon-jvm.properties` modifications and completed earlier-task changes remain untouched.
- Next action: add the public local configuration, establish an approved Google session in the later auth flow, and run the authenticated request/RLS verification before marking 1.4 complete.

### 2026-07-13 — Phase 1, Task 1.5 restore flow

- Model: Sol.
- Reading mode: only the approved Task 1.5 documentation sections and approved Phase 1 source/test/configuration paths; no credential values were opened or printed.
- Implemented so far: authenticated first-login restore coordinator, user-scoped snapshots, DataStore completion marker, transactional duplicate-safe Room writer, reminder recreation boundary, and focused coordinator/database tests.
- Restore behavior: requires an authenticated session, skips users already restored, rejects cross-user snapshots, applies tasks/subtasks transactionally with stable-ID and last-write-wins checks, preserves tombstones and newer local edits, recreates reminders after the database succeeds, and records completion last in DataStore.
- Boundaries: added remote snapshot, reminder recreation, and restore-state contracts without inventing Supabase task/subtask table names or reminder scheduling behavior that are not yet specified.
- Approved compile correction: replaced two prohibited Kotlin member-extension callable references with equivalent lambdas; no behavior changed.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; `./gradlew connectedDebugAndroidTest --console=plain` — PASS, 9 tests on physical tablet `SM-X115` (Android 16), including duplicate-safe last-write-wins and tombstone restore tests. Diff and credential checks passed.
- Known limitation: live cloud restoration awaits approved Supabase task/subtask schemas and RLS policies plus an authenticated app session. The workflow is verified through controlled remote/reminder fakes and real Room tests.
- Phase status: all Phase 1 numbered implementations are present. Task 1.4 remains `NEEDS_REVIEW` only for its live authenticated-request check.
- Next task: resolve Task 1.4 live verification when authentication is available, then begin Phase 2 — Task 2.1 only after separate user approval.

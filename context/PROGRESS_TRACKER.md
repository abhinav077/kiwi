# Kiwi — Progress Tracker

## Status values

- `NOT_STARTED`
- `IN_PROGRESS`
- `BLOCKED`
- `COMPLETE`
- `NEEDS_REVIEW`

## Current checkpoint

- Current phase: Phase 5 — Wellness tracker
- Current task: 5.5 Health alerts
- Status: COMPLETE
- Active model: Sol for health-safe pattern episodes and private notifications

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
| 2.1 | Implement welcome screen | COMPLETE | Luna | `testDebugUnitTest assembleDebug` passed |
| 2.2 | Implement Google sign-in | COMPLETE | Sol | Unit/build checks pass; live Google/Supabase sign-in passed on physical tablet |
| 2.3 | Implement approved-user gate | COMPLETE | Sol | Unit/build checks and live approved/unapproved RLS paths passed on physical tablet |
| 2.4 | Implement minimal setup | COMPLETE | Sol | Unit/build checks and 12 instrumented tests passed on physical tablet |
| 2.5 | Implement session restoration | COMPLETE | Sol | Unit/build checks, 12 instrumented tests, and cold-relaunch verification passed on physical tablet |
| 3.1 | Implement Today shell | COMPLETE | Luna | `testDebugUnitTest assembleDebug` passed |
| 3.2 | Implement task model and CRUD | COMPLETE | Sol | Unit/build checks, 14 instrumented tests, and migration launch passed on physical tablet |
| 3.3 | Implement subtasks | COMPLETE | Sol | Unit/build checks and 15 instrumented tests passed on physical tablet |
| 3.4 | Implement recurrence | COMPLETE | Sol | Unit/build checks, 17 instrumented tests, and migration launch passed on physical tablet |
| 3.5 | Implement offline interaction | COMPLETE | Sol | Unit/build checks and 18 instrumented tests passed on physical tablet |
| 4.1 | Implement calendar navigation | COMPLETE | Terra | `testDebugUnitTest assembleDebug` passed |
| 4.2 | Implement timeline | COMPLETE | Luna | `testDebugUnitTest assembleDebug` passed |
| 4.3 | Implement local reminders | COMPLETE | Sol | Unit/build and 19 instrumented tests passed on physical tablet |
| 4.4 | Implement reconciliation | COMPLETE | Sol | Unit/build and 20 instrumented tests passed on physical tablet |
| 5.1 | Implement cycle records | COMPLETE | Sol | Unit/build and 24 instrumented tests passed on physical tablet |
| 5.2 | Implement daily wellness fields | COMPLETE | Sol | Unit/build and 27 instrumented tests passed on physical tablet |
| 5.3 | Implement wellness interface | COMPLETE | Sol | Unit/build and 27 instrumented tests passed on physical tablet |
| 5.4 | Implement historical analytics | COMPLETE | Sol | Focused unit/build and 27 instrumented tests passed on physical tablet |
| 5.5 | Implement health alerts | COMPLETE | Sol | Unit/build and 29 instrumented tests passed on physical tablet |

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

### 2026-07-13 — Phase 2, Task 2.1 welcome

- Model: Luna.
- Reading mode: only the approved `BUILD_PLAN.md` Phase 2/Task 2.1 section, `PROJECT_OVERVIEW.md`, `PROGRESS_TRACKER.md`, `CODE_STANDARDS.md`, and relevant presentation/repository-structure sections of `ARCHITECTURE.md`.
- Files changed: `feature/onboarding/WelcomeScreen.kt`, `core/navigation/KiwiDestination.kt`, `core/navigation/KiwiNavigation.kt`, and this tracker.
- Behaviour: added the warm Kiwi opening screen with established background, typography, card, and button components; Continue navigates to a sign-in boundary placeholder reserved for Task 2.2. The bottom dock is hidden during onboarding routes.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; `git diff --check` — PASS; no secrets or generated build outputs were added to the working tree.
- Known limitation: Google sign-in and device runtime verification remain outside this task. Google sign-in belongs to Task 2.2.
- Next unblocked task: Phase 2, Task 2.2 Google sign-in, pending separate user approval.

### 2026-07-13 — Phase 2, Task 2.2 Google sign-in

- Model: Sol.
- Reading mode: only the approved Task 2.2 sections of `BUILD_PLAN.md`, `ARCHITECTURE.md`, `CODE_STANDARDS.md`, `PROJECT_OVERVIEW.md`, `PROGRESS_TRACKER.md`, `SCREEN_SPECIFICATIONS.md`, and `UI_RULES.md`; `COMPONENT_SPECIFICATIONS.md` had no sign-in-specific section and was not opened beyond its approved heading search.
- Files changed: Gradle dependency/configuration files, the existing onboarding navigation files, `data/remote/SupabaseGoogleAuthGateway.kt`, Google credential/sign-in screen and ViewModel files under `feature/onboarding/`, focused ViewModel tests, and this tracker.
- Authentication: added Android Credential Manager and Google ID dependencies, nonce-protected native Google ID-token retrieval, Supabase `IDToken` exchange, safe authenticated-session mapping, and a public `GOOGLE_WEB_CLIENT_ID` BuildConfig field sourced from ignored `local.properties`.
- UI/state: replaced the sign-in placeholder with explicit idle, loading, cancellation, failure, and authenticated states. Successful authentication navigates to an approved-user-gate placeholder; Task 2.3 allowlist/RLS enforcement was not implemented.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS, including three focused sign-in ViewModel tests; `git diff --check` and focused secret/generated-output checks — PASS.
- Known limitation: a live sign-in was not attempted because Task 1.4 records that public Supabase configuration and an authenticated device session are still pending. `GOOGLE_WEB_CLIENT_ID`, `SUPABASE_URL`, and `SUPABASE_PUBLISHABLE_KEY` must be supplied through ignored local configuration before device verification. No credential values were opened, printed, logged, or committed.
- Status: NEEDS_REVIEW pending successful Google account selection, Supabase session creation, and navigation to the Task 2.3 boundary on a device.
- Next task: verify Task 2.2 live when public local configuration is available, then implement Phase 2, Task 2.3 only after separate user approval.

### 2026-07-13 — Phase 2, Task 2.3 approved-user gate

- Model: Sol.
- Reading mode: reused approved Task 2.2 context and read only the approved Task 2.3 sections of `BUILD_PLAN.md`, `ARCHITECTURE.md`, `CODE_STANDARDS.md`, `PROJECT_OVERVIEW.md`, and `PROGRESS_TRACKER.md`. Supabase MCP read-only metadata access was used to inspect the Kiwi `allowed_users`/`profiles` schemas, RLS policies, `is_approved_user()` definition, and grants; no table rows or credential values were read.
- Files changed: `data/repository/ApprovedUserRepository.kt`, approved-user screen/ViewModel files under `feature/onboarding/`, onboarding navigation files, focused repository/ViewModel tests, and this tracker.
- Access enforcement: requires an authenticated session with an email, queries only the matching active `allowed_users` row through the authenticated PostgREST client, relies on RLS to hide unapproved rows, and locally validates the returned active row against the normalized session email. No approved email is embedded in production code.
- UI/state: added checking, approved, denied, retryable error, signing-out, and signed-out states. Access denied and errors provide Retry and Sign out; approval navigates to the Task 2.4 profile-setup boundary and sign-out returns to Google sign-in.
- Supabase contract verified: `allowed_users(email, is_active, created_at)` and `profiles` have RLS enabled; the SELECT policy exposes only the authenticated user's row when `is_approved_user()` confirms a matching active JWT email. No database or policy changes were required or made.
- Verification: initial build found a missing `dp` import in `ApprovedUserScreen.kt`; after explicit user approval, the import was added. `./gradlew testDebugUnitTest assembleDebug --console=plain` then passed with 46 tasks, including three local allowlist evaluator tests and five access-gate ViewModel tests. `git diff --check`, focused secret checks, and generated-output checks passed; example.com identities appear only in tests.
- Known limitation: live approved and unapproved Google identities were not exercised because local public auth configuration/device sign-in remains pending from Tasks 1.4 and 2.2. Task status is `NEEDS_REVIEW` until both identities confirm RLS behavior through the app.
- Next task: complete live Task 2.2/2.3 authentication and RLS verification when local public configuration is available, then implement Phase 2, Task 2.4 only after separate user approval.

### 2026-07-14 — Phase 2, Task 2.4 minimal setup

- Model: Sol.
- Reading mode: only the approved Task 2.4 sections of `BUILD_PLAN.md`, `ARCHITECTURE.md`, `CODE_STANDARDS.md`, `PROJECT_OVERVIEW.md`, and `PROGRESS_TRACKER.md`; setup-related headings in `SCREEN_SPECIFICATIONS.md` and `UI_RULES.md` were searched and no dedicated minimal-setup specification was present.
- Files changed: the Room database/schema and migration tests, profile entity/DAO/repository/domain/use-case files, application database/device-ID wiring, the sync record type, minimal-setup screen/ViewModel and focused tests, onboarding navigation, generated Room schema version 3, and this tracker.
- Behaviour: the approved user is asked only for a preferred name. Continue trims and validates the name, saves the profile transactionally to Room with pending sync metadata, adds a durable profile UPSERT to the existing pending-change queue, and navigates to Today. No Supabase profile write or later sync processing was added in this task.
- Persistence: added a non-destructive Room 2-to-3 migration for a single profile per authenticated user. Updating the name preserves its original creation time and replaces the stable queued profile UPSERT instead of accumulating duplicates.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; `./gradlew connectedDebugAndroidTest --console=plain` — PASS with 12 tests on `SM-X115 - 16`, including Room 2-to-3 migration and transactional local profile/queue persistence; `git diff --check` — PASS.
- Known limitation: Tasks 2.2 and 2.3 remain `NEEDS_REVIEW` because their live Google/Supabase approved and unapproved identity checks were not completed. Task 2.4 itself is verified through unit and physical-device persistence tests.
- Next unblocked task: Phase 2, Task 2.5 session restore, pending separate user approval.

### 2026-07-14 — Phase 2, Tasks 2.2 and 2.3 live verification

- Model: Sol.
- Reading mode: reused established authentication/RLS context and read only the approved Task 2.2/2.3 source paths, configuration presence, device runtime state/logs, and this tracker. Public configuration values and account identities were not displayed or recorded.
- Configuration: `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, and `GOOGLE_WEB_CLIENT_ID` were confirmed present and nonblank in ignored local configuration without printing their values.
- Approved path: on physical tablet `SM-X115`, Google account selection created a Supabase session, the RLS-backed allowlist accepted the approved identity, minimal setup appeared, and saving a preferred name navigated to Today.
- Denied path: after the user-approved local app-data reset, a different unapproved Google identity reached the Access denied screen. Its Sign out action cleared the session and returned to Google sign-in.
- Verification: `./gradlew testDebugUnitTest assembleDebug installDebug --console=plain` — PASS; live approved and denied account paths — PASS; denied-path Sign out — PASS; final `git diff --check` — PASS.
- Device state: Kiwi is left signed out. The approved reset removed the locally entered preferred name and other Kiwi app-local data; no Supabase rows were changed by the reset.
- Status: Tasks 2.2 and 2.3 are COMPLETE. Main-screen Settings/More sign-out remains outside these tasks and requires its own approved build-plan scope.
- Next unblocked task: Phase 2, Task 2.5 session restore, pending separate user approval.

### 2026-07-14 — Phase 2, Task 2.5 session restoration

- Model: Sol.
- Reading mode: only the approved Task 2.5 sections of `BUILD_PLAN.md`, authentication/session/startup/local-first sections of `ARCHITECTURE.md`, relevant `CODE_STANDARDS.md` and `PROJECT_OVERVIEW.md` sections, Task 1.5 and Tasks 2.2-2.4 tracker entries, and directly involved startup/session/access/profile/restore source and tests.
- Files changed: session-restoration domain model/repository/use case, Room-backed local-profile lookup and startup repository, session-restoration screen/ViewModel and focused tests, profile DAO existence query, startup navigation destinations/wiring, and this tracker.
- Behaviour: startup now waits for Supabase Auth storage initialization, then routes a first launch to Welcome, an expired returning session to sign-in, an approved session without a profile to minimal setup, an approved session with a matching local profile directly to Today, and an unapproved session to Access denied.
- Offline safety: a cached session may open Today offline only when Room contains a profile owned by that same session user ID. Without that ownership proof, Kiwi shows a calm retry/sign-out state and does not open local user data.
- Sign-in integration: every successful Google sign-in returns through the restoration decision, preventing returning users from being asked for their preferred name again while still sending new approved users to setup.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; `./gradlew connectedDebugAndroidTest --console=plain` — PASS with 12 tests on `SM-X115 - 16`; `./gradlew installDebug --console=plain` — PASS; signed-out/no-profile launch opened Welcome; approved sign-in and setup reached Today; cold relaunch restored the cached session and opened directly to Today; `git diff --check` — PASS.
- Approved corrections during verification: fixed a delegated-state smart-cast compile error, corrected one stale test-double name, and awaited Supabase Auth initialization after live cold relaunch initially reached sign-in before cached-session loading completed.
- Known limitation: offline-safe routing is covered by focused ownership/failure unit tests; the tablet network radios were not toggled during this task. The Phase 1 production cloud task/subtask restore source and reminder implementation remain separately deferred and were not added here.
- Next unblocked task: Phase 3, Task 3.1 Today shell, pending separate user approval.

### 2026-07-14 — Phase 3, Task 3.1 Today shell

- Model: Luna.
- Reading mode: only the approved project overview, Phase 3 / Task 3.1 build-plan section, progress tracker, and directly involved Today/navigation/shared-design source files.
- Files changed: Today shell screen under `feature/today/`, Today destination wiring in `core/navigation/KiwiNavigation.kt`, and this tracker.
- Behaviour: added the Today composition with greeting, next-task hero, progress summary, time-of-day groups, self-care moment, quick actions, and Ask Kiwi entry. Task data and CRUD remain outside this task; the shell uses calm static empty-state content until Task 3.2 provides live task operations.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; 46 actionable tasks, exit code 0.
- Known limitation: runtime tablet verification and live task interactions are not part of this shell task.
- Next unblocked task: Phase 3, Task 3.2 Task model and CRUD, pending separate user approval.

### 2026-07-14 — Phase 3, Task 3.2 task model and CRUD

- Model: Sol.
- Reading mode: only the approved Task 3.2 build-plan section, local-first/data/sync architecture sections, relevant code standards and project decisions, progress tracker, and directly involved task persistence/domain/Today source and tests.
- Files changed: task entity/DAO/domain/repository/use-case files; Room database migration and schema; Today screen/ViewModel; focused JVM and instrumented task tests; directly impacted existing DAO tests; and this tracker.
- Behaviour: added timed and untimed tasks with title, description, category, priority, notes, date, completion, editing, and confirmation-gated deletion. Today now observes user-scoped Room data and exposes create, edit, complete/undo, and delete actions with calm loading, empty, and error states.
- Local-first persistence: added a concrete authenticated `RoomTaskRepository`. Every create, edit, completion, and tombstone is committed transactionally with a stable `TASK:<localId>` pending-change entry; records retain ownership and synchronization metadata, and deleted tasks remain stored as tombstones while disappearing from active queries.
- Migration: database version 4 adds nullable description/time columns and backward-compatible category/priority defaults through non-destructive `MIGRATION_3_4`.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; `./gradlew connectedDebugAndroidTest --console=plain` — PASS with 14 tests on `SM-X115 - 16`; `./gradlew installDebug --console=plain` — PASS; existing app data opened through migration 3→4, `MainActivity` resumed, and no Android runtime crash was logged.
- Scoped corrections during verification: replaced prohibited member-extension callable references with lambdas, corrected a task test fake that referenced a private helper, and updated one restore database test for the new user-scoped DAO query.
- Known limitation: Task 3.2 queues changes locally but does not add the production Supabase task processor; full offline interaction/sync processing remains Task 3.5. Subtasks and recurrence remain Tasks 3.3 and 3.4.
- Next unblocked task: Phase 3, Task 3.3 Subtasks, pending separate user approval.

### 2026-07-14 — Phase 3, Task 3.3 subtasks

- Model: Sol.
- Reading mode: reused established Phase 3 local-first task context and read only the approved Task 3.3 build-plan/tracker sections plus directly involved subtask/task repository, Room DAO, Today, and test files.
- Files changed: subtask domain/use-case/repository paths, user-scoped subtask DAO queries, Today ViewModel/screen controls, focused subtask progress and Room repository tests, directly affected task/database tests, and this tracker.
- Behaviour: added authenticated subtask creation, editing, completion/undo, move up/down ordering, and confirmation-gated deletion. Today now observes each task’s subtasks, displays completed/total progress, renders subtask checkboxes and ordering controls, and supports add/edit/delete actions.
- Local-first persistence: subtask writes update Room transactionally and enqueue stable `SUBTASK:<localId>` UPSERT/DELETE changes. Parent task ownership and subtask ownership are checked before writes; tombstones remain stored and disappear from active queries.
- Progress: added a pure `calculateSubtaskProgress` rule with empty-list safety and focused unit coverage.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; `./gradlew connectedDebugAndroidTest --console=plain` — PASS with 15 tests on `SM-X115 - 16`; no remaining compile warnings from this task; `git diff --check` — PASS.
- Known limitation: subtask cloud processing remains part of the later sync processor scope; recurrence and broader offline interaction remain Tasks 3.4 and 3.5.
- Next unblocked task: Phase 3, Task 3.4 Recurrence, pending separate user approval.

### 2026-07-14 — Phase 3, Task 3.4 recurrence

- Model: Sol.
- Reading mode: reused established Phase 3 context and read only the approved Task 3.4 build-plan/tracker sections plus directly involved task entity/domain/DAO/repository, Today, database/sync, and test files.
- Files changed: recurrence domain model/tests, task entity/domain/DAO/repository, Room database migration/schema, Today ViewModel/screen, repository/migration tests, and this tracker.
- Rules: added explicit None, Daily, Weekly, and Monthly frequencies; a positive repeat interval; and an optional inclusive end date. Recurring tasks require a valid scheduled date. Monthly calculation uses `LocalDate` month-end clamping.
- Occurrence handling: completing an incomplete recurring occurrence transactionally queues the completed task, generates the next dated task with a shared series ID, and copies active subtasks with new stable IDs and incomplete state. A unique series/date index plus repository lookup prevents duplicate occurrences.
- Today integration: task create/edit supports recurrence frequency, interval, and optional end date; task cards show a repeat summary. Future generated occurrences remain in Room for Calendar but are excluded from Today until their scheduled date.
- Migration: database version 5 adds recurrence frequency, interval, end date, and series ID through non-destructive `MIGRATION_4_5`, plus the duplicate-safe series/date index.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; `./gradlew connectedDebugAndroidTest --console=plain` — PASS with 17 tests on `SM-X115 - 16`; `./gradlew installDebug --console=plain` — PASS; existing app data opened through migration 4→5, `MainActivity` resumed, and no Android runtime crash was logged.
- Known limitation: edit/delete applies to the selected occurrence rather than silently changing the entire series. Production cloud processing remains in the later sync scope.
- Next unblocked task: Phase 3, Task 3.5 Offline interaction, pending separate user approval.

### 2026-07-14 — Phase 3, Task 3.5 offline interaction

- Model: Sol.
- Reading mode: reused established Phase 3 local-first context and read only the approved Task 3.5 build-plan, architecture, standards, tracker, and directly involved planner repository, queue, Today, and test files.
- Files changed: planner sync-state domain/use-case/repository paths, pending-change DAO observation queries, Today ViewModel/screen status, focused queue/repository/use-case tests, and this tracker.
- Behaviour: Today now observes planner queue state and calmly reports that changes are saved on this device, waiting to sync, syncing, or scheduled for retry. Task and subtask operations continue to update Room immediately, so UI behavior does not depend on network availability.
- Queue durability: planner pending, processing, and failed counts are derived from Room Flows and exclude unrelated record types. Instrumented repository checks confirm task and subtask writes create or coalesce stable durable queue entries, including completion, ordering, recurrence, and tombstones.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; `./gradlew connectedDebugAndroidTest --console=plain` — PASS with 18 tests on `SM-X115 - 16`.
- Scoped correction during verification: corrected one new test expectation to include the parent task queue entry alongside its two subtask entries; the implementation itself was unchanged.
- Known limitation: production Supabase task/subtask processing remains unavailable because no approved remote planner schema or RLS contract exists. This task does not invent cloud tables, policies, or ownership rules; local Room writes and durable retry state are complete.
- Next unblocked task: Phase 4, Task 4.1, pending separate user approval.

### 2026-07-14 — Phase 4, Task 4.1 calendar navigation

- Model: Terra.
- Reading mode: only the approved Task 4.1 build-plan section, progress tracker, code standards, and directly involved calendar/navigation/task-creation source and test paths.
- Files changed: `feature/calendar/CalendarScreen.kt`, `feature/calendar/CalendarViewModel.kt`, Calendar destination wiring in `core/navigation/KiwiNavigation.kt`, focused Calendar ViewModel tests, and this tracker.
- Behaviour: replaced the Calendar placeholder with month navigation, past/future date selection, a selected-day task list, and a concise task-creation dialog. New Calendar tasks are created through the existing local-first task use case with the selected ISO date as `scheduledDate`.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; 46 actionable tasks, exit code 0. `git diff --check` — PASS.
- Known limitation: device runtime verification was not run for this task; task creation supports title and selected date only, while richer task editing remains in Today.
- Next unblocked task: Phase 4, Task 4.2 Timeline, pending separate user approval.

### 2026-07-14 — Phase 4, Task 4.2 timeline

- Model: Luna.
- Reading mode: only the approved Task 4.2 build-plan section, progress tracker, and directly involved Calendar/task model/use-case/test paths; established code standards were reused.
- Files changed: `feature/calendar/CalendarScreen.kt`, `feature/calendar/CalendarViewModel.kt`, focused Calendar ViewModel tests, and this tracker.
- Behaviour: the selected Calendar date now shows timed tasks in chronological order, untimed tasks afterward, category color markers, completed-state styling, and break cards for gaps of at least one hour. Empty and loading states remain available.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; 46 actionable tasks, exit code 0. `git diff --check` — PASS.
- Known limitation: device runtime verification was not run for this task; break cards are derived from scheduled-time gaps and do not represent persisted break records.
- Next unblocked task: Phase 4, Task 4.3 Local reminders, pending separate user approval.

### 2026-07-14 — Phase 4, Task 4.3 local reminders

- Model: Sol.
- Reading mode: only the approved Task 4.3 build-plan, notification/background/privacy architecture and standards sections, progress tracker, and directly involved task persistence, application, manifest, Calendar/Today, and test paths.
- Files changed: local notification scheduler/planner and receiver under `core/notifications/`; task repository, application wiring, Calendar/Today repository construction, manifest permissions/receiver, focused JVM and Room repository tests, and this tracker.
- Behaviour: future timed, incomplete tasks schedule device-only `AlarmManager` reminders. Task updates reschedule, completion or an invalid/past/untimed schedule cancels, deletion cancels, and generated future recurring occurrences schedule after creation. Notifications use a dedicated channel and generic text only; task titles, descriptions, notes, and personal data are never displayed.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; 46 actionable tasks. `./gradlew connectedDebugAndroidTest --console=plain` — PASS, 19 tests on `SM-X115 - 16`. `git diff --check` — PASS.
- Known limitation: Android 13+ notification delivery requires the user to grant the declared notification permission; this task adds no runtime permission prompt. Task 4.4 owns reboot, app-update, time-zone, and restore reconciliation.
- Next unblocked task: Phase 4, Task 4.4 Reconciliation, pending separate user approval.

### 2026-07-14 — Phase 4, Task 4.4 reminder reconciliation

- Model: Sol.
- Reading mode: only the approved Task 4.4 build-plan, notification/startup/background architecture and standards sections, progress tracker, and directly involved reminder, Room task query, manifest, WorkManager, session-restoration, and test paths.
- Files changed: reminder reconciliation source/worker/receiver under `core/notifications/`, task DAO reminder-candidate query, session-restoration trigger, manifest lifecycle receiver/permission, focused JVM and Room repository tests, and this tracker.
- Behaviour: a unique durable WorkManager job rebuilds future active timed-task alarms. It runs after boot, app replacement, time-zone changes, and successful session restoration. Room excludes deleted, completed, and untimed tasks; the existing planner excludes past or invalid times and recalculates trigger times in the current time zone.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; 46 actionable tasks. `./gradlew connectedDebugAndroidTest --console=plain` — PASS, 20 tests on `SM-X115 - 16`. `git diff --check` — PASS.
- Known limitation: lifecycle broadcasts were not manually triggered on the tablet; receiver/manifest wiring compiles and the reconciliation source/runner are covered by focused tests.
- Next unblocked task: Phase 5, Task 5.1 Cycle records, pending separate user approval.

### 2026-07-14 — Phase 5, Task 5.1 cycle records

- Model: Sol.
- Reading mode: only the approved Task 5.1 build-plan section, progress tracker, code standards, relevant local-first/data/sync architecture sections, and directly involved Room, sync metadata, repository/use-case, and focused test paths.
- Files changed: cycle and daily wellness entities/DAO/domain/repository/use-case files; sync record types; Room database migration and schema; focused JVM and instrumented repository/migration tests; and this tracker.
- Behaviour: added authenticated local-first cycle intervals with start/end dates and one daily wellness record per user/date. Both record types support observation, creation, complete editing, and deletion through tombstones. Daily records may optionally reference an active cycle owned by the same user; recreating a deleted date safely revives its stable daily record.
- Validation and ownership: dates require ISO local-date format, period end cannot precede period start, and every read/write is scoped to the authenticated user. No cycle prediction, diagnosis, symptom fields, UI, or remote table contract was introduced.
- Persistence: every create, edit, revive, and delete writes Room and a stable pending-change entry in one transaction. Database version 6 adds `cycle_records` and `wellness_daily_records` through non-destructive `MIGRATION_5_6`; mutable records include the required sync metadata and deletion state.
- Verification: initial compilation found two prohibited member-extension callable references; after explicit user approval they were replaced with equivalent lambdas. `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; `./gradlew connectedDebugAndroidTest --console=plain` — PASS with 24 tests on `SM-X115 - 16`, including migration 5→6 and cycle/daily CRUD/tombstone coverage; `git diff --check` and focused secret checks — PASS.
- Known limitation: Task 5.1 intentionally provides the data/domain foundation only. Daily wellness fields belong to Task 5.2, the wellness interface belongs to Task 5.3, and production Supabase processing awaits an approved remote schema/RLS contract.
- Next unblocked task: Phase 5, Task 5.2 Daily wellness fields, pending separate user approval.

### 2026-07-14 — Phase 5, Task 5.2 daily wellness fields

- Model: Sol.
- Reading mode: only the approved Task 5.2 build-plan section, progress tracker, code standards, relevant wellness/data/sync architecture sections, and existing Task 5.1 wellness source and tests.
- Files changed: existing daily wellness entity, domain model, repository, and Room database migration; focused wellness domain/repository/migration tests; generated Room schema version 7; and this tracker.
- Behaviour: daily records now persist optional flow, pain, cramps, symptoms, mood, energy, sleep duration, notes, exercise, and self-care/medication notes. Existing create, edit, delete, revive, ownership, tombstone, and pending-sync behavior is retained for the enriched record.
- Validation: flow uses controlled None/Light/Medium/Heavy values; pain, cramps, and energy accept only 0–10; sleep accepts only 0–1440 minutes; text is trimmed and blank values become absent; symptom labels are trimmed, deduplicated, and persisted as a JSON list. This task stores descriptive user entries only and adds no diagnosis, prediction, treatment guidance, or alerts.
- Migration: database version 7 adds nullable wellness columns through non-destructive `MIGRATION_6_7`, preserving existing daily records with empty optional fields.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; `./gradlew connectedDebugAndroidTest --console=plain` — PASS with 27 tests on `SM-X115 - 16`, including migration 6→7 and enriched daily-record round trips; `git diff --check` and focused secret checks — PASS.
- Known limitation: Task 5.2 provides persistence and domain validation only. Task 5.3 owns the wellness interface; no production Supabase wellness schema/RLS contract has been approved yet.
- Next unblocked task: Phase 5, Task 5.3 Wellness interface, pending separate user approval.

### 2026-07-14 — Phase 5, Task 5.3 wellness interface

- Model: Sol (user-selected override of the preferred Luna screen implementation).
- Reading mode: only the approved Task 5.3 build-plan, tracker, architecture, shared UI specifications/tokens/rules, existing wellness/navigation/theme paths, and `context/design-references/README.md`. The visual-reference guidance was used for general hierarchy and color rhythm; production Compose components and tokens were retained rather than copying concept imagery.
- Files changed: `feature/wellness/WellnessScreen.kt`, `feature/wellness/WellnessViewModel.kt`, Wellness/Assistant destination wiring, and this tracker.
- Behaviour: added a record-first Wellness destination with factual record totals, quick daily logging, period start/end editing, full daily-field logging, month calendar indicators, history, editing, and confirmation-gated deletion. Saves remain local-first and show calm queued-sync feedback; empty, loading, and error states are represented.
- Health safety: the screen presents only recorded facts. It adds no cycle/fertility forecast, diagnosis, medical certainty, treatment recommendation, alert, or historical calculation reserved for later tasks.
- Navigation decision: with explicit user approval, Wellness replaces Ask Kiwi in the bottom dock. Ask Kiwi remains reachable through Today and its non-dock route remains registered.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS; `./gradlew connectedDebugAndroidTest --console=plain` — PASS with 27 tests on `SM-X115 - 16`; `git diff --check` and focused secret checks — PASS.
- Known limitation: device interaction was not manually exercised for the new Compose route; build/package and existing device instrumentation passed. Task 5.4 owns historical calculations and Task 5.5 owns health alerts.
- Next unblocked task: Phase 5, Task 5.4 Historical analytics, pending separate user approval.

### 2026-07-14 — Phase 5, Task 5.4 historical analytics

- Model: Sol.
- Reading mode: only the approved Task 5.4 build-plan, tracker, code standards, analytics/wellness/health-safety architecture and UI sections, and existing Task 5.1–5.3 wellness domain, interface, and focused test paths.
- Files changed: immutable wellness analytics domain models, pure `CalculateWellnessAnalytics` use case, focused analytics unit tests, existing Wellness ViewModel/screen factual presentation, and this tracker.
- Calculations: recorded cycle count uses saved cycle records; cycle lengths are positive day intervals between distinct sorted period starts; average/shortest/longest and variation derive only from those intervals. Completed bleeding duration is inclusive of start and end dates; open or invalid intervals are excluded. Flow counts, dated pain/energy series, dated symptom/mood history, and normalized occurrence counts derive only from valid dated daily records.
- Sparse-data behavior: one or zero valid period starts produces unavailable cycle-length summaries instead of estimates. Malformed dates are ignored for calculations, open periods are excluded from bleeding duration, and no missing values are imputed.
- Presentation: Wellness now shows a historical-summary card labeled “Based only on recorded entries,” including available cycle, duration, flow, pain, symptom, mood, and energy facts. It adds no forecast, fertility window, diagnosis, medical certainty, recommendation, or alert behavior.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS, including focused empty/sparse/multiple/malformed analytics cases; `./gradlew connectedDebugAndroidTest --console=plain` — PASS with 27 tests on `SM-X115 - 16`; `git diff --check` and focused secret checks — PASS.
- Known limitation: the analytics card was not manually exercised on-device. Historical values are recalculated in memory from observed local records and are intentionally not persisted. Task 5.5 owns health alerts and must not reuse these facts as medical conclusions.
- Next unblocked task: Phase 5, Task 5.5 Health alerts, pending separate user approval.

### 2026-07-14 — Phase 5, Task 5.5 health alerts

- Model: Sol.
- Reading mode: only the approved Task 5.5 build-plan, tracker, standards, health/privacy/notification/episode architecture and product boundaries, alert UI rules, existing Task 5.1–5.4 wellness paths, and directly involved Room/sync/notification/application/test files.
- User-authorized decisions: Version 1 detects only active-cycle recorded patterns: pain level 7–10 on at least two recorded days in the same active cycle; Heavy flow on at least two consecutive recorded days in the same active cycle; and an open period record reaching eight inclusive calendar days. Completed cycles do not create new cautions. These are conservative product rules, not medical thresholds or conclusions.
- Episode behavior: durable Room episodes use the unique logical key `user_id + pattern_type + source_cycle_id`. Reconciliation updates evidence without duplicate episodes or repeat notifications, retains acknowledgement, respects dismissal for that source cycle, and resolves active/acknowledged episodes when the recorded pattern or active cycle ends. Episode writes are local-first and enqueue a stable `HEALTH_ALERT_EPISODE` sync change.
- Notifications and quiet hours: new episodes schedule one generic privacy-safe device notification. No pain, flow, date, symptom, or other health detail appears in notification text. Local quiet hours are 21:00–08:00; alerts detected during that window are deferred to 08:00 in the device time zone.
- Presentation and wording: Wellness shows active/acknowledged recorded-pattern notes with evidence dates/count, acknowledgement, dismissal, and explicit wording that the note is based on records, is not a diagnosis, and may be discussed with a qualified health professional if concerning. No emergency determination, treatment advice, prediction, fertility, pregnancy, or medical certainty was added.
- Persistence: database version 8 adds `health_alert_episodes` and a non-destructive `MIGRATION_7_8`, including ownership, stable IDs, episode state, notification timestamp, sync metadata, and the deduplication index. Manifest/application wiring registers the private receiver and local scheduler.
- Verification: `./gradlew testDebugUnitTest assembleDebug --console=plain` — PASS, including detection and quiet-hour tests; `./gradlew connectedDebugAndroidTest --console=plain` — PASS with 29 tests on `SM-X115 - 16`, including migration 7→8 and Room episode deduplication/single-notification/acknowledgement/resolution; `git diff --check`, schema-8 presence, and focused secret checks — PASS.
- Known limitations: notification permission still depends on the existing Android runtime permission state; health notifications and the new caution cards were not manually triggered on-device. Production Supabase processing remains unavailable until a remote alert schema and RLS contract are separately approved.
- Next unblocked task: Phase 6, Task 6.1, pending separate user approval.

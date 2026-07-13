# Kiwi — Progress Tracker

## Status values

- `NOT_STARTED`
- `IN_PROGRESS`
- `BLOCKED`
- `COMPLETE`
- `NEEDS_REVIEW`

## Current checkpoint

- Current phase: Phase 0 — Foundation
- Current task: 0.5 Create navigation shell
- Status: NEEDS_REVIEW
- Active model: Luna for scoped navigation/UI work

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
| 1.1 | Create Room database foundation | NOT_STARTED | Sol | Database tests |
| 1.2 | Create repositories and use cases | NOT_STARTED | Sol | Unit tests |
| 1.3 | Create sync queue and worker | NOT_STARTED | Sol | Offline/retry test |
| 1.4 | Configure Supabase client | NOT_STARTED | Sol | Authenticated request test |
| 1.5 | Create restore flow | NOT_STARTED | Sol | Fresh-install restore test |

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

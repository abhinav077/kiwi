# Kiwi — Controlled Build Plan

## Execution contract

The agent implements one task at a time. A task is complete only when its acceptance criteria pass, the relevant build/tests pass, and `PROGRESS_TRACKER.md` is updated.

## Model key

The model listed under each task is the recommended primary model. The user may override it, but the agent must explain the risk before proceeding. Sol is reserved for architecture, data, authentication, security, health, synchronization, migrations, and release work. Terra is used for repository exploration or unclear approaches. Luna is used for clearly scoped UI, routine feature work, fixes, and refactors after the foundation is stable.

## Phase 0 — Repository and foundation

### 0.1 Repository audit

**Preferred model:** Terra

Inspect the generated project, package name, Gradle setup, manifest, source tree, and current build. Record conflicts. Do not redesign the project during this task.

### 0.2 Build stabilization

**Preferred model:** Sol

Lock the Gradle memory configuration, dependency strategy, Java/Kotlin compatibility, and debug build. Verify `assembleDebug` on the development laptop.

### 0.3 App architecture skeleton

**Preferred model:** Sol

Create the target package structure, `KiwiApplication`, core result/state types, dispatchers, and dependency boundaries without implementing feature logic.

### 0.4 Design foundation

**Preferred model:** Luna

Implement theme tokens, typography, shapes, spacing, background layers, shared buttons/cards/chips, and a preview catalog.

### 0.5 Navigation shell

**Preferred model:** Luna

Create the root navigation graph, placeholder destinations, bottom dock, system-bar treatment, and tablet-aware scaffold.

## Phase 1 — Local persistence and sync foundation

### 1.1 Room database

**Preferred model:** Sol

Create the database, converters, base sync metadata, and initial entities. Add migrations and database tests.

### 1.2 Repository contracts

**Preferred model:** Sol

Define repository interfaces and domain models so screens do not depend directly on Room or Supabase.

### 1.3 Sync queue

**Preferred model:** Sol

Create pending-change records, retry states, tombstones, last-write-wins metadata, and a WorkManager worker. Network failure must never discard a local change.

### 1.4 Supabase client

**Preferred model:** Sol

Add the Supabase Kotlin client, public configuration loading, authenticated session handling, and safe error mapping. Never add service-role credentials to the app.

### 1.5 Restore flow

**Preferred model:** Sol

Define first-login restore, local database rebuild, reminder recreation, and duplicate-safe upsert behaviour.

## Phase 2 — Authentication and onboarding

### 2.1 Welcome

**Preferred model:** Luna

Implement the warm opening screen and Continue action.

### 2.2 Google sign-in

**Preferred model:** Sol

Implement native Google/Supabase authentication, deep-link or ID-token callback handling, loading, cancellation, and failure states.

### 2.3 Approved-user gate

**Preferred model:** Sol

Check the allowlist locally and through RLS. Add Access denied and retry/sign-out actions.

### 2.4 Minimal setup

**Preferred model:** Luna

Ask only what Kiwi should call the user. Persist the profile locally first and sync it later.

### 2.5 Session restoration

**Preferred model:** Sol

Open directly into Today after a valid session. Handle expired sessions and offline reopening safely.

## Phase 3 — Today planner

### 3.1 Today shell

**Preferred model:** Luna

Build the approved visual composition: greeting, hero next task, progress, time-of-day groups, self-care moment, quick actions, and Ask Kiwi entry.

### 3.2 Task model and CRUD

**Preferred model:** Sol

Implement timed/untimed tasks, title, description, category, priority, notes, completion, edit, and delete.

### 3.3 Subtasks

**Preferred model:** Luna

Add subtasks, completion, ordering, and progress calculation.

### 3.4 Recurrence

**Preferred model:** Sol

Implement recurring tasks with explicit repeat rules and generated occurrence handling.

### 3.5 Offline interaction

**Preferred model:** Sol

Every planner operation must update Room and the screen immediately, then enter the sync queue.

## Phase 4 — Calendar and reminders

### 4.1 Calendar navigation

**Preferred model:** Luna

Implement month/date selection, previous dates, future dates, and date-specific task creation.

### 4.2 Timeline

**Preferred model:** Luna

Show tasks by time with category colors, completion states, breaks, and empty states.

### 4.3 Local reminders

**Preferred model:** Sol

Schedule, cancel, and reschedule AlarmManager reminders. Add notification channels and privacy-safe content.

### 4.4 Reconciliation

**Preferred model:** Sol

Rebuild future alarms after reboot, app update, time-zone change, and restore.

## Phase 5 — Wellness tracker

### 5.1 Cycle records

**Preferred model:** Sol

Implement period start/end and daily records with complete editing/deletion.

### 5.2 Daily wellness fields

**Preferred model:** Sol definitions, Luna implementation

Add flow, pain, cramps, symptoms, mood, energy, sleep, notes, exercise, and self-care/medication notes.

### 5.3 Wellness interface

**Preferred model:** Luna screens, Sol analytics and health review

Build Wellness overview, quick log, log form, calendar, history, and factual analytics.

### 5.4 Historical analytics

**Preferred model:** Sol

Calculate recorded cycle count, historical average, shortest/longest cycle, bleeding duration, flow distribution, pain/symptom/mood/energy history, and variation.

### 5.5 Health alerts

**Preferred model:** Sol

Implement local pattern detection, alert episodes, deduplication, acknowledgement, dismissal, quiet hours, and non-diagnostic wording.

## Phase 6 — Diary and personal space

### 6.1 Diary entries

**Preferred model:** Luna

Implement title, writing content, date, best thing today, mood, favourite, timestamps, edit/delete, and search.

### 6.2 Diary photos

**Preferred model:** Sol

Compress and save locally first, preserve local previews, upload to private Storage when online, and delete remote files safely.

### 6.3 Diary experience

**Preferred model:** Luna

Build the intimate writing surface, calendar, favourites, filters, empty states, and lock/privacy entry point.

## Phase 7 — Self-care

**Preferred model:** Luna

Implement routines with name, description, category, time, repeat days, optional checklist, active/paused state, completion history, local reminders, and edit/delete.

## Phase 8 — Deterministic Kiwi Assistant

### 8.1 Parser foundation

**Preferred model:** Sol

Define supported intents, tokenization, date/time parsing, confidence/ambiguity handling, and unsupported-command responses.

### 8.2 Planner commands

**Preferred model:** Sol

Add, update, delete, complete, reschedule, postpone, recurring task, date query, unfinished query, and copy commands.

### 8.3 Wellness commands

**Preferred model:** Sol

Start/end record, flow, symptoms, pain, mood, energy, notes, edit/delete, and analytics commands.

### 8.4 Diary/self-care commands

**Preferred model:** Luna

Create/open/edit diary, best thing today, routine creation/update/completion/pause, and recurring reminder commands.

### 8.5 Safety and confirmation

**Preferred model:** Sol

Confirm destructive, bulk, and ambiguous sensitive actions. Ensure commands call the same domain use cases as normal UI.

## Phase 9 — Analytics and review

**Preferred model:** Sol calculation layer, Luna presentation

Implement planner completion percentages, streaks, planned-versus-completed, categories, missed/postponed counts, heatmap, weekly reflections, wellness analytics, and review history. Do not generate behavioural judgments.

## Phase 10 — Themes, privacy, and account

**Preferred model:** Luna theme/UI, Sol privacy/account

Implement theme variants, font choices, notification privacy, PIN/biometric lock, recent-screen privacy, sync status, export, account deletion, and logout.

## Phase 11 — Pinterest downloader

**Preferred model:** Terra for extraction investigation; Sol review required for access-control and privacy decisions.

Implement paste URL, Android share receiver, validation, isolated extraction function, preview, DownloadManager, local gallery saving, attribution, and clear failures. This module must not affect other app features.

## Phase 12 — Quality and release

**Preferred model:** Sol

Complete offline tests, migration tests, sync conflict tests, authentication/RLS checks, notification tests, tablet adaptation, accessibility review, crash handling, release signing, APK generation, and restore-after-reinstall testing.

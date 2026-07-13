# Kiwi — Technical Architecture

## Architectural style

Kiwi uses a local-first, single-user Android architecture:

`Compose UI → ViewModel/State Holder → Use Case → Repository → Room`

`Repository → Sync Queue → WorkManager → Supabase`

Room is the operational source for the app. Supabase is the synchronized cloud copy used for backup, restoration, and device replacement.

## Technology choices

- Kotlin
- Jetpack Compose
- Navigation Compose
- Room
- WorkManager
- AlarmManager for user-created time-sensitive reminders
- DataStore for small local preferences
- BiometricPrompt and secure PIN storage
- Supabase Kotlin client
- Supabase Auth with Google
- Supabase PostgreSQL and Row Level Security
- Supabase private Storage for diary photos
- Android DownloadManager for Pinterest files
- Kotlin serialization
- Coil for local/remote images when needed

## Layer responsibilities

### Presentation

Compose screens, reusable components, UI state, accessibility semantics, transitions, and user interaction. Presentation must not perform direct database or network calls.

### ViewModel/state

Own screen state, invoke use cases, expose loading/error/sync states, preserve drafts, and survive configuration changes.

### Domain

Pure business rules and use cases. Examples: calculate completion percentage, parse an assistant command, determine whether an alert episode already exists, and calculate historical cycle statistics.

### Data

Repositories coordinate Room, sync queue, Supabase, photo upload queue, and local reminder scheduling. The UI never decides whether to use Room or Supabase.

### Local persistence

Room stores user-created records, sync metadata, alert episodes, drafts, and enough data to restore reminders. Every mutable record needs a stable local ID, timestamps, sync state, and deletion metadata.

### Synchronization

Each local change is written first and marked pending. WorkManager retries pending changes when connectivity is available. A practical Version 1 conflict rule is last-write-wins using `updated_at`, because only one person uses the app.

Deletion uses tombstones until the remote deletion is acknowledged.

## Data ownership

| Data | Local Room | Supabase | Notes |
|---|---:|---:|---|
| Tasks and subtasks | Yes | Yes | Local-first, synchronized |
| Period and symptom records | Yes | Yes | No prediction fields |
| Diary text and mood | Yes | Yes | Save immediately offline |
| Diary photos | Local URI/cache | Private Storage | Upload later, delete after acknowledgement |
| Self-care routines | Yes | Yes | Local reminders |
| Health alerts | Yes | Yes | Deduplicated episodes |
| Settings | DataStore/Room | Yes where needed | Local UI availability first |
| Auth session | Secure local session storage | Auth service | Never hard-code credentials |
| Pinterest videos | Device storage | No | Never use Supabase as video storage |

## Sync metadata required on mutable records

- `local_id`
- `remote_id` when available
- `user_id`
- `created_at`
- `updated_at`
- `deleted_at` nullable
- `sync_status`
- `last_sync_error` nullable
- `device_id`

## Authentication and access

1. User selects Continue with Google.
2. Supabase Auth creates/restores the session.
3. The app checks the authenticated email against `allowed_users`.
4. Unapproved users see Access denied and cannot read or write user data.
5. RLS repeats the restriction server-side.
6. The local session may reopen the app offline after a successful first login, but cloud restoration still requires connectivity.

## Notifications

Notifications are scheduled on the device after a task/routine is saved. Supabase does not contact the phone at the exact reminder time.

Rebuild reminders after:

- Device restart
- App update
- Time-zone change
- Session restoration
- Task/routine time change

## Health-pattern alert model

Use alert episodes instead of daily independent alerts.

Logical key:

`user_id + pattern_type + source_cycle_id`

An active episode is updated when more evidence is logged. It does not notify repeatedly. It resolves when the source situation ends. A future separate cycle may create a new episode.

## Repository structure target

```text
app/src/main/java/com/abhinavsirohi/kiwi/
├── KiwiApplication.kt
├── MainActivity.kt
├── core/
│   ├── design/
│   ├── navigation/
│   ├── common/
│   ├── database/
│   ├── sync/
│   ├── notifications/
│   └── security/
├── data/
│   ├── local/
│   ├── remote/
│   └── repository/
├── domain/
│   ├── model/
│   └── usecase/
└── feature/
    ├── onboarding/
    ├── today/
    ├── calendar/
    ├── wellness/
    ├── diary/
    ├── selfcare/
    ├── assistant/
    ├── analytics/
    ├── downloads/
    └── settings/
```
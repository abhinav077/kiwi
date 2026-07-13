# Kiwi — Code Standards

## General

- Kotlin only for application code.
- Prefer immutable data and unidirectional data flow.
- Keep functions small and named by behaviour.
- Use explicit domain names; avoid vague names such as `Data`, `Manager`, or `Helper` when a specific name is possible.
- Do not hide network/database work inside Composables.
- Do not use global mutable state.
- Do not duplicate business rules between screens.

## Compose

- Composables render state and emit events.
- Screen-level Composables may collect ViewModel state; reusable components should receive plain state and callbacks.
- Use previews for shared components and representative screen states.
- Provide loading, empty, error, offline, and success states.
- Preserve state through rotation and process recreation where appropriate.
- Use semantic labels for icons and interactive controls.
- Keep touch targets accessible and consistent.

## State and errors

Use explicit state models instead of scattered booleans:

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Ready<T>(val value: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

Errors shown to users must be calm, actionable, and non-technical. Log technical details locally without exposing secrets.

## Coroutines

- Use structured concurrency.
- Inject dispatchers where testing requires them.
- Never use `GlobalScope`.
- Never block the main thread.
- Use `viewModelScope` for ViewModel work and WorkManager for durable background work.

## Room

- Entities represent persistence, not UI.
- Use migrations for schema changes.
- Never use destructive migration in production.
- Every user-created mutable entity needs sync metadata.
- Use tombstones for remote deletion synchronization.
- Test important queries and migrations.

## Supabase and secrets

- Client code may contain only the public/publishable key.
- Never include the service-role key in the app.
- Never commit OAuth client secrets.
- Keep local values in an ignored properties/env file.
- Validate all RLS policies with approved and unapproved test identities.

## Naming

- Types: `PascalCase`.
- Functions/variables: `camelCase`.
- Constants: `UPPER_SNAKE_CASE` only for true constants.
- Screens end with `Screen`.
- ViewModels end with `ViewModel`.
- Repositories end with `Repository`.
- Use feature-prefixed names for conflicting concepts, e.g. `WellnessMood` versus `DiaryMood` only when the domain meanings differ.

## Tests

Every feature task should add tests appropriate to its risk:

- Pure business rules: unit tests.
- Room queries/migrations: database tests.
- ViewModel state transitions: unit tests.
- Critical Compose flows: UI tests.
- Auth/RLS/sync: integration or manual verification checklist.

## Git

- One focused commit per completed task where practical.
- Commit format: `type(scope): description`.
- Examples: `feat(today): add task creation`, `fix(sync): retry tombstones`, `chore(build): add room dependencies`.
- Do not commit generated APKs, local databases, secrets, or IDE machine-specific files.
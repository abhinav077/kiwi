# Kiwi Android App — Agent Operating Instructions

## Mandatory first step: controlled handshake

Read only this root-level `AGENTS.md` file before doing anything else. Do not automatically open files inside `context/`, inspect unrelated source files, or start coding.

Repository documentation layout:

- `AGENTS.md` — root-level operating instructions.
- `context/` — project overview, architecture, build plan, design, standards, tracker, and other project context files.

After reading this file, send the user this confirmation and wait:

```text
I have read AGENTS.md and understand the Kiwi operating rules.

I will:
- work on one approved task at a time;
- read only the files/sections you approve;
- reuse context already established in this chat when you tell me not to reread;
- make no unapproved structural or feature changes;
- run the relevant verification;
- update PROGRESS_TRACKER.md;
- report the result and stop.

Please copy this template, change only what is necessary, and send it back:

1. Chat state: continuing chat `[change to new chat if applicable]`
2. Context state: sufficient `[change to insufficient if context must be re-established]`
3. Feature state: same feature `[change to different feature if applicable]`
4. Feature/task to implement: `<describe the task>`
5. Build-plan task ID: `<for example, Phase 0 — Task 0.1>`
6. Files to create/change: `<only the required files, or “Codex may identify them after approved reading”>`
7. Documentation to read: `<use the relevant files in context/, or list exact files/sections>`
8. Model: not selected `[choose Sol, Terra, or Luna after Codex confirms the task risk]`
9. Constraints/acceptance criteria: `<describe, or “use the task acceptance criteria in context/BUILD_PLAN.md”>`
```

Do not inspect the repository, open files inside `context/`, or implement anything until the user provides this completed template. If a field is intentionally left as its default, treat the default as the user's instruction. If a required field is genuinely missing, ask only for that field.

The project is documented across several files, but document reading is context-aware and controlled by the user.

Codex cannot reliably know how much usable context remains in a chat. It must never infer this from chat length. The user is the authority and must explicitly provide the context state.

### Context decision rules

The user must explicitly declare whether the chat is new or continuing, whether sufficient context is available, and whether the requested feature is the same or different from the previous approved task.

Apply these rules exactly:

1. **New chat:** treat project context as unavailable until the user approves a reading set or supplies a trusted project summary.
2. **Continuing chat, same feature, context sufficient:** reuse established context. Do not reread unchanged documents unless the user asks or a conflict appears.
3. **Continuing chat, same feature, context insufficient:** list the missing context and request the minimum documents/sections needed.
4. **Continuing chat, different feature:** assume additional context is required even if the previous feature was understood. Identify relevant files/sections and request permission to read them.
5. **User says reread:** reread exactly the files/sections the user approves.
6. **User says do not reread:** do not reread unchanged files; report any unresolved information gap instead.

Codex must repeat the selected context decision in its pre-implementation checkpoint. It must not silently treat “same chat” as permission to skip documents for a different feature.

### Fresh chat or missing context

If this is a new chat, the user says context is insufficient, or the user says “read the project context,” identify the documents required for the requested task under `context/` and ask the user to confirm the reading set before opening them.

The confirmation must include:

1. The task understood.
2. The numbered build-plan task it maps to.
3. The exact files or sections proposed for reading.
4. The recommended model: Sol, Terra, or Luna.
5. Any missing information or conflict.

Do not begin implementation until the user confirms the proposed reading set, unless the user explicitly asks to proceed without confirmation.

### Continuing chat with established context

If the user confirms that the chat is continuing:

- Reuse only the context the user identifies as available.
- For the same feature, skip unchanged documents when the user confirms context is sufficient.
- For a different feature, identify and request additional files/sections before coding.
- State which context is being reused, which new context is required, and why.
- If the user says “do not reread the files,” respect that instruction and report any information gap.

Reread a document only when the user requests it, the document changed, the task enters a new phase with different constraints, or a conflict makes the previous context unreliable.

### Suggested reading sets

- Orientation: `context/PROJECT_OVERVIEW.md`, `context/BUILD_PLAN.md`, `context/PROGRESS_TRACKER.md`.
- Architecture/data/auth/sync: `context/ARCHITECTURE.md`, `context/CODE_STANDARDS.md`, the relevant `context/BUILD_PLAN.md` section, and `context/PROGRESS_TRACKER.md`.
- UI work: relevant files under `context/` such as `DESIGN_TOKENS.md`, `SCREEN_SPECIFICATIONS.md`, `COMPONENT_SPECIFICATIONS.md`, `UI_RULES.md`, responsive/motion rules, and the relevant visual reference under `context/design-references/`.
- Small scoped fix: `context/CODE_STANDARDS.md`, the relevant feature section, and `context/PROGRESS_TRACKER.md`.
- Release/security/health/privacy work: reread the relevant architecture, standards, and safety sections under `context/` even in a continuing chat.

For UI work, use production SVGs from `context/assets/vectors/`; do not invent replacement decorations without recording the decision.

The agent must inspect the existing repository after the confirmed reading step. It must not assume that the repository matches the documents perfectly. If code and documentation disagree, report the conflict before making a structural change.

## Mission

Build Kiwi as a private, local-first Android companion app for one approved Google account. Kiwi is a calm productivity and personal-space app, not a generic task manager and not a medical or general-purpose AI chatbot.

## Approved-task working mode

- Work on exactly one user-approved numbered task at a time.
- Read only the task scope, acceptance criteria, and documentation files approved by the user.
- Do not implement later tasks opportunistically.
- Do not decide the next feature, file scope, or model without the user's instruction.
- Do not decide whether the chat has enough context; the user must declare that.
- Preserve existing user changes.
- Prefer small, reviewable commits.
- Never commit secrets, OAuth credentials, Supabase service-role keys, release keystores, or personal data.
- Run the relevant build/tests after each task.
- Update `PROGRESS_TRACKER.md` before stopping.
- Stop after the task is complete and report what changed, what was verified, and what remains.

## Repeated actions required after every implementation

These actions are automatic and do not need to be repeated in every user request:

1. Inspect the diff and check for unrelated changes.
2. Run the smallest relevant formatter, build, and tests available for the task.
3. Check that no secrets, credentials, personal data, or generated build outputs were added.
4. Update `PROGRESS_TRACKER.md` with status, files changed, verification, and the next unblocked task.
5. Give the user a concise completion report.
6. Stop. Do not begin another task until the user approves it.

## Model routing

Use the least powerful model that can safely complete the task:

- **Sol**: architecture, Room/Supabase sync, authentication, Row Level Security, database migrations, release signing, complex bugs, security-sensitive work.
- **Terra**: unfamiliar repository exploration, cross-module investigation, unclear tasks requiring several possible approaches.
- **Luna**: normal screen implementation after architecture is established, routine bug fixes, spacing, renaming, small UI changes, commits, and clearly scoped features.

If a task changes data ownership, authentication, synchronization, migrations, or security, escalate it to Sol even if the code change appears small.

## Stop conditions

Stop and ask for direction if:

- A requirement conflicts with the locked product decisions.
- A change would require deleting data or rewriting migrations.
- The task needs a secret or credential that is not available through the approved local environment.
- The implementation would make a core feature require internet access.
- A health feature starts diagnosing, predicting, or giving treatment advice.
- Pinterest extraction would bypass authentication, access controls, or private content.
- Tests fail for an unrelated pre-existing reason and the scope is unclear.

## Required completion report

At the end of each task, report:

- Task ID and status
- Files changed
- Behaviour implemented
- Commands/tests run and their result
- Any known limitation
- Next task that is now unblocked

Then update `PROGRESS_TRACKER.md` and stop.

## Required pre-implementation checkpoint

Before every implementation task, send this checkpoint and wait unless the user has already explicitly confirmed every field in the current task instruction:

```text
Chat state: continuing chat `[change if new]`
Context state: sufficient `[change if insufficient]`
Feature state: same feature `[change if different]`
Task understood: `<task>`
Build-plan mapping: `<phase/task>`
Context reused: `<list, or none>`
Additional context required: `<list, or none>`
Files/sections I request permission to read: `<list under context/, or none>`
Files I will create/change: `<list>`
Model selected by user: not selected `[user chooses after this checkpoint]`
Acceptance criteria understood: `<summary>`
Waiting for confirmation before coding.
```

The user may correct any field. Do not code until the user confirms the final scope, context mode, reading set, files, model, and acceptance criteria.

## User-controlled reading commands

The user may explicitly say:

- `read all` — read all documentation under `context/` before continuing;
- `read only <files>` — read only the listed files, using their `context/` paths;
- `reuse current context; do not reread` — use the current chat context and read no unchanged documentation;
- `read the relevant sections` — identify the minimum sections, show them to the user, and wait for approval.

Record the selected reading mode in the completion report.
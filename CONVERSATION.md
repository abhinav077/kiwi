# Kiwi — Codex Conversation Playbook

Use these messages in the Codex VS Code chat. Replace angle-bracket values and leave the defaults when they are correct.

<!-- install -->
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

<!-- start the app -->
adb shell am start -n com.abhinavsirohi.kiwi/.MainActivity

<!-- stop the app -->
adb shell am force-stop com.abhinavsirohi.kiwi

## 1. Start a new chat

Send:

```text
Read only the root-level AGENTS.md file.
Do not inspect the repository, open context/, or modify files yet.
After reading AGENTS.md, confirm the controlled workflow and wait for my task instructions.
```

## 2. Give the first task

```text
Chat state: new chat
Context state: insufficient
Feature state: different feature
Feature/task to implement: <task>
Build-plan mapping: <phase and task ID>
Context reused: none
Additional context required: <what must be understood>
Files/sections I request permission to read:
- required for this task
Files I will create/change: <files, or “only the files required by the approved task”>
Model selected by user: <Sol/Terra/Luna>
Acceptance criteria: if everything is okay, if not then state the error that is coming after you completed the task and ask for for my approval before doing more things.
Please confirm your understanding, the proposed reading set, the files you will touch, and the model risk before reading those files.
```

## 3. Approve the reading set

```text
Confirmed. Read only the listed files and sections. Do not inspect unrelated files or implement anything until you show the task plan and wait for my implementation approval.
```

## 4. Approve implementation

```text
Implementation scope approved. Proceed with only this task and only the approved files. Run the relevant verification, update context/PROGRESS_TRACKER.md, report the result, and stop.
```

## 5. Same feature in the same chat

```text
Chat state: continuing chat
Context state: sufficient
Feature state: same feature
Reuse the current chat context. Do not reread unchanged documentation.
Task: <next task within the same feature>
Build-plan mapping: <phase and task ID>
Files to create/change: <files>
Model selected by user: <Sol/Terra/Luna>
Acceptance criteria: <criteria>
Confirm the scope before coding.
```

## 6. Different feature in the same chat

```text
Chat state: continuing chat
Context state: sufficient for the previous feature only
Feature state: different feature
Do not assume the previous feature context covers this task.
Task: <new task>
Build-plan mapping: <phase and task ID>
Identify the minimum additional context files/sections required and wait for my approval before reading them.
Do not code yet.
```

## 7. Context is no longer sufficient

```text
Chat state: continuing chat
Context state: insufficient
Feature state: <same/different>
The current chat may not contain enough reliable context. Identify the minimum project files/sections needed to re-establish context. Do not reread everything automatically and do not code yet.
```

## 8. Ask for an audit only

```text
Perform an inspection-only audit for <task/build-plan ID>.
Do not modify files.
Read only the approved files, inspect the relevant repository files, report conflicts and risks, and stop with a proposed implementation plan.
```

## 9. Allow a specific command

```text
You may run this command because it is required for the approved task:
<command>
Do not run unrelated commands or make unrelated changes.
```

## 10. If Codex proposes extra scope

```text
Do not implement that extra change. Keep the current task limited to <approved scope>. Record the extra item as a future task in your report only; do not add it to the implementation.
```

## 11. If a build/test fails

```text
Stop implementation and diagnose only the failure related to the approved task.
Report the failing command, relevant error, likely cause, and the smallest proposed fix. Do not apply the fix until I approve it.
```

## 12. If the failure is unrelated

```text
Treat this as a pre-existing or unrelated failure. Do not change unrelated code. Report the evidence and continue only if the approved task can be verified safely without changing it.
```

## 13. Approve a scoped fix

```text
The proposed fix is approved. Apply only that fix, rerun the failed verification, update context/PROGRESS_TRACKER.md, report the result, and stop.
```

## 14. Ask Codex to stop for review

```text
Stop now without making further changes. Summarize the current diff, verification status, open risks, and the next decision I need to make.
```

## 15. Task completion response expected from Codex

Codex must finish with:

```text
Task ID/status:
Files changed:
Behaviour implemented:
Verification commands/results:
Tracker update:
Known limitations:
Next unblocked task:
Stopped and waiting for approval.
```

## 16. Start the next task after completion

```text
The previous task is accepted. Begin the next approved task only:
Task: <task>
Build-plan mapping: <phase and task ID>
Chat state: continuing chat
Context state: <sufficient/insufficient>
Feature state: <same/different>
Reading instruction: <reuse current context / read only listed files>
Files to create/change: <files>
Model selected by user: <Sol/Terra/Luna>
Acceptance criteria: <criteria>
Confirm before coding.
```

## 17. Move to a new chat

Start the new chat with:

```text
Read only AGENTS.md first. This is a new chat, so do not assume previous context.
After confirming the workflow, I will provide the relevant context files, current task, model, and acceptance criteria.
```

Then provide the current task plus the relevant files under `context/`. Always include the latest `context/PROGRESS_TRACKER.md`.
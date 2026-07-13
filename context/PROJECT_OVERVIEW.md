# Kiwi — Project Overview

## Product identity

Kiwi is a private Android companion app made for one approved person. It combines practical planning, personal wellness logging, a private diary, self-care routines, and a deterministic in-app assistant.

Kiwi should feel warm, personal, joyful, calm, organized, and mature. It should make everyday use feel pleasant without becoming childish, noisy, or overly decorative.

## Core product promise

Give the user one private place to plan the day, record personal information, care for herself, and quickly act through a gentle assistant—while keeping the main functionality usable without internet.

## Product boundaries

Kiwi is:

- Private and single-user in Version 1.
- Local-first and offline-capable.
- A rule-based command assistant, not an external AI chatbot.
- A factual wellness tracker, not a medical diagnosis or prediction tool.
- Distributed directly as a signed APK, not initially published on the Play Store.

Kiwi is not:

- A social platform.
- A multi-user productivity SaaS.
- A general ChatGPT replacement.
- A fertility, pregnancy, contraception, or medical-advice application.
- A cloud-dependent task manager.

## Locked Version 1 modules

1. Welcome and minimal onboarding
2. Google authentication with one approved account
3. Today planner
4. Calendar and historical/future planning
5. Local reminders and notifications
6. Period and wellness tracker without predictions
7. Historical wellness analytics
8. Deduplicated health-pattern caution alerts
9. Personal diary with optional compressed photos
10. Best thing today and mood logging
11. Self-care routines and reminders
12. Deterministic Kiwi Assistant
13. Planner and wellness analytics
14. Themes and privacy settings
15. PIN or biometric app lock
16. Private Supabase backup and synchronization
17. Pinterest video downloader as an isolated, fragile module

## Main user flow

First launch:

`Welcome → Continue with Google → Approved-account check → Preferred name → Today`

Daily flow:

`Open Kiwi → unlock if enabled → Today → plan, complete, log, reflect, or Ask Kiwi`

## Core product rule

Every feature must support at least one of these stages:

`Record → Plan → Execute → Review → Protect`

If a proposed feature supports none of these stages, it is out of scope for Version 1.

## Current implementation checkpoint

- Android Studio installed and working.
- Physical Android tablet connected and able to run the app.
- Kotlin/Compose project created.
- GitHub repository configured.
- Supabase project created.
- Google OAuth configured, with the final approved account to be added later.
- Private `diary-photos` storage bucket created with access policies.
- Debug SHA-1 recorded for development OAuth.
- Gradle memory reduced for the 8 GB development laptop.

## Product decisions that must not be silently changed

- Room powers all core screens and operations locally.
- Supabase is the authenticated cloud backup and synchronization layer.
- Saving locally happens before synchronization.
- Core features continue to work without Wi-Fi after first login.
- Only one approved Google account can access meaningful data.
- No cycle, ovulation, fertility, pregnancy, or hormonal-phase predictions.
- Health alerts describe recorded patterns and recommend professional discussion when appropriate; they never diagnose.
- The assistant executes only supported deterministic commands.
- Delete, bulk changes, and ambiguous sensitive health changes require confirmation.
- Pinterest processing must not bypass access controls and must not be allowed to destabilize other modules.
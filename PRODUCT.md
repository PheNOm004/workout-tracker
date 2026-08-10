# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Single primary user (the app's own developer/owner): a lifter who trains both strength and calisthenics, uses the app at the gym mid-session to log sets and check suggested targets, and reviews progress/trends afterward. Personal, single-account app — not multi-tenant.

## Product Purpose

TimeGo is a fully-local Android app for logging gym workouts (strength, calisthenics holds, cardio/warmup) and tracking progress over time, with on-device deterministic rule-based recommendations for what to do next session (progressive overload targets, plateau/deload detection, calisthenics tier progression, muscle-group balance nudges). No account, no cloud sync, no network dependency for core function.

## Positioning

Combines routine-based and freeform logging with per-exercise, muscle-weighted recommendation logic that runs entirely on-device — deterministic and inspectable rather than an opaque ML suggestion, explicitly designed as a rules-based base layer a future personal ML model could sit on top of once enough longitudinal data exists.

## Operating Context

Used primarily standing in a gym between sets — logging must be fast, thumb-reachable, and legible in a bright/loud environment, then reviewed at rest (progress screen, charts, PRs). Session-based: a workout is started, exercises/sets logged incrementally, session ends.

## Capabilities and Constraints

- Stack: Kotlin, Jetpack Compose, Room (schema v6), Gradle/AGP toolchain shared with the sibling HeatP project. Min SDK 26.
- Screens: Log (session logging, routine-or-freeform), Progress (strength curves, PRs, body metrics, muscle heatmap/radar), Routines (routine builder/scheduling).
- Exercise library: 585 seeded exercises (strength/calisthenics/cardio/warmup) plus user-added custom exercises, each with muscle-group tags and per-muscle weighting (0-100).
- Logging types per exercise: WEIGHT_REPS, HOLD (duration), DURATION_DISTANCE (cardio) — drives which input fields render.
- Recommendation modules (`RuleBasedOverloadSuggester`, `RuleBasedHoldSuggester`) are deterministic, unit-tested, and swappable behind a shared interface for a future ML replacement.
- No backend/network required for core functionality; single local SQLite (Room) store.
- Verification discipline: user builds/installs and verifies on their own Galaxy S23 Ultra device — agent does not proactively screenshot.

## Brand Commitments

App name: TimeGo (`com.lsing.timego`). No committed visual identity going into this redesign — the existing Onyx dark theme, Manrope/Fraunces font pairing, and current motion system (tween-based AnimatedExpand/AnimatedContent/crossfade) are all explicitly in scope to be replaced, not preserved as constraints. This redesign supersedes the Aug 10 "visual-identity pass," which the user still considers crude/generic app-wide (chrome, spacing rhythm, typography, buttons — systemic, not one screen) despite icons/elevation/SectionHeader already being applied.

## Evidence on Hand

No real user-facing marketing copy, testimonials, or external assets — this is a personal-use app, not a distributed product. Existing anatomical muscle-diagram art (`MuscleBodyArt.kt`, traced SVG paths) and radar/heatmap visualizations are real, functioning data visualizations to preserve functionally through any redesign, even if restyled.

## Product Principles

- Speed and thumb-reach in the gym-logging flow outrank visual flourish on the Log screen specifically.
- Recommendations and progress data must stay legible and trustworthy — restyling must not obscure what a number or trend actually means.
- Local-only, deterministic, inspectable: no dark patterns, no engagement-bait animation, nothing that pretends to be smarter than the rule-based logic actually is.
- Motion should feel intentional and spring-like with spatial continuity between states, not generic fades — this was named directly as the current system's failure.

## Accessibility & Inclusion

No project-specific accessibility requirement established beyond standard Android/Compose conventions (contrast, touch target size, TalkBack where reasonable for a personal-use app).

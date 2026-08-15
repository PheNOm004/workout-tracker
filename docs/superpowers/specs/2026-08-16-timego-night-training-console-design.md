# TimeGo — Night Training Console Redesign

**Date**: 2026-08-16  
**Status**: draft, pending user review  
**Scope**: app-wide visual and motion redesign; no domain or persistence changes.

## Context

TimeGo's current visual system is the previously approved Training Ledger direction: warm graphite/bone surfaces, one brick-red accent, near-rectangular cards, and ruled-page structure. That system is coherent but too restrained in practice: screens read as one flat color field, hierarchy is weak, and motion feels abrupt because different transitions use unrelated fixed timings and effects.

The user approved a hybrid direction based on two visual references: fitness-specific energy and data hierarchy, combined with calm spacing and premium restraint. The app remains dark-first.

## Direction: Night Training Console

TimeGo should feel like a focused training console opened in a dark gym: quiet, high-contrast surfaces with controlled bursts of training color. Its single promise is: **know what to do next, log it quickly, see whether you are improving**.

The visual signature is the **training pulse**: a short coral edge indicator or trace that appears only on active exercises, current workout progress, selected states, and meaningful performance moments. It is not a permanent stripe on every card.

## Color system

The palette uses three dark surface levels and semantic fitness colors. Coral is the brand/action color; mint, amber, and violet are functional data colors, not decoration.

| Token role | Target value | Use |
|---|---|---|
| Background | `#101315` | App ground |
| Surface | `#181D20` | Primary sections and cards |
| Surface raised | `#22292C` | Active cards, dialogs, expanded rows |
| Primary text | `#F4F1EA` | Warm high-emphasis text |
| Secondary text | `#AAB2B3` | Supporting labels and metadata |
| Brand coral | `#FF6B5E` | Start, log, active, selected, PR moments |
| Coral shade | `#B94742` | Pressed states and dark containers |
| Progress mint | `#9BD8B2` | Improving, completed, positive trend |
| Caution amber | `#F2B866` | Plateau, attention, incomplete target |
| Data violet | `#B8A7FF` | Secondary chart series and comparison data |

The light theme remains supported as a deliberate warm companion, but dark mode is the primary composition and receives the first visual pass.

## Typography

- Manrope remains the UI and body face for navigation, labels, exercise names, buttons, and instructions.
- JetBrains Mono remains the data face for weight, reps, timers, PR values, dates, and chart figures.
- Section titles and data values gain stronger scale contrast so screens do not read as equal-weight lists.
- No new display font is required for this redesign; the distinction comes from scale, weight, spacing, and semantic color.

## Shape and layout

- Use 12–16dp corner radii: softer than the ledger direction but more controlled than a bubbly fitness template.
- Use surface contrast and spacing as the primary hierarchy; reduce unnecessary borders and permanent accent bars.
- Cards group related content; they do not wrap every individual row.
- Keep bottom navigation quiet so it does not compete with the active workout.
- Preserve the existing Log / Progress / Routines navigation structure.

## Screen hierarchy

### Log — train now

Log is active-workout-first because recording a set is TimeGo's highest-frequency, highest-pressure interaction.

- Compact header with session status and elapsed time.
- Coral-highlighted next-action area for the recommended exercise or active set.
- Expandable exercise modules containing the exercise name/category, suggested target, logging controls, and recent performance.
- The active exercise receives the training pulse.
- Logged sets settle into a quieter completed state; the next target becomes the focus.
- Session finish remains persistent but secondary until the workout has meaningful content.

### Progress — understand the trend

- Strong summary header with one headline metric.
- PRs and trend status presented as compact metric tiles with prominent mono values.
- Charts are visual anchors, not small decorations inside many cards.
- Timeframe and exercise selectors use a dark segmented control with coral selection.
- Existing chart and muscle-distribution data models remain unchanged; only their presentation and color semantics change.

### Routines — prepare the next session

- Recommended or most recently used routine appears first.
- Routine cards show exercise count, estimated duration, and last completed date.
- Routine actions have a clear primary/secondary hierarchy.
- Empty states direct the user toward creating or starting a routine.

## Motion system

Motion should be responsive and continuous, with one shared character across screens. Related elements move as a group along a clear axis.

- **Navigation**: fade-through plus a small horizontal shift; avoid full-width slides.
- **Expand/collapse**: animate content height and opacity together; the training pulse grows into place with the row.
- **Set logged**: brief lift-and-settle of the row, value transition into place, then focus moves to the next target.
- **Progress filters**: grouped chart content crossfades and shifts as one surface.
- **Dialogs/sheets**: fade and scale from their point of origin, without bounce.
- **Press states**: quick surface compression rather than a separate pop animation.
- **Timing**: use tuned springs for interruptible interaction; use short, consistent fade-through timings for navigation and content replacement.
- **Accessibility**: respect the system reduced-motion setting and avoid decorative looping animation.

## Implementation boundaries

This redesign is presentation-only.

In scope:

- `ui/theme` color, shape, typography, and motion tokens.
- Shared surface/card, navigation, selector, and training-pulse components.
- Log, Progress, and Routines screen composition and styling.
- Existing chart, diagram, and heatmap colors where they communicate state.
- Shared transition and animation helpers.

Out of scope:

- Domain rules, workout recommendations, Room schema, persistence, or data migration.
- Navigation destinations or screen count.
- Chart/diagram math and interaction models.
- New product features.

## Verification goals

- Existing unit tests remain green.
- Debug build succeeds after theme foundation and after each screen rollout.
- On-device review confirms dark-first contrast, one-handed Log interaction, readable numeric values, and smooth transitions across expand/collapse, navigation, set logging, and Progress filters.
- Reduced-motion behavior is checked before release.

## Approval history

- Hybrid direction approved by user on 2026-08-16.
- Dark-first composition approved by user on 2026-08-16.
- Night Training Console palette, hierarchy, and motion direction approved by user on 2026-08-16.

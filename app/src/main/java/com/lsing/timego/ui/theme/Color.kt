package com.lsing.timego.ui.theme

import androidx.compose.ui.graphics.Color

// "Ledger" palette — a lifter's paper training logbook, rendered dark-first. Chrome
// (backgrounds/surfaces/text/rules) only; chart-specific colors (heatmap scale, radar chart)
// live in their own domain/UI files and are re-picked separately within this same family.

// Dark theme (primary mode): warm near-black graphite, not the prior cool blue-black --
// closer to a ledger page under a desk lamp than a phone-screen black.
val LedgerBackgroundDark = Color(0xFF0D0C0A)
val LedgerSurfaceDark = Color(0xFF16140F)
val LedgerSurfaceVariantDark = Color(0xFF211E17)
val LedgerOnSurfaceDark = Color(0xFFEDE8DC)
val LedgerOnSurfaceVariantDark = Color(0xFFB8B2A0)
val LedgerOutlineDark = Color(0xFF7A7461)
val LedgerOutlineVariantDark = Color(0xFF332F25)

// Elevation/tonal ramp for the roles Material3 resolves from surfaceContainer* -- every role is
// set explicitly for the same reason the prior Onyx system set them explicitly: an unset role
// falls back to the baseline violet-grey palette in NavigationBar/ModalBottomSheet/AlertDialog/Card.
val LedgerSurfaceDimDark = Color(0xFF0A0908)
val LedgerSurfaceBrightDark = Color(0xFF322E24)
val LedgerSurfaceContainerLowestDark = Color(0xFF080706)
val LedgerSurfaceContainerLowDark = Color(0xFF19170F)
val LedgerSurfaceContainerDark = Color(0xFF1D1A13)
val LedgerSurfaceContainerHighDark = Color(0xFF27231A)
val LedgerSurfaceContainerHighestDark = Color(0xFF322D22)

// Light theme: real bone/aged-paper ground -- its own tonal pass, not a mechanical invert.
val LedgerBackgroundLight = Color(0xFFF6F1E4)
val LedgerSurfaceLight = Color(0xFFFBF8EF)
val LedgerSurfaceVariantLight = Color(0xFFE9E1CC)
val LedgerOnSurfaceLight = Color(0xFF211E17)
val LedgerOnSurfaceVariantLight = Color(0xFF534E3E)
val LedgerOutlineLight = Color(0xFF847D66)
val LedgerOutlineVariantLight = Color(0xFFD6CCAE)

val LedgerSurfaceDimLight = Color(0xFFE2D9C0)
val LedgerSurfaceBrightLight = Color(0xFFFBF8EF)
val LedgerSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val LedgerSurfaceContainerLowLight = Color(0xFFF8F3E5)
val LedgerSurfaceContainerLight = Color(0xFFF1EADA)
val LedgerSurfaceContainerHighLight = Color(0xFFEBE2CE)
val LedgerSurfaceContainerHighestLight = Color(0xFFE5DBC3)

// The one committed brand accent -- brick/ink red, carries the margin rule, active-row indicator,
// FAB, and primary buttons. Shared across light/dark (contrast-adjusted where Material requires
// an "on" color); no secondary/tertiary brand hues -- both roles collapse to graphite tonal steps
// so the palette stays Restrained (neutrals + one accent).
val LedgerAccent = Color(0xFFB33A2E)
val LedgerAccentDim = Color(0xFF8C2C22)
val LedgerOnAccent = Color(0xFFFFF3EC)
val LedgerSecondary = Color(0xFF8A8367)
val LedgerOnSecondary = Color(0xFF1D1A13)
val LedgerTertiary = Color(0xFF6E6A57)
val LedgerTertiaryDim = Color(0xFF56523F)
val LedgerOnTertiary = Color(0xFFF6F1E4)
val LedgerError = Color(0xFFCF4A3D)
val LedgerOnError = Color(0xFFFFFFFF)

// Tonal containers -- warm-shifted so filled chips, the nav indicator, and assist surfaces stay
// inside the ledger's graphite/paper family instead of Material's default violet-grey.
val LedgerAccentContainerDark = Color(0xFF4A160F)
val LedgerOnAccentContainerDark = Color(0xFFFFDAD2)
val LedgerSecondaryContainerDark = Color(0xFF3A3728)
val LedgerOnSecondaryContainerDark = Color(0xFFE5DFC7)
val LedgerTertiaryContainerDark = Color(0xFF302D20)
val LedgerOnTertiaryContainerDark = Color(0xFFDBD6C0)
val LedgerErrorContainerDark = Color(0xFF5C1A14)
val LedgerOnErrorContainerDark = Color(0xFFFFDAD5)

val LedgerAccentContainerLight = Color(0xFFFFDAD2)
val LedgerOnAccentContainerLight = Color(0xFF3A0E08)
val LedgerSecondaryContainerLight = Color(0xFFE5DFC7)
val LedgerOnSecondaryContainerLight = Color(0xFF201E10)
val LedgerTertiaryContainerLight = Color(0xFFDBD6C0)
val LedgerOnTertiaryContainerLight = Color(0xFF1C1A0F)
val LedgerErrorContainerLight = Color(0xFFFFDAD5)
val LedgerOnErrorContainerLight = Color(0xFF410300)

// Functional status color -- deliberately separate from the brand accent above, so a plateau
// flag never reads as "the app's red button." Used only for PlateauStatus (PROGRESSING /
// PLATEAUING / REGRESSING) and PR-moment highlighting, never for chrome or navigation.
val LedgerProgressing = Color(0xFF5E8C5A)
val LedgerPlateauing = Color(0xFFC98A2E)
val LedgerRegressing = LedgerError

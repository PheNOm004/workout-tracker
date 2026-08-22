package com.lsing.timego.ui.theme

import androidx.compose.ui.graphics.Color

// Night Training Console palette: dark gym surfaces with warm text and controlled training color.
// Ledger-prefixed names remain stable for existing call sites while the values move to the new
// semantic system.

// Dark-first surfaces.
val LedgerBackgroundDark = Color(0xFF101315)
val LedgerSurfaceDark = Color(0xFF181D20)
val LedgerSurfaceVariantDark = Color(0xFF22292C)
val LedgerOnSurfaceDark = Color(0xFFF4F1EA)
val LedgerOnSurfaceVariantDark = Color(0xFFAAB2B3)
val LedgerOutlineDark = Color(0xFF596366)
val LedgerOutlineVariantDark = Color(0xFF343B3E)

val LedgerSurfaceDimDark = Color(0xFF0C0F10)
val LedgerSurfaceBrightDark = Color(0xFF30383B)
val LedgerSurfaceContainerLowestDark = Color(0xFF0B0E0F)
val LedgerSurfaceContainerLowDark = Color(0xFF151A1C)
val LedgerSurfaceContainerDark = Color(0xFF1C2224)
val LedgerSurfaceContainerHighDark = Color(0xFF252C2F)
val LedgerSurfaceContainerHighestDark = Color(0xFF30383B)

// Warm light companion, intentionally not a mechanical inversion of dark mode.
val LedgerBackgroundLight = Color(0xFFF2F0EC)
val LedgerSurfaceLight = Color(0xFFFBFAF7)
val LedgerSurfaceVariantLight = Color(0xFFE7E4DF)
val LedgerOnSurfaceLight = Color(0xFF1B2022)
val LedgerOnSurfaceVariantLight = Color(0xFF596164)
val LedgerOutlineLight = Color(0xFF7C8587)
val LedgerOutlineVariantLight = Color(0xFFD2D5D4)

val LedgerSurfaceDimLight = Color(0xFFD9DCD9)
val LedgerSurfaceBrightLight = Color(0xFFFFFFFF)
val LedgerSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val LedgerSurfaceContainerLowLight = Color(0xFFF7F6F2)
val LedgerSurfaceContainerLight = Color(0xFFEDEBE6)
val LedgerSurfaceContainerHighLight = Color(0xFFE7E5E0)
val LedgerSurfaceContainerHighestLight = Color(0xFFE1E0DB)

// Brand/action color and its contrast-adjusted companion roles.
val LedgerAccent = Color(0xFFFF6B5E)
val LedgerAccentDim = Color(0xFFB94742)
val LedgerOnAccent = Color(0xFF32100D)
val LedgerSecondary = Color(0xFF9BA6A8)
val LedgerOnSecondary = Color(0xFF172022)
val LedgerTertiary = Color(0xFFB8A7FF)
val LedgerTertiaryDim = Color(0xFF7668B5)
val LedgerOnTertiary = Color(0xFF211A49)
val LedgerError = Color(0xFFFF8A80)
val LedgerOnError = Color(0xFF3B0906)

val LedgerAccentContainerDark = Color(0xFF68241F)
val LedgerOnAccentContainerDark = Color(0xFFFFDAD5)
val LedgerSecondaryContainerDark = Color(0xFF30393B)
val LedgerOnSecondaryContainerDark = Color(0xFFD8E1E2)
val LedgerTertiaryContainerDark = Color(0xFF39305F)
val LedgerOnTertiaryContainerDark = Color(0xFFE6DEFF)
val LedgerErrorContainerDark = Color(0xFF5C1A16)
val LedgerOnErrorContainerDark = Color(0xFFFFDAD5)

val LedgerAccentContainerLight = Color(0xFFFFDAD5)
val LedgerOnAccentContainerLight = Color(0xFF5A1712)
val LedgerSecondaryContainerLight = Color(0xFFDCE4E5)
val LedgerOnSecondaryContainerLight = Color(0xFF182123)
val LedgerTertiaryContainerLight = Color(0xFFE8E0FF)
val LedgerOnTertiaryContainerLight = Color(0xFF251B52)
val LedgerErrorContainerLight = Color(0xFFFFDAD5)
val LedgerOnErrorContainerLight = Color(0xFF410002)

// Semantic fitness colors. These never replace the brand accent for action chrome.
val NightMint = Color(0xFF9BD8B2)
val NightAmber = Color(0xFFF2B866)
val NightViolet = Color(0xFFB8A7FF)
val LedgerProgressing = NightMint
val LedgerPlateauing = NightAmber
val LedgerRegressing = LedgerError

// Explicit aliases for new presentation code and design-token readability.
val NightBackground = LedgerBackgroundDark
val NightSurface = LedgerSurfaceDark
val NightSurfaceRaised = LedgerSurfaceVariantDark
val NightCoral = LedgerAccent
val NightCoralShade = LedgerAccentDim

// NTC "Backlit" depth tokens. Dark-mode separation comes from tonal decks and light edges,
// not drop shadows; glow is reserved for active navigation, FABs, data, and pulse moments.
val NightDeckLow = Color(0xFF1A2125)
val NightDeckHigh = Color(0xFF232C30)
val NightEdgeHairline = Color(0x12FFFFFF)
val NightSheenTop = Color(0x08FFFFFF)
val NightGlow = Color(0x2AFF6B5E)

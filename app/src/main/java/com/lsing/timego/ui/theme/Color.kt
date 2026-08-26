package com.lsing.timego.ui.theme

import androidx.compose.ui.graphics.Color

// Engine-Room Gauge Panel palette: riveted steel surfaces read as machinery, not a themed dark
// mode. Ledger-prefixed names remain stable for existing call sites while the values move to the
// new semantic system -- only Brass/Dial/Rivet tokens below are new symbols.

// Dark-first surfaces (gunmetal steel, not neutral charcoal).
val LedgerBackgroundDark = Color(0xFF0D1113)
val LedgerSurfaceDark = Color(0xFF161B1E)
val LedgerSurfaceVariantDark = Color(0xFF1F262A)
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

// Brand/action color: brass instrument trim, not the old coral accent -- toggles, the FAB, and
// selected nav read as a machined brass fitting on the steel panel.
val LedgerAccent = Color(0xFFC9A24B)
val LedgerAccentDim = Color(0xFF8F7233)
val LedgerOnAccent = Color(0xFF241C0A)
val LedgerSecondary = Color(0xFF9BA6A8)
val LedgerOnSecondary = Color(0xFF172022)
val LedgerTertiary = Color(0xFFB8A7FF)
val LedgerTertiaryDim = Color(0xFF7668B5)
val LedgerOnTertiary = Color(0xFF211A49)
val LedgerError = Color(0xFFFF8A80)
val LedgerOnError = Color(0xFF3B0906)

val LedgerAccentContainerDark = Color(0xFF4A3B18)
val LedgerOnAccentContainerDark = Color(0xFFF3E3B8)
val LedgerSecondaryContainerDark = Color(0xFF30393B)
val LedgerOnSecondaryContainerDark = Color(0xFFD8E1E2)
val LedgerTertiaryContainerDark = Color(0xFF39305F)
val LedgerOnTertiaryContainerDark = Color(0xFFE6DEFF)
val LedgerErrorContainerDark = Color(0xFF5C1A16)
val LedgerOnErrorContainerDark = Color(0xFFFFDAD5)

val LedgerAccentContainerLight = Color(0xFFF3E3B8)
val LedgerOnAccentContainerLight = Color(0xFF4A3B18)
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

// Engine-Room Gauge Panel depth tokens. Unlike the retired Backlit rule (hairline-only, no
// shadow), riveted panels are meant to read as three-dimensional plate: a real cast shadow plus
// a brass-lit top bezel, not a flat tonal step.
val NightDeckLow = Color(0xFF181F23)
val NightDeckHigh = Color(0xFF212A2F)
val NightEdgeHairline = Color(0x1EFFFFFF)
val NightSheenTop = Color(0x14C9A24B)
val NightGlow = Color(0x2AC9A24B)

// Brass instrument trim -- the panel's one warm metal, reserved for controls and active state.
// Never used as a flat fill; always a rim, ring, or fitting.
val Brass = LedgerAccent
val BrassDim = LedgerAccentDim
val BrassHighlight = Color(0xFFE7C878)

// Rivets are drawn as small radial highlight/shadow pairs at panel corners -- see SurfaceCard's
// `riveted` flag.
val RivetHighlight = Color(0x4DFFFFFF)
val RivetShadow = Color(0x66000000)

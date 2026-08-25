package com.lsing.timego.ui.common

internal fun String.toFiniteDoubleOrNull(): Double? =
    toDoubleOrNull()?.takeIf(Double::isFinite)

internal fun String.toPositiveFiniteDoubleOrNull(): Double? =
    toFiniteDoubleOrNull()?.takeIf { it > 0.0 }

internal fun String.toPositiveIntOrNull(): Int? =
    toIntOrNull()?.takeIf { it > 0 }

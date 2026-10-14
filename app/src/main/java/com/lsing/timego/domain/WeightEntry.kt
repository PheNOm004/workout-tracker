package com.lsing.timego.domain

/** Dumbbell-style exercises can be typed per hand or as both hands combined. Stored weights are always
 *  per hand, so history, suggestions and charts stay comparable whichever way an exercise is entered. */
fun combinedEntryToStored(entered: Double, combined: Boolean): Double = if (combined) entered / 2.0 else entered

fun storedToCombinedEntry(stored: Double, combined: Boolean): Double = if (combined) stored * 2.0 else stored

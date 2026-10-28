package com.lsing.timego.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/*
 * PLATE COLOURS (see Color.kt). Both light and dark are first-class schemes designed on their own;
 * neither is an inversion of the other.
 *
 * Deliberately no dynamic (Material You / wallpaper-derived) colour: the plate palette is the identity.
 */
@Composable
fun TimeGoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalTimeGoDark provides darkTheme,
        LocalTimeGoPalette provides if (darkTheme) DarkPalette else LightPalette,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = Typography,
            shapes = TimeGoShapes,
            content = content,
        )
    }
}

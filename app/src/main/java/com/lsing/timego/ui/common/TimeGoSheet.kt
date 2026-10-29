package com.lsing.timego.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.statusBarsPadding
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.lsing.timego.ui.theme.LocalTimeGoPalette

/**
 * Blurs the app behind the surrounding dialog or sheet window (Android 12+; the system ignores it where
 * cross-window blur is off, leaving just the scrim). A sheet is its own window, so the in-app backdrop
 * blur used by the dock cannot reach it; the window-level blur is how it gets the same frosted depth.
 */
@Composable
fun BlurBehindWindow(radius: Dp = GlassBlurRadius) {
    val view = LocalView.current
    val radiusPx = with(LocalDensity.current) { radius.roundToPx() }
    SideEffect {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@SideEffect
        val window = (view as? DialogWindowProvider ?: view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        if (window.attributes.blurBehindRadius != radiusPx) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.also { it.blurBehindRadius = radiusPx }
        }
    }
}

/**
 * The app's modal bottom sheet: an opaque raised surface over a dimmed (and, where supported, blurred) app, offset
 * below the status bar so the content scrolls and clips at the sheet's own edge, with a rounded top and the standard drag handle. Always opens fully expanded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeGoBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = Modifier.statusBarsPadding(),
        containerColor = LocalTimeGoPalette.current.raised,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = LocalTimeGoPalette.current.scrim,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) },
    ) {
        BlurBehindWindow()
        content()
    }
}

package com.lsing.timego.ui.common

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * Keyboard actions for a row of single-line inputs: the Next key moves to the following field, the Done key
 * runs [onDone] (e.g. logs the set) and puts the keyboard away. Pair with `imeAction = ImeAction.Next` (or `Done` on the last field) and `singleLine`,
 * otherwise the Enter key inserts a new line instead.
 */
@Composable
fun rememberAdvanceKeyboardActions(onNext: (() -> Unit)? = null, onDone: (() -> Unit)? = null): KeyboardActions {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return KeyboardActions(
        onNext = {
            if (onNext != null) {
                // Explicit target: jumping straight to a named field avoids the focus traversal landing on a
                // non-text control, and re-showing the keyboard covers keyboards that blink on focus changes.
                onNext()
                keyboard?.show()
            } else {
                focus.moveFocus(FocusDirection.Next)
            }
        },
        onDone = {
            onDone?.invoke()
            focus.clearFocus()
            keyboard?.hide()
        },
    )
}

/** Returns an action that drops focus and hides the keyboard (call it after logging a set). */
@Composable
fun rememberDismissKeyboard(): () -> Unit {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return remember(focus, keyboard) {
        {
            focus.clearFocus()
            keyboard?.hide()
        }
    }
}

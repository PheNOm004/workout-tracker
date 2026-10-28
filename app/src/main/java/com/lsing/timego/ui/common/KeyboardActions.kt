package com.lsing.timego.ui.common

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * Keyboard actions for a row of single-line inputs: the Next key moves to the following field, the Done key
 * puts the keyboard away. Pair with `imeAction = ImeAction.Next` (or `Done` on the last field) and `singleLine`,
 * otherwise the Enter key inserts a new line instead.
 */
@Composable
fun rememberAdvanceKeyboardActions(): KeyboardActions {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return remember(focus, keyboard) {
        KeyboardActions(
            onNext = { focus.moveFocus(FocusDirection.Next) },
            onDone = {
                focus.clearFocus()
                keyboard?.hide()
            },
        )
    }
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

package com.lsing.timego.ui.log

import com.lsing.timego.data.RpeEntryMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EffortPromptTest {
    @Test fun promptsForUnratedWorkingSetInAfterSetMode() = assertTrue(shouldPromptForEffort(false, null, RpeEntryMode.AFTER_SET))
    @Test fun skipsWarmups() = assertFalse(shouldPromptForEffort(true, null, RpeEntryMode.AFTER_SET))
    @Test fun skipsAlreadyRatedSets() = assertFalse(shouldPromptForEffort(false, 8, RpeEntryMode.AFTER_SET))
    @Test fun skipsOtherModes() {
        assertFalse(shouldPromptForEffort(false, null, RpeEntryMode.SLIDER))
        assertFalse(shouldPromptForEffort(false, null, RpeEntryMode.OFF))
    }
}

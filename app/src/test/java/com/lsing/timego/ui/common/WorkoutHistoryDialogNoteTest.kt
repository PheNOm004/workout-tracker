package com.lsing.timego.ui.common

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutHistoryDialogNoteTest {
    @Test fun setNoteIsAppendedToItsHistoryEntry() {
        val ex = Exercise(id = 1, name = "Bench", muscleGroups = listOf("CHEST"), isCustom = false)
        val sets = listOf(
            SetLog(sessionId = 1, exerciseId = 1, weightKg = 60.0, reps = 8, targetReps = 8, loggedAtEpochMillis = 1, note = "left shoulder pinched"),
            SetLog(sessionId = 1, exerciseId = 1, weightKg = 60.0, reps = 8, targetReps = 8, loggedAtEpochMillis = 2),
        )
        val entry = buildDayHistoryEntries(sets, mapOf(1L to ex)).single()
        assertEquals(listOf("60.0kg x 8 - left shoulder pinched", "60.0kg x 8"), entry.setDescriptions)
    }
}

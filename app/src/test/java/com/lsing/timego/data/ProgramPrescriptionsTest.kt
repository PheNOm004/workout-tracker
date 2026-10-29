package com.lsing.timego.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgramPrescriptionsTest {
    @Test
    fun `the personal rir program is never seeded for other users`() {
        assertEquals(emptyList<String>(), SEED_ROUTINES.filter { it.programId == RIR_PROGRAM_ID }.map { it.name })
    }

    @Test
    fun `every rir prescription names a catalogue exercise`() {
        val catalogue = SEED_EXERCISES.map { it.name }.toSet()
        assertEquals(emptyList<String>(), RIR_PROGRAM_PRESCRIPTIONS.keys.filterNot { it in catalogue })
    }

    @Test
    fun `rir converts to rpe`() {
        assertEquals("7-9", RIR_PROGRAM_PRESCRIPTIONS.getValue("Pull-Up").rpeText)
        assertEquals("6-8", RIR_PROGRAM_PRESCRIPTIONS.getValue("Preacher Curl").rpeText)
    }
}

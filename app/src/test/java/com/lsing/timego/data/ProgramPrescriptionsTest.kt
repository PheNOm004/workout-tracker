package com.lsing.timego.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgramPrescriptionsTest {
    private val rirRoutines = SEED_ROUTINES.filter { it.programId == RIR_PROGRAM_ID }
    private val rirNames = rirRoutines.flatMap { it.exerciseNames }

    @Test
    fun `rir program has four routines of six exercises`() {
        assertEquals(listOf("Upper A", "Lower A", "Upper B", "Lower B"), rirRoutines.map { it.name })
        assertEquals(listOf(6, 6, 6, 6), rirRoutines.map { it.exerciseNames.size })
    }

    @Test
    fun `every rir exercise exists in the catalogue and has a prescription`() {
        val catalogue = SEED_EXERCISES.map { it.name }.toSet()
        assertEquals(emptyList<String>(), rirNames.filterNot { it in catalogue })
        assertEquals(emptyList<String>(), rirNames.filterNot { it in RIR_PROGRAM_PRESCRIPTIONS })
        assertEquals(rirNames.size, rirNames.toSet().size)
    }

    @Test
    fun `rir converts to rpe`() {
        assertEquals("7-9", RIR_PROGRAM_PRESCRIPTIONS.getValue("Pull-Up").rpeText)
        assertEquals("6-8", RIR_PROGRAM_PRESCRIPTIONS.getValue("Preacher Curl").rpeText)
    }
}

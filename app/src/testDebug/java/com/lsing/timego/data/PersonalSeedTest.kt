package com.lsing.timego.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Debug variant only: the personal program exists here and must stay consistent with its prescriptions. */
class PersonalSeedTest {
    private val names = PERSONAL_SEED_ROUTINES.flatMap { it.exerciseNames }

    @Test
    fun `personal program has four routines of six exercises`() {
        assertEquals(listOf("Upper A", "Lower A", "Upper B", "Lower B"), PERSONAL_SEED_ROUTINES.map { it.name })
        assertEquals(listOf(6, 6, 6, 6), PERSONAL_SEED_ROUTINES.map { it.exerciseNames.size })
    }

    @Test
    fun `every personal exercise exists in the catalogue and has a prescription`() {
        val catalogue = SEED_EXERCISES.map { it.name }.toSet()
        assertEquals(emptyList<String>(), names.filterNot { it in catalogue })
        assertEquals(emptyList<String>(), names.filterNot { it in RIR_PROGRAM_PRESCRIPTIONS })
    }
}

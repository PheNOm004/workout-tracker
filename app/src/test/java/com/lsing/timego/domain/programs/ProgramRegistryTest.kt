package com.lsing.timego.domain.programs

import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramRegistryTest {
    @Test
    fun `every program has at least one day-type`() {
        ProgramRegistry.ALL.forEach { program ->
            assertTrue("${program.id} has no day-types", program.dayTypes.isNotEmpty())
        }
    }

    @Test
    fun `every day-type has at least one slot and a non-empty region set`() {
        ProgramRegistry.ALL.forEach { program ->
            program.dayTypes.forEach { dayType ->
                assertTrue("${program.id}/${dayType.name} has no slots", dayType.slots.isNotEmpty())
                assertTrue("${program.id}/${dayType.name} has no region groups", dayType.regionGroups.isNotEmpty())
            }
        }
    }

    @Test
    fun `every slot's target groups are a subset of its day-type's region groups`() {
        ProgramRegistry.ALL.forEach { program ->
            program.dayTypes.forEach { dayType ->
                dayType.slots.forEach { slot ->
                    assertTrue(
                        "${program.id}/${dayType.name}/${slot.label} targets groups outside its day-type",
                        dayType.regionGroups.containsAll(slot.targetGroups),
                    )
                }
            }
        }
    }

    @Test
    fun `program ids are unique`() {
        val ids = ProgramRegistry.ALL.map { it.id }
        assertTrue(ids.size == ids.toSet().size)
    }
}

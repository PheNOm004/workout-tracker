package com.lsing.timego.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `muscleWeights round-trips through encode-decode`() {
        val original = mapOf("QUADS" to 100, "GLUTES" to 60)
        val encoded = converters.fromMuscleWeights(original)
        val decoded = converters.toMuscleWeights(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `blank muscleWeights string decodes to empty map`() {
        assertEquals(emptyMap<String, Int>(), converters.toMuscleWeights(""))
    }

    @Test
    fun `single-entry muscleWeights round-trips`() {
        val original = mapOf("ABS" to 40)
        assertEquals(original, converters.toMuscleWeights(converters.fromMuscleWeights(original)))
    }
}

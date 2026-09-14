package com.lsing.timego.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogueContractTest {
    private val root = File(requireNotNull(System.getProperty("user.dir"))).parentFile
    private val catalogue = File(root, "catalogue/exercises.json")

    @Test
    fun bundledCataloguePreservesEverySeedKey() {
        val json = catalogue.readText()
        val keys = Regex("\\\"key\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").findAll(json).map { it.groupValues[1] }.toList()
        val seedKeys = SEED_EXERCISES.mapNotNull(Exercise::catalogueKey)

        assertEquals(820, seedKeys.size)
        assertEquals(seedKeys.toSet(), keys.toSet())
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun nodeValidatorAcceptsCatalogueStructure() {
        val process = ProcessBuilder("node", "scripts/validate-catalogue.mjs", "--allow-incomplete")
            .directory(root)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()

        assertEquals(output, 0, process.waitFor())
        assertTrue(output, output.contains("Catalogue valid"))
    }
}

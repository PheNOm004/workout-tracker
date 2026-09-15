package com.lsing.timego

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseConfigurationTest {
    @Test
    fun `release signing reads every upload credential from the environment`() {
        val script = projectFile("app/build.gradle.kts").readText()
        val requiredVariables = listOf(
            "TIMEGO_UPLOAD_STORE_FILE",
            "TIMEGO_UPLOAD_STORE_PASSWORD",
            "TIMEGO_UPLOAD_KEY_ALIAS",
            "TIMEGO_UPLOAD_KEY_PASSWORD",
        )

        requiredVariables.forEach { variable ->
            assertTrue("Missing environment variable $variable", variable in script)
        }
        assertTrue(
            "Unsigned bundleRelease must be rejected",
            "bundleRelease requires all TIMEGO_UPLOAD_*" in script,
        )
        assertFalse(
            "Release credentials must not be literal Gradle strings",
            Regex("(?:storePassword|keyPassword)\\s*=\\s*\\\"[^$]", RegexOption.IGNORE_CASE)
                .containsMatchIn(script),
        )
    }

    private fun projectFile(relativePath: String): File {
        var directory = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (!File(directory, "settings.gradle.kts").isFile) {
            directory = requireNotNull(directory.parentFile) { "Cannot locate project root" }
        }
        return File(directory, relativePath)
    }
}

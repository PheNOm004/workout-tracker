package com.lsing.timego

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseDocumentationTest {
    @Test
    fun `Play release documents cover public data obligations`() {
        val requiredContent = mapOf(
            "docs/release/privacy-policy-draft.md" to listOf(
                "com.lsing.timego", "Firebase", "report consent", "retention", "account deletion",
            ),
            "docs/release/data-safety-matrix.md" to listOf(
                "com.lsing.timego", "Firebase", "weekly", "monthly", "deletion",
            ),
            "docs/release/play-console-checklist.md" to listOf(
                "Health Apps", "Data Safety", "closed test", "account deletion",
            ),
            "docs/release/account-deletion-page.md" to listOf(
                "com.lsing.timego", "Delete", "HTTPS", "support",
            ),
        )

        requiredContent.forEach { (path, needles) ->
            val file = projectFile(path)
            assertTrue("Missing required release document: $path", file.isFile)
            val text = file.readText()
            needles.forEach { needle ->
                assertTrue("$path must contain '$needle'", text.contains(needle, ignoreCase = true))
            }
        }
    }

    private fun projectFile(relativePath: String): File {
        var directory = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (!File(directory, "settings.gradle.kts").isFile) {
            directory = requireNotNull(directory.parentFile) { "Cannot locate project root" }
        }
        return File(directory, relativePath)
    }
}

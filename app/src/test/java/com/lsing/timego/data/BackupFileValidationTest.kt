package com.lsing.timego.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFileValidationTest {
    @Test
    fun copyBackupTo_acceptsContentAtLimit() {
        val source = byteArrayOf(1, 2, 3, 4)
        val output = ByteArrayOutputStream()

        ByteArrayInputStream(source).copyBackupTo(output, maxBytes = source.size.toLong())

        assertArrayEquals(source, output.toByteArray())
    }

    @Test
    fun copyBackupTo_rejectsContentOverLimit() {
        val error = assertThrows(IllegalStateException::class.java) {
            ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5)).copyBackupTo(
                ByteArrayOutputStream(),
                maxBytes = 4,
            )
        }

        assertTrue(error.message.orEmpty().contains("allowed restore size"))
    }

    @Test
    fun hasSqliteHeader_acceptsSqliteAndRejectsOtherFiles() {
        val sqlite = File.createTempFile("timego-valid-", ".db")
        val other = File.createTempFile("timego-invalid-", ".db")
        try {
            sqlite.writeBytes("SQLite format 3\u0000payload".toByteArray(Charsets.US_ASCII))
            other.writeText("not a database")

            assertTrue(sqlite.hasSqliteHeader())
            assertFalse(other.hasSqliteHeader())
        } finally {
            sqlite.delete()
            other.delete()
        }
    }
}

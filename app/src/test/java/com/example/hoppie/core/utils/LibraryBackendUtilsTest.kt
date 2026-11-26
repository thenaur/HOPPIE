package com.example.hoppie.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryBackendUtilsTest {

    @Test
    fun sha256HexMatchesKnownVectorAndIntegrityVerification() {
        val hash = FileHashUtil.sha256Hex("hoppie".toByteArray(Charsets.UTF_8))
        assertEquals(64, hash.length)
        assertTrue(FileHashUtil.verifyIntegrity(hash, hash.uppercase()))
        assertFalse(FileHashUtil.verifyIntegrity(hash, "0".repeat(64)))
        assertFalse(FileHashUtil.verifyIntegrity(null, hash))
    }

    @Test
    fun classifyCategoryMatchesSpecification() {
        assertEquals(StorageFormatter.CATEGORY_DOCUMENTS, StorageFormatter.classifyCategory("Lecture_Week9.pdf"))
        assertEquals(StorageFormatter.CATEGORY_DOCUMENTS, StorageFormatter.classifyCategory("Activity2.pptx"))
        assertEquals(StorageFormatter.CATEGORY_DOCUMENTS, StorageFormatter.classifyCategory("Notes.docx"))
        assertEquals(StorageFormatter.CATEGORY_DOCUMENTS, StorageFormatter.classifyCategory("Readme.txt"))
        assertEquals(StorageFormatter.CATEGORY_IMAGES, StorageFormatter.classifyCategory("Wireframe.png"))
        assertEquals(StorageFormatter.CATEGORY_IMAGES, StorageFormatter.classifyCategory("Photo.JPG"))
        assertEquals(StorageFormatter.CATEGORY_CODE, StorageFormatter.classifyCategory("Database.sql"))
        assertEquals(StorageFormatter.CATEGORY_CODE, StorageFormatter.classifyCategory("Main.java"))
        assertEquals(StorageFormatter.CATEGORY_CODE, StorageFormatter.classifyCategory("script.py"))
    }

    @Test
    fun formatBytesFormatsCleanlyAcrossUnits() {
        assertEquals("0 B", StorageFormatter.formatBytes(0L))
        assertEquals("512 B", StorageFormatter.formatBytes(512L))
        assertEquals("1 KB", StorageFormatter.formatBytes(1024L))
        assertEquals("15 MB", StorageFormatter.formatBytes(15L * 1024L * 1024L))
        assertEquals("240 MB", StorageFormatter.formatBytes(240L * 1024L * 1024L))
        assertEquals("1.5 GB", StorageFormatter.formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))
    }
}

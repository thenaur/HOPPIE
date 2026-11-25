package com.example.hoppie.data.nearby

import android.content.Context
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.example.hoppie.core.components.OfferedFile
import com.example.hoppie.core.components.sha256
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

@SdkSuppress(minSdkVersion = 29)
class ReceivedPdfStoreTest {
    @Test fun failedSecondFilePreservesFirstUriAndDuplicateNameGetsSeparateContent() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val store = ReceivedPdfStore(app)
        val name = "Hoppie-batch-${UUID.randomUUID()}.txt"
        val uris = mutableListOf<android.net.Uri>()
        try {
            val first = "first".toByteArray()
            val second = "other".toByteArray()
            val one = OfferedFile("a", name, 5, "text/plain", sha256 { first.inputStream() })
            val two = one.copy(id = "b", sha256 = sha256 { second.inputStream() })
            val names = com.example.hoppie.features.receive.reviewDuplicates(listOf(one, two), emptyList())
                .destinations(listOf(one, two))
            uris += store.save(one, names.getValue("a"), { true }) { first.inputStream() }
            assertTrue(runCatching { store.save(two, names.getValue("b"), { true }) { first.inputStream() } }.isFailure)
            assertTrue(runCatching { store.save(two, names.getValue("b"), { true }) { byteArrayOf(1).inputStream() } }.isFailure)
            uris += store.save(two, names.getValue("b"), { true }) { second.inputStream() }
            assertNotEquals(uris.first(), uris.last())
            assertEquals(one.sha256, sha256 { app.contentResolver.openInputStream(uris.first())!! })
            assertEquals(two.sha256, sha256 { app.contentResolver.openInputStream(uris.last())!! })
        } finally {
            uris.forEach { app.contentResolver.delete(it, null, null) }
        }
    }

    @Test fun publishesOnlyExactVerifiedBytes() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val store = ReceivedPdfStore(app)
        val bytes = "%PDF-1.4\n%%EOF".toByteArray()
        val name = "Hoppie-test-${UUID.randomUUID()}.pdf"
        val pdf = OfferedFile("0", name, bytes.size.toLong(), "application/pdf", sha256 { bytes.inputStream() })
        try {
            val failed = runCatching { store.save(pdf.copy(sha256 = "0".repeat(64)), name, { true }) { bytes.inputStream() } }
            assertTrue(failed.isFailure)
            assertFalse(store.inspect(listOf(pdf)).any { it.name == name })
            store.save(pdf, name, { true }) { bytes.inputStream() }
            assertEquals(pdf.sha256, store.inspect(listOf(pdf)).single { it.name == name }.sha256)
        } finally {
            app.contentResolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                "_display_name=? AND relative_path=?", arrayOf(name, "Download/HOPPIE/"))
        }
    }
}

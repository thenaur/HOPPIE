package com.example.hoppie.data.nearby

import com.example.hoppie.core.components.OfferedFile
import com.example.hoppie.features.send.MAX_FILE_BYTES
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID

internal enum class PdfCommand { OFFER, ACCEPT, SKIP, DECLINE, START, SAVED, FAIL, CANCEL }
internal data class ManifestEntry(val file: OfferedFile, val payloadId: Long)

internal data class TransferFrame(
    val transfer: UUID,
    val command: PdfCommand,
    val legacy: Boolean,
    val entries: List<ManifestEntry> = emptyList(),
    val ids: List<String> = emptyList()
) {
    fun encode(): ByteArray {
        validate()
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeUTF(if (legacy) "HOPPIE/PDF/1" else "HOPPIE/BATCH/1")
            out.writeLong(transfer.mostSignificantBits)
            out.writeLong(transfer.leastSignificantBits)
            out.writeUTF(command.name)
            if (command == PdfCommand.OFFER) {
                if (!legacy) out.writeInt(entries.size)
                for ((file, payload) in entries) {
                    out.writeLong(payload)
                    out.writeUTF(file.id)
                    out.writeUTF(file.name)
                    out.writeLong(file.sizeBytes)
                    out.writeUTF(requireNotNull(file.sha256))
                    if (!legacy) out.writeUTF(file.mimeType.orEmpty())
                }
            } else if (!legacy) {
                out.writeInt(ids.size)
                ids.forEach(out::writeUTF)
            }
        }
        require(bytes.size() <= MAX_CONTROL_BYTES)
        return bytes.toByteArray()
    }

    private fun validate() {
        if (command == PdfCommand.OFFER) {
            require(entries.size in 1..MAX_FILES && (!legacy || entries.size == 1))
            require(entries.map { it.payloadId }.distinct().size == entries.size)
            require(entries.map { it.file.id }.distinct().size == entries.size)
            for ((file, _) in entries) {
                require(file.id.length in 1..64 && file.sizeBytes in 0..MAX_FILE_BYTES)
                require(safeName(file.name) && (!legacy || file.name.endsWith(".pdf", true)))
                require(digest.matches(file.sha256.orEmpty()))
                require(file.mimeType.orEmpty().length <= 128 && file.mimeType.orEmpty().none(Char::isISOControl))
            }
        } else require(entries.isEmpty())
        require(ids.size <= MAX_FILES && ids.distinct().size == ids.size && ids.all { it.length in 1..64 })
        if (legacy) require(ids.isEmpty())
        else when (command) {
            PdfCommand.START, PdfCommand.SAVED -> require(ids.size == 1)
            PdfCommand.ACCEPT -> require(ids.isNotEmpty())
            else -> require(ids.isEmpty())
        }
    }

    companion object {
        const val MAX_FILES = 64
        private const val MAX_CONTROL_BYTES = 128 * 1024
        private val digest = Regex("[0-9a-f]{64}")
        fun safeName(name: String) = name.toByteArray().size in 1..250 && name !in setOf(".", "..") &&
            name.none { it == '/' || it == '\\' || it.isISOControl() }

        fun decode(bytes: ByteArray): TransferFrame {
            require(bytes.size <= MAX_CONTROL_BYTES)
            return DataInputStream(bytes.inputStream()).use { input ->
                val legacy = when (input.readUTF()) {
                    "HOPPIE/PDF/1" -> true
                    "HOPPIE/BATCH/1" -> false
                    else -> error("Unknown transfer protocol")
                }
                val transfer = UUID(input.readLong(), input.readLong())
                val cmd = PdfCommand.valueOf(input.readUTF())
                val count = if (legacy) { if (cmd == PdfCommand.OFFER) 1 else 0 } else input.readInt()
                require(count in 0..MAX_FILES)
                val frame = if (cmd == PdfCommand.OFFER) {
                    TransferFrame(transfer, cmd, legacy, List(count) {
                        val payload = input.readLong()
                        val id = input.readUTF()
                        val name = input.readUTF()
                        val size = input.readLong()
                        val hash = input.readUTF()
                        val mime = if (legacy) "application/pdf" else input.readUTF().ifEmpty { null }
                        ManifestEntry(OfferedFile(id, name, size, mime, hash), payload)
                    })
                } else TransferFrame(transfer, cmd, legacy, ids = List(count) { input.readUTF() })
                require(input.available() == 0)
                frame.apply { validate() }
            }
        }
    }
}

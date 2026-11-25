package com.example.hoppie.features.transfers

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import com.example.hoppie.features.send.FileTransferState
import com.example.hoppie.features.send.TransferStatus
import java.util.UUID
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal data class CompletedTransferFile(val name: String, val sizeBytes: Long, val verified: Boolean = true)

internal data class CompletedTransfer(
    val id: String,
    val completedAtMillis: Long,
    val files: List<CompletedTransferFile>,
    val received: Boolean = false,
    val completed: Boolean = true,
    val peerName: String? = null
) {
    val totalBytes: Long get() = files.filter { it.verified }.sumOf { it.sizeBytes }
}

internal class CompletedTransferHistory(context: Context) {
    private val preferences = context.getSharedPreferences("completed_transfers", Context.MODE_PRIVATE)

    fun record(transfer: FileTransferState, received: Boolean, peerName: String? = transfer.recipientName) {
        val files = transfer.files.map { CompletedTransferFile(it.file.name, it.file.sizeBytes, it.verified) }
        val entries = JSONArray()
        files.forEach { file ->
            entries.put(JSONObject().put("name", file.name).put("sizeBytes", file.sizeBytes).put("verified", file.verified))
        }
        val record = JSONObject()
            .put("completedAtMillis", System.currentTimeMillis())
            .put("files", entries)
            .put("received", received)
            .put("peerName", peerName)
            .put("completed", transfer.status == TransferStatus.Completed)
        preferences.edit { putString(UUID.randomUUID().toString(), record.toString()) }
    }

    fun observe() = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(Unit) }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(Unit)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate().map { load() }.flowOn(Dispatchers.IO)

    fun load(): List<CompletedTransfer> = preferences.all.mapNotNull { (id, value) ->
        val json = value as? String ?: return@mapNotNull null
        try {
            val record = JSONObject(json)
            val entries = record.getJSONArray("files")
            val files = List(entries.length()) { index ->
                val file = entries.getJSONObject(index)
                CompletedTransferFile(file.getString("name"), file.getLong("sizeBytes"), file.optBoolean("verified", true))
            }
            CompletedTransfer(id, record.getLong("completedAtMillis"), files,
                record.optBoolean("received", false), record.optBoolean("completed", true), record.optString("peerName").takeIf { it.isNotBlank() })
        } catch (_: JSONException) {
            null
        }
    }.sortedByDescending { it.completedAtMillis }
}

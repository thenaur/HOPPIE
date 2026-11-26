package com.example.hoppie.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "received_files",
    indices = [
        Index(value = ["sha256_hash"]),
        Index(value = ["category"]),
        Index(value = ["received_timestamp"])
    ]
)
data class ReceivedFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long,
    @ColumnInfo(name = "sender_name")
    val senderName: String,
    @ColumnInfo(name = "file_path")
    val filePath: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "sha256_hash")
    val sha256Hash: String,
    @ColumnInfo(name = "received_timestamp")
    val receivedTimestamp: Long,
    @ColumnInfo(name = "is_shareable")
    val isShareable: Boolean = true
)

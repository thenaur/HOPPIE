package com.example.hoppie.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hoppie.data.local.entities.ReceivedFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceivedFileDao {

    @Query("SELECT * FROM received_files ORDER BY received_timestamp DESC")
    fun observeAllByNewest(): Flow<List<ReceivedFileEntity>>

    @Query("SELECT * FROM received_files ORDER BY received_timestamp ASC")
    fun observeAllByOldest(): Flow<List<ReceivedFileEntity>>

    @Query("SELECT * FROM received_files ORDER BY LOWER(file_name) ASC")
    fun observeAllByName(): Flow<List<ReceivedFileEntity>>

    @Query("SELECT * FROM received_files ORDER BY file_size_bytes DESC")
    fun observeAllBySize(): Flow<List<ReceivedFileEntity>>

    @Query(
        """
        SELECT * FROM received_files
        WHERE (:category IS NULL OR category = :category)
          AND (
              :searchQuery = ''
              OR LOWER(file_name) LIKE '%' || LOWER(:searchQuery) || '%'
              OR LOWER(sender_name) LIKE '%' || LOWER(:searchQuery) || '%'
          )
        ORDER BY
          CASE WHEN :sortKey = 'NEWEST' THEN received_timestamp END DESC,
          CASE WHEN :sortKey = 'OLDEST' THEN received_timestamp END ASC,
          CASE WHEN :sortKey = 'NAME' THEN LOWER(file_name) END ASC,
          CASE WHEN :sortKey = 'SIZE' THEN file_size_bytes END DESC,
          received_timestamp DESC
        """
    )
    fun observeFiltered(
        searchQuery: String,
        category: String?,
        sortKey: String
    ): Flow<List<ReceivedFileEntity>>

    @Query("SELECT * FROM received_files WHERE LOWER(sha256_hash) = LOWER(:sha256Hash) LIMIT 1")
    suspend fun findBySha256(sha256Hash: String): ReceivedFileEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM received_files WHERE LOWER(sha256_hash) = LOWER(:sha256Hash))")
    suspend fun existsBySha256(sha256Hash: String): Boolean

    @Query("SELECT * FROM received_files WHERE file_path = :filePath LIMIT 1")
    suspend fun findByFilePath(filePath: String): ReceivedFileEntity?

    @Query("SELECT COALESCE(SUM(file_size_bytes), 0) FROM received_files")
    fun observeTotalSizeBytes(): Flow<Long>

    @Query("SELECT COUNT(*) FROM received_files")
    fun observeFileCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(file_size_bytes), 0) FROM received_files")
    suspend fun getTotalSizeBytes(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: ReceivedFileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<ReceivedFileEntity>)

    @Query("UPDATE received_files SET is_shareable = :isShareable WHERE id = :id")
    suspend fun updateShareable(id: Long, isShareable: Boolean)

    @Delete
    suspend fun delete(file: ReceivedFileEntity)

    @Query("DELETE FROM received_files WHERE id = :id")
    suspend fun deleteById(id: Long)
}

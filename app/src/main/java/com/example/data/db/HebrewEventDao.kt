package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HebrewEventDao {
    @Query("SELECT * FROM hebrew_events ORDER BY createdAt DESC")
    fun getAllEvents(): Flow<List<HebrewEventEntity>>

    @Query("SELECT * FROM hebrew_events WHERE id = :id")
    suspend fun getEventById(id: Long): HebrewEventEntity?

    @Query("SELECT * FROM hebrew_events WHERE title = :title")
    suspend fun getEventsByTitle(title: String): List<HebrewEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: HebrewEventEntity): Long

    @Update
    suspend fun updateEvent(event: HebrewEventEntity)

    @Delete
    suspend fun deleteEvent(event: HebrewEventEntity)

    @Query("DELETE FROM hebrew_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("DELETE FROM hebrew_events WHERE title = :title")
    suspend fun deleteEventsByTitle(title: String): Int

    @Query("DELETE FROM hebrew_events")
    suspend fun deleteAllEvents()
}

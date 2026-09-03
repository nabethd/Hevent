package com.example.data.repository

import com.example.data.db.HebrewEventDao
import com.example.data.db.HebrewEventEntity
import kotlinx.coroutines.flow.Flow

class HebrewEventRepository(private val dao: HebrewEventDao) {
    val allEvents: Flow<List<HebrewEventEntity>> = dao.getAllEvents()

    suspend fun getEventById(id: Long): HebrewEventEntity? = dao.getEventById(id)

    suspend fun insertEvent(event: HebrewEventEntity): Long = dao.insertEvent(event)

    suspend fun updateEvent(event: HebrewEventEntity) = dao.updateEvent(event)

    suspend fun deleteEvent(event: HebrewEventEntity) = dao.deleteEvent(event)

    suspend fun deleteEventById(id: Long) = dao.deleteEventById(id)

    suspend fun deleteEventsByTitle(title: String): Int = dao.deleteEventsByTitle(title)

    suspend fun deleteAllEvents() = dao.deleteAllEvents()
}

package com.example.domain.calendar

data class DeviceCalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
    val isPrimary: Boolean,
    val isLocal: Boolean
)

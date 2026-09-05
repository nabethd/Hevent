package com.example.data.db

import androidx.room.TypeConverter
import com.example.domain.model.EventType
import com.example.domain.model.LeapYearRule
import com.example.domain.model.RecurrenceType

/**
 * Enums are stored as their [Enum.name]. Reads are total — an unknown string degrades to the
 * enum's default instead of throwing, so a hand-edited or future-versioned row cannot crash a
 * query.
 */
class Converters {
    @TypeConverter fun eventTypeToString(value: EventType): String = value.name
    @TypeConverter fun stringToEventType(value: String?): EventType = EventType.fromId(value)

    @TypeConverter fun recurrenceToString(value: RecurrenceType): String = value.name
    @TypeConverter fun stringToRecurrence(value: String?): RecurrenceType = RecurrenceType.fromId(value)

    @TypeConverter fun leapRuleToString(value: LeapYearRule): String = value.name
    @TypeConverter fun stringToLeapRule(value: String?): LeapYearRule = LeapYearRule.fromId(value)
}

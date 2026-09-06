package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.HebrewEventEntity
import com.example.domain.hebrew.HebrewCalendarEngine
import com.example.domain.model.HebrewDateInfo
import com.example.domain.model.RecurrenceType
import com.example.ui.i18n.AppStrings
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

data class CalendarDayItem(
    val dayOfMonth: Int,
    val month: Int,
    val year: Int,
    val isCurrentMonth: Boolean,
    val hebrewDateInfo: HebrewDateInfo,
    val isToday: Boolean
)

@Composable
fun CalendarViewScreen(
    strings: AppStrings,
    year: Int,
    month: Int,
    selectedDay: Int,
    events: List<HebrewEventEntity>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onDaySelect: (Int) -> Unit,
    onAddEventForDate: (HebrewDateInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    // From the active locale rather than a hardcoded pair of arrays — these were the last
    // bilingual literals left in the UI, and they were rebuilt on every recomposition.
    val monthNames = remember(strings.lang) {
        DateFormatSymbols(Locale(strings.lang.code)).months
    }

    val todayCal = remember { Calendar.getInstance() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH) + 1
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    // Compute month grid
    val daysInGrid by remember(year, month) {
        derivedStateOf {
            val list = mutableListOf<CalendarDayItem>()
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, ... 7 = Saturday
            val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            // Padding days from previous month
            val prevCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            val maxPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val paddingCount = startDayOfWeek - Calendar.SUNDAY

            for (i in paddingCount - 1 downTo 0) {
                val pDay = maxPrevMonth - i
                val pMonth = prevCal.get(Calendar.MONTH) + 1
                val pYear = prevCal.get(Calendar.YEAR)
                val hInfo = HebrewCalendarEngine.fromGregorian(pYear, pMonth, pDay)
                list.add(
                    CalendarDayItem(
                        dayOfMonth = pDay,
                        month = pMonth,
                        year = pYear,
                        isCurrentMonth = false,
                        hebrewDateInfo = hInfo,
                        isToday = pYear == todayYear && pMonth == todayMonth && pDay == todayDay
                    )
                )
            }

            // Days of current month
            for (d in 1..maxDaysInMonth) {
                val hInfo = HebrewCalendarEngine.fromGregorian(year, month, d)
                list.add(
                    CalendarDayItem(
                        dayOfMonth = d,
                        month = month,
                        year = year,
                        isCurrentMonth = true,
                        hebrewDateInfo = hInfo,
                        isToday = year == todayYear && month == todayMonth && d == todayDay
                    )
                )
            }

            // Fill trailing days to complete full weeks
            val remaining = (7 - (list.size % 7)) % 7
            val nextCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            for (d in 1..remaining) {
                val nMonth = nextCal.get(Calendar.MONTH) + 1
                val nYear = nextCal.get(Calendar.YEAR)
                val hInfo = HebrewCalendarEngine.fromGregorian(nYear, nMonth, d)
                list.add(
                    CalendarDayItem(
                        dayOfMonth = d,
                        month = nMonth,
                        year = nYear,
                        isCurrentMonth = false,
                        hebrewDateInfo = hInfo,
                        isToday = nYear == todayYear && nMonth == todayMonth && d == todayDay
                    )
                )
            }

            list
        }
    }

    // Selected day info
    val selectedDayInfo = remember(year, month, selectedDay) {
        HebrewCalendarEngine.fromGregorian(year, month, selectedDay)
    }

    /**
     * Which events fall on each visible day, keyed by Gregorian date.
     *
     * This is built from [HebrewCalendarEngine] rather than by comparing Hebrew day/month numbers.
     * The previous version compared them by hand in two separate places, and both copies ignored
     * the event's chosen Adar rule and the 30-Cheshvan / 30-Kislev shifts — so the grid marked days
     * that were never synced and missed days that were.
     */
    val eventsByDate: Map<Int, List<HebrewEventEntity>> = remember(events, daysInGrid) {
        if (daysInGrid.isEmpty()) return@remember emptyMap()

        val index = mutableMapOf<Int, MutableList<HebrewEventEntity>>()
        val hebrewYears = daysInGrid.map { it.hebrewDateInfo.hebrewYear }
        val yearSpan = (hebrewYears.min())..(hebrewYears.max())

        for (event in events) {
            if (event.recurrenceType == RecurrenceType.ONE_TIME) {
                // A single date: indexed exactly, never projected across years.
                val occ = HebrewCalendarEngine.singleOccurrence(
                    event.hebrewYear, event.hebrewMonth, event.hebrewDay
                )
                index.getOrPut(gregorianKey(occ.gregorianYear, occ.gregorianMonth, occ.gregorianDay)) {
                    mutableListOf()
                }.add(event)
            } else if (event.recurrenceType == RecurrenceType.MONTHLY) {
                for (day in daysInGrid) {
                    val info = day.hebrewDateInfo
                    val landsOn = HebrewCalendarEngine.monthlyDayIn(
                        info.hebrewYear,
                        info.hebrewMonth,
                        event.hebrewDay
                    )
                    if (info.hebrewDay == landsOn) {
                        index.getOrPut(gregorianKey(day.year, day.month, day.dayOfMonth)) { mutableListOf() }
                            .add(event)
                    }
                }
            } else {
                HebrewCalendarEngine.occurrencesInHebrewYears(
                    originHebrewYear = event.hebrewYear,
                    originHebrewMonth = event.hebrewMonth,
                    originHebrewDay = event.hebrewDay,
                    leapYearRule = event.leapYearRule,
                    years = yearSpan
                ).forEach { occ ->
                    index.getOrPut(gregorianKey(occ.gregorianYear, occ.gregorianMonth, occ.gregorianDay)) {
                        mutableListOf()
                    }.add(event)
                }
            }
        }
        index
    }

    val matchingEvents = eventsByDate[gregorianKey(year, month, selectedDay)].orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Month Header Controls
        ElevatedCard(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevMonth,
                    modifier = Modifier.testTag("cal_prev_month")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.prevMonth
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${monthNames[month - 1]} $year",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Display Hebrew Month span
                    val midMonthJd = remember(year, month) {
                        HebrewCalendarEngine.fromGregorian(year, month, 15)
                    }
                    Text(
                        text = "${midMonthJd.hebrewMonthNameHe} ${midMonthJd.hebrewYearStr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.testTag("cal_next_month")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = strings.nextMonth
                    )
                }

                OutlinedButton(
                    onClick = onTodayClick,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("cal_today_button")
                ) {
                    Text(strings.todayBtn, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Weekday Headers (RTL: Sunday through Saturday)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val weekdays = listOf(strings.sun, strings.mon, strings.tue, strings.wed, strings.thu, strings.fri, strings.sat)
            weekdays.forEach { dayName ->
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Days Grid (6 rows of 7 days)
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(daysInGrid) { item ->
                val isSelected = item.isCurrentMonth && item.dayOfMonth == selectedDay

                val hasEventOnDay =
                    eventsByDate.containsKey(gregorianKey(item.year, item.month, item.dayOfMonth))

                val backgroundColor = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    item.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    !item.isCurrentMonth -> MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.surface
                }

                val cellDescription = buildString {
                    append(item.dayOfMonth).append(' ').append(monthNames[item.month - 1])
                    append(", ").append(item.hebrewDateInfo.formattedHe)
                    if (item.isToday) append(", ").append(strings.todayBtn)
                    if (hasEventOnDay && item.isCurrentMonth) append(", ").append(strings.dayHasEvents)
                }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .semantics(mergeDescendants = true) {
                            contentDescription = cellDescription
                            selected = isSelected
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(backgroundColor)
                        .then(
                            if (isSelected) Modifier
                            else if (item.isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        )
                        .clickable(enabled = item.isCurrentMonth) {
                            onDaySelect(item.dayOfMonth)
                        }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Gregorian day number
                        Text(
                            text = item.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected || item.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                item.isToday -> MaterialTheme.colorScheme.primary
                                item.isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            }
                        )
                        // Hebrew day letters
                        Text(
                            text = item.hebrewDateInfo.hebrewDayStr,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                item.isToday -> MaterialTheme.colorScheme.primary
                                item.isCurrentMonth -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                            }
                        )

                        // Event indicator dot
                        if (hasEventOnDay && item.isCurrentMonth) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Day Details Card
        ElevatedCard(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedDayInfo.formattedHe,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${selectedDayInfo.gregorianDay}/${selectedDayInfo.gregorianMonth}/${selectedDayInfo.gregorianYear}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { onAddEventForDate(selectedDayInfo) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("add_event_for_date_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.addEventForDay, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }

                if (matchingEvents.isEmpty()) {
                    item {
                        Text(
                            text = strings.noEventsOnDay,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    }
                } else {
                    item {
                        Text(
                            text = strings.eventsOnDay,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(matchingEvents) { ev ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = ev.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = when (ev.recurrenceType) {
                                            RecurrenceType.ONE_TIME -> strings.recurOneTimeShort
                                            RecurrenceType.MONTHLY -> strings.recurMonthlyShort
                                            RecurrenceType.YEARLY -> strings.recurYearlyShort
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (ev.isSyncedToCalendar) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = strings.syncedBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Packs a Gregorian date into a single comparable key. */
private fun gregorianKey(year: Int, month: Int, day: Int): Int = year * 10_000 + month * 100 + day

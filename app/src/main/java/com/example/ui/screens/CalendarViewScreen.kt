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
import com.example.ui.i18n.AppStrings
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.util.Calendar

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

    // Filter events matching selected Hebrew date
    val matchingEvents = remember(events, selectedDayInfo) {
        events.filter { ev ->
            if (ev.recurrenceType == "MONTHLY") {
                ev.hebrewDay == selectedDayInfo.hebrewDay
            } else {
                // Check if this occurrence matches
                ev.hebrewDay == selectedDayInfo.hebrewDay &&
                    (ev.hebrewMonth == selectedDayInfo.hebrewMonth ||
                        (selectedDayInfo.isLeapYear && ev.hebrewMonth == JewishDate.ADAR && selectedDayInfo.hebrewMonth == JewishDate.ADAR_II))
            }
        }
    }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPrevMonth,
                        modifier = Modifier.testTag("cal_prev_month")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }

                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.testTag("cal_next_month")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val gregorianMonthNamesHe = listOf(
                        "ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני",
                        "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר"
                    )
                    val gregorianMonthNamesEn = listOf(
                        "January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December"
                    )

                    val monthName = if (strings.isHe) gregorianMonthNamesHe[month - 1] else gregorianMonthNamesEn[month - 1]
                    Text(
                        text = "$monthName $year",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Display Hebrew Month span
                    val midMonthJd = HebrewCalendarEngine.fromGregorian(year, month, 15)
                    Text(
                        text = "${midMonthJd.hebrewMonthNameHe} ${midMonthJd.hebrewYearStr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
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

                // Check if this day has any matching recurring events
                val hasEventOnDay = events.any { ev ->
                    if (ev.recurrenceType == "MONTHLY") {
                        ev.hebrewDay == item.hebrewDateInfo.hebrewDay
                    } else {
                        ev.hebrewDay == item.hebrewDateInfo.hebrewDay &&
                            (ev.hebrewMonth == item.hebrewDateInfo.hebrewMonth ||
                                (item.hebrewDateInfo.isLeapYear && ev.hebrewMonth == JewishDate.ADAR && item.hebrewDateInfo.hebrewMonth == JewishDate.ADAR_II))
                    }
                }

                val backgroundColor = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    item.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    !item.isCurrentMonth -> MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.surface
                }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
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
                                isSelected -> Color.White
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
                                isSelected -> Color.White.copy(alpha = 0.9f)
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
                                    .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
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
                            Text(if (strings.isHe) "אירוע ליום זה" else "Add Event", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
                                        text = if (ev.recurrenceType == "MONTHLY") strings.recurMonthly else strings.recurYearly,
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

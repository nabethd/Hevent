package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.HebrewEventEntity
import com.example.domain.hebrew.HebrewCalendarEngine
import com.example.domain.model.EventType
import com.example.domain.model.RecurrenceType
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AnniversaryAccent
import com.example.ui.theme.AnniversaryContainer
import com.example.ui.theme.BirthdayAccent
import com.example.ui.theme.BirthdayContainer
import com.example.ui.theme.GeneralEventAccent
import com.example.ui.theme.GeneralEventContainer
import com.example.ui.theme.HeroGradient
import com.example.ui.theme.HeroGradientDark
import com.example.ui.theme.HolidayAccent
import com.example.ui.theme.HolidayContainer
import com.example.ui.theme.YahrtzeitAccent
import com.example.ui.theme.YahrtzeitContainer
import com.example.ui.theme.AnniversaryAccentDark
import com.example.ui.theme.AnniversaryContainerDark
import com.example.ui.theme.BirthdayAccentDark
import com.example.ui.theme.BirthdayContainerDark
import com.example.ui.theme.GeneralEventAccentDark
import com.example.ui.theme.GeneralEventContainerDark
import com.example.ui.theme.HolidayAccentDark
import com.example.ui.theme.HolidayContainerDark
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.YahrtzeitAccentDark
import com.example.ui.theme.YahrtzeitContainerDark
import java.util.Calendar

@Composable
fun HomeScreen(
    strings: AppStrings,
    events: List<HebrewEventEntity>,
    onAddEventClick: () -> Unit,
    onDeleteEvent: (HebrewEventEntity) -> Unit,
    onEditEvent: (HebrewEventEntity) -> Unit,
    onExportIcs: (HebrewEventEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var eventToDelete by remember { mutableStateOf<HebrewEventEntity?>(null) }
    val todayInfo = remember { HebrewCalendarEngine.getToday() }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Atmospheric Hero Banner with Live Hebrew Date & Quick Stats
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(if (LocalIsDarkTheme.current) HeroGradientDark else HeroGradient))
                            .padding(22.dp)
                    ) {
                        Column {
                            // Top Tag: Hebrew Calendar badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = strings.todayHebrewDate,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (todayInfo.isLeapYear) strings.leapYearTag else strings.regularYearTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.95f),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Primary Hebrew Date Display
                            Text(
                                text = todayInfo.formattedHe,
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Gregorian Equivalent
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = todayInfo.gregorianFormatted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Scheduled count. The "new event" button that used to sit beside it
                            // duplicated the FAB two rows below.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${events.size} ${strings.eventsCountLabel}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            if (events.isEmpty()) {
                // Empty State with polished presentation
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = strings.emptyEventsTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.emptyEventsSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onAddEventClick,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                                modifier = Modifier.testTag("empty_add_event_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.addEventFab,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.trackedEvents,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${events.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                items(events, key = { it.id }) { event ->
                    HebrewEventCard(
                        event = event,
                        strings = strings,
                        onExportIcs = { onExportIcs(event) },
                        onEditClick = { onEditEvent(event) },
                        onDeleteClick = { eventToDelete = event }
                    )
                }
            }
        }

        // Extended Floating Action Button
        ExtendedFloatingActionButton(
            onClick = onAddEventClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(50),
            elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_event_fab"),
            icon = { Icon(Icons.Default.Add, contentDescription = strings.addEventFab) },
            text = {
                Text(
                    text = strings.addEventFab,
                    fontWeight = FontWeight.Bold
                )
            }
        )
    }

    // Confirmation dialog for deletion
    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = {
                Text(
                    text = strings.deleteEventConfirmTitle,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("${strings.deleteEventConfirmMsg(event.occurrenceCount)}\n\n״${event.title}״")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEvent(event)
                        eventToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(strings.delete, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { eventToDelete = null },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(strings.cancel)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun HebrewEventCard(
    event: HebrewEventEntity,
    strings: AppStrings,
    onExportIcs: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Next occurrence calculation
    val nextOccurrences = remember(event) {
        if (event.recurrenceType == RecurrenceType.MONTHLY) {
            HebrewCalendarEngine.calculateMonthlyOccurrences(
                originHebrewDay = event.hebrewDay,
                monthsCount = 3
            )
        } else {
            HebrewCalendarEngine.calculateYearlyOccurrences(
                originHebrewYear = event.hebrewYear,
                originHebrewMonth = event.hebrewMonth,
                originHebrewDay = event.hebrewDay,
                leapYearRule = event.leapYearRule,
                yearsCount = 3
            )
        }
    }

    // Days countdown
    val daysUntil = remember(nextOccurrences) {
        val first = nextOccurrences.firstOrNull() ?: return@remember null
        val targetCal = Calendar.getInstance().apply {
            set(first.gregorianYear, first.gregorianMonth - 1, first.gregorianDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = targetCal.timeInMillis - todayCal.timeInMillis
        (diffMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    val categoryConfig = categoryStyle(event.eventType, strings)

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("event_card_${event.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Category Pill + Countdown Pill + Delete Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                Surface(
                    shape = RoundedCornerShape(50),
                    color = categoryConfig.containerColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            categoryConfig.icon,
                            contentDescription = null,
                            tint = categoryConfig.color,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = categoryConfig.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryConfig.color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Countdown Badge
                    if (daysUntil != null) {
                        val badgeText = when {
                            daysUntil <= 0 -> strings.todayBadge
                            daysUntil == 1 -> strings.tomorrowBadge
                            daysUntil <= 30 -> strings.inDays(daysUntil)
                            else -> strings.inMonths(daysUntil / 30)
                        }
                        val isImminent = daysUntil in 0..7

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isImminent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isImminent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isImminent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isImminent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("edit_event_${event.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = strings.edit,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_event_${event.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = strings.delete,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title and Hebrew Date
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = event.hebrewDateFormatted,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Badges Row: Sync status & Recurrence rule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (event.isSyncedToCalendar) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${strings.syncedBadge} (${event.syncedEventsCount})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (event.recurrenceType == RecurrenceType.MONTHLY) strings.recurMonthly else strings.recurYearly,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Next occurrences scannable preview container
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = strings.nextUpcoming,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    nextOccurrences.take(2).forEachIndexed { index, occ ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${occ.gregorianDay}/${occ.gregorianMonth}/${occ.gregorianYear}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = occ.hebrewDateFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (index == 0 && nextOccurrences.size > 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Card Action: Export / Share ICS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onExportIcs,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("export_ics_${event.id}")
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.exportIcs,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * The container colours are pale pastels that only work on a light surface, so they need a dark
 * counterpart now that the app follows the system theme.
 */
@Composable
private fun categoryStyle(type: EventType, strings: AppStrings): CategoryStyle {
    val dark = LocalIsDarkTheme.current
    return when (type) {
        EventType.BIRTHDAY -> CategoryStyle(
            Icons.Default.Cake,
            if (dark) BirthdayAccentDark else BirthdayAccent,
            if (dark) BirthdayContainerDark else BirthdayContainer,
            strings.typeBirthday
        )
        EventType.YAHRZEIT -> CategoryStyle(
            Icons.Default.Whatshot,
            if (dark) YahrtzeitAccentDark else YahrtzeitAccent,
            if (dark) YahrtzeitContainerDark else YahrtzeitContainer,
            strings.typeYahrzeit
        )
        EventType.ANNIVERSARY -> CategoryStyle(
            Icons.Default.Favorite,
            if (dark) AnniversaryAccentDark else AnniversaryAccent,
            if (dark) AnniversaryContainerDark else AnniversaryContainer,
            strings.typeAnniversary
        )
        EventType.HOLIDAY -> CategoryStyle(
            Icons.Default.AutoAwesome,
            if (dark) HolidayAccentDark else HolidayAccent,
            if (dark) HolidayContainerDark else HolidayContainer,
            strings.typeHoliday
        )
        EventType.CUSTOM -> CategoryStyle(
            Icons.Default.Bookmark,
            if (dark) GeneralEventAccentDark else GeneralEventAccent,
            if (dark) GeneralEventContainerDark else GeneralEventContainer,
            strings.typeCustom
        )
    }
}

private data class CategoryStyle(
    val icon: ImageVector,
    val color: Color,
    val containerColor: Color,
    val label: String
)

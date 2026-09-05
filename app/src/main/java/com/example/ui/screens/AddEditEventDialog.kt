package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.calendar.DeviceCalendarInfo
import com.example.domain.hebrew.HebrewCalendarEngine
import com.example.domain.model.CalculatedOccurrence
import com.example.domain.model.HebrewDateInfo
import com.example.domain.model.LeapYearRule
import com.example.ui.i18n.AppStrings
import com.example.ui.viewmodel.HebrewCalendarViewModel
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditEventDialog(
    strings: AppStrings,
    availableCalendars: List<DeviceCalendarInfo>,
    hasCalendarPermission: Boolean,
    onRequestCalendarPermission: () -> Unit,
    onCreateNewCalendar: suspend (String) -> Long?,
    onDismiss: () -> Unit,
    onSaveBatch: (
        events: List<HebrewCalendarViewModel.EventDraft>,
        targetCalendarId: Long?,
        targetCalendarName: String?,
        isIcsOnly: Boolean
    ) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var eventTitle by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("BIRTHDAY") }
    var isHebrewInputMode by remember { mutableStateOf(false) } // False = Gregorian, True = Hebrew

    // Gregorian initial values (e.g. 13 Oct 1993 from user's example)
    val today = remember { Calendar.getInstance() }
    var gDay by remember { mutableIntStateOf(13) }
    var gMonth by remember { mutableIntStateOf(10) }
    var gYear by remember { mutableIntStateOf(1993) }

    // Hebrew initial values
    var hDay by remember { mutableIntStateOf(28) }
    var hMonth by remember { mutableIntStateOf(JewishDate.TISHREI) }
    var hYear by remember { mutableIntStateOf(5754) }

    // Recurrence
    var recurrenceType by remember { mutableStateOf("YEARLY") } // YEARLY or MONTHLY

    // Duration (in years) chosen by user: defaults to 20
    var yearsDuration by remember { mutableIntStateOf(20) }

    // Multi-event queue / batch list
    val stagedEvents = remember { mutableStateListOf<HebrewCalendarViewModel.EventDraft>() }

    // Halachic Leap Year rule
    var leapYearRule by remember { mutableStateOf(LeapYearRule.STANDARD_ADAR_II) }
    var showHalachicInfo by remember { mutableStateOf(false) }

    // Sync destination: "EXISTING_CAL", "NEW_CAL", "ICS_ONLY"
    var syncDestination by remember {
        mutableStateOf(if (hasCalendarPermission && availableCalendars.isNotEmpty()) "EXISTING_CAL" else if (hasCalendarPermission) "NEW_CAL" else "ICS_ONLY")
    }
    var selectedCalendarId by remember {
        mutableLongStateOf(availableCalendars.firstOrNull()?.id ?: 0L)
    }
    var newCalendarName by remember { mutableStateOf(if (strings.isHe) "אירועים עבריים" else "Hebrew Events") }

    var isCreatingCal by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var creationErrorFeedback by remember { mutableStateOf<String?>(null) }

    // Automatically select the best calendar (Google Calendar or primary) when calendars become available
    LaunchedEffect(availableCalendars, hasCalendarPermission) {
        if (availableCalendars.isNotEmpty()) {
            if (selectedCalendarId == 0L || availableCalendars.none { it.id == selectedCalendarId }) {
                val best = availableCalendars.find { it.accountType.contains("google", ignoreCase = true) }
                    ?: availableCalendars.find { it.isPrimary }
                    ?: availableCalendars.first()
                selectedCalendarId = best.id
            }
            if (syncDestination == "ICS_ONLY" && hasCalendarPermission) {
                syncDestination = "EXISTING_CAL"
            }
        }
    }

    // Synchronize conversions
    val currentHebrewDateInfo by remember {
        derivedStateOf {
            if (!isHebrewInputMode) {
                HebrewCalendarEngine.fromGregorian(gYear, gMonth, gDay)
            } else {
                HebrewCalendarEngine.fromHebrew(hYear, hMonth, hDay)
            }
        }
    }

    // Calculated preview occurrences (first 5 of 100)
    val previewOccurrences by remember {
        derivedStateOf {
            if (recurrenceType == "MONTHLY") {
                HebrewCalendarEngine.calculateMonthlyOccurrences(
                    originHebrewDay = currentHebrewDateInfo.hebrewDay,
                    monthsCount = 5
                )
            } else {
                HebrewCalendarEngine.calculateYearlyOccurrences(
                    originHebrewYear = currentHebrewDateInfo.hebrewYear,
                    originHebrewMonth = currentHebrewDateInfo.hebrewMonth,
                    originHebrewDay = currentHebrewDateInfo.hebrewDay,
                    leapYearRule = leapYearRule,
                    yearsCount = 5,
                    startFromCurrentYear = true
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 750.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.addEventTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = strings.cancel)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 12.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Staged / Batch Events summary card if user added events to the batch
                    if (stagedEvents.isNotEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (strings.isHe) "אירועים שממתינים לסנכרון (${stagedEvents.size})" else "Events queued for sync (${stagedEvents.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        TextButton(onClick = { stagedEvents.clear() }) {
                                            Text(if (strings.isHe) "נקה הכל" else "Clear all", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    stagedEvents.forEachIndexed { idx, draft ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${idx + 1}. ${draft.title}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "${draft.hebrewDateInfo.formattedHe} • ${draft.yearsCount} ${if (strings.isHe) "שנים" else "years"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { stagedEvents.removeAt(idx) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Remove",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        if (idx < stagedEvents.lastIndex) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Event Title Field
                    item {
                        Column {
                            OutlinedTextField(
                                value = eventTitle,
                                onValueChange = {
                                    eventTitle = it
                                    if (it.isNotBlank()) titleError = null
                                },
                                label = { Text(strings.eventNameLabel) },
                                placeholder = { Text(strings.eventNamePlaceholder) },
                                isError = titleError != null,
                                supportingText = titleError?.let { { Text(it) } },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("event_name_input"),
                                shape = RoundedCornerShape(16.dp)
                            )

                            // Quick Title Suggestions
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val suggestions = if (strings.isHe) {
                                    listOf("יום הולדת (עברי)", "יום הולדת דרור (עברי)", "אזכרה (יארצייט)", "יום נישואין עברי")
                                } else {
                                    listOf("Hebrew Birthday", "Dror's Birthday (Hebrew)", "Yahrzeit", "Hebrew Anniversary")
                                }
                                suggestions.forEach { s ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.clickable { eventTitle = s }
                                    ) {
                                        Text(
                                            text = s,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Event Type Selector
                    item {
                        Text(
                            text = strings.eventTypeLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = eventType == "BIRTHDAY",
                                onClick = { eventType = "BIRTHDAY" },
                                shape = RoundedCornerShape(50),
                                label = { Text(strings.typeBirthday, maxLines = 1) },
                                leadingIcon = {
                                    Icon(
                                        if (eventType == "BIRTHDAY") Icons.Default.Check else Icons.Default.Cake,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = eventType == "YAHRZEIT",
                                onClick = { eventType = "YAHRZEIT" },
                                shape = RoundedCornerShape(50),
                                label = { Text(strings.typeYahrzeit, maxLines = 1) },
                                leadingIcon = {
                                    Icon(
                                        if (eventType == "YAHRZEIT") Icons.Default.Check else Icons.Default.Whatshot,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = eventType == "ANNIVERSARY",
                                onClick = { eventType = "ANNIVERSARY" },
                                shape = RoundedCornerShape(50),
                                label = { Text(strings.typeAnniversary, maxLines = 1) },
                                leadingIcon = {
                                    Icon(
                                        if (eventType == "ANNIVERSARY") Icons.Default.Check else Icons.Default.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1.1f)
                            )
                        }
                    }

                    // Date Input Mode (Gregorian vs Hebrew)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = strings.dateInputMode,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = !isHebrewInputMode,
                                        onClick = { isHebrewInputMode = false },
                                        shape = RoundedCornerShape(50),
                                        label = { Text(strings.modeGregorian) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = isHebrewInputMode,
                                        onClick = { isHebrewInputMode = true },
                                        shape = RoundedCornerShape(50),
                                        label = { Text(strings.modeHebrew) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (!isHebrewInputMode) {
                                    // Gregorian Pickers (Day, Month, Year)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Day Picker
                                        NumberWheelPicker(
                                            label = strings.dayLabel,
                                            value = gDay,
                                            range = 1..31,
                                            onValueChange = { gDay = it },
                                            modifier = Modifier.weight(1f)
                                        )
                                        // Month Picker
                                        NumberWheelPicker(
                                            label = strings.monthLabel,
                                            value = gMonth,
                                            range = 1..12,
                                            onValueChange = { gMonth = it },
                                            modifier = Modifier.weight(1f)
                                        )
                                        // Year Picker
                                        NumberWheelPicker(
                                            label = strings.yearLabel,
                                            value = gYear,
                                            range = 1920..2040,
                                            onValueChange = { gYear = it },
                                            modifier = Modifier.weight(1.3f)
                                        )
                                    }
                                } else {
                                    // Hebrew Pickers (Day, Month, Year)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Hebrew Day Picker (with Hebrew letters)
                                        HebrewDayPicker(
                                            label = strings.dayLabel,
                                            selectedDay = hDay,
                                            onDaySelected = { hDay = it },
                                            modifier = Modifier.weight(1f)
                                        )
                                        // Hebrew Month Picker
                                        HebrewMonthPicker(
                                            label = strings.monthLabel,
                                            selectedMonth = hMonth,
                                            onMonthSelected = { hMonth = it },
                                            isLeap = HebrewCalendarEngine.isLeapYear(hYear),
                                            isHe = strings.isHe,
                                            modifier = Modifier.weight(1.5f)
                                        )
                                        // Hebrew Year Picker
                                        HebrewYearPicker(
                                            label = strings.yearLabel,
                                            selectedYear = hYear,
                                            onYearSelected = { hYear = it },
                                            modifier = Modifier.weight(1.2f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                 // Dynamic Calculated Equivalent Card
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = if (!isHebrewInputMode) strings.convertedEquivalent else strings.gregorianEquivalent,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (!isHebrewInputMode) currentHebrewDateInfo.formattedHe else "${currentHebrewDateInfo.gregorianDay}/${currentHebrewDateInfo.gregorianMonth}/${currentHebrewDateInfo.gregorianYear}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (currentHebrewDateInfo.isLeapYear) {
                                            Text(
                                                text = if (strings.isHe) "שנת מעוברת (13 חודשים)" else "Leap Year (13 months)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Recurrence Type
                    item {
                        Column {
                            Text(
                                text = strings.recurrenceLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { recurrenceType = "YEARLY" },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = recurrenceType == "YEARLY",
                                    onClick = { recurrenceType = "YEARLY" }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.recurYearly,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (recurrenceType == "YEARLY") FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { recurrenceType = "MONTHLY" },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = recurrenceType == "MONTHLY",
                                    onClick = { recurrenceType = "MONTHLY" }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.recurMonthly,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (recurrenceType == "MONTHLY") FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Halachic Leap Year Rules (for Yearly recurrence)
                    if (recurrenceType == "YEARLY") {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.leapYearRuleTitle,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        IconButton(onClick = { showHalachicInfo = !showHalachicInfo }) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.HelpOutline,
                                                contentDescription = "Halachic Info",
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    AnimatedVisibility(visible = showHalachicInfo) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = strings.leapYearHalachicNote,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }

                                    // Rules Radio Group
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { leapYearRule = LeapYearRule.STANDARD_ADAR_II },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = leapYearRule == LeapYearRule.STANDARD_ADAR_II,
                                            onClick = { leapYearRule = LeapYearRule.STANDARD_ADAR_II }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = strings.ruleStandardAdarII,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { leapYearRule = LeapYearRule.ADAR_I },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = leapYearRule == LeapYearRule.ADAR_I,
                                            onClick = { leapYearRule = LeapYearRule.ADAR_I }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = strings.ruleAdarI,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { leapYearRule = LeapYearRule.BOTH },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = leapYearRule == LeapYearRule.BOTH,
                                            onClick = { leapYearRule = LeapYearRule.BOTH }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = strings.ruleBoth,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Duration (How many years / occurrences)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (strings.isHe) "משך חזרתיות (שנים)" else "Duration (Years)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "$yearsDuration ${if (strings.isHe) "שנים קדימה" else "years ahead"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(10, 20, 50, 100).forEach { yrs ->
                                        FilterChip(
                                            selected = yearsDuration == yrs,
                                            onClick = { yearsDuration = yrs },
                                            label = {
                                                Text(
                                                    text = "$yrs ${if (strings.isHe) "שנים" else "yrs"}",
                                                    fontWeight = if (yearsDuration == yrs) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sync Destination Options
                    item {
                        Column {
                            Text(
                                text = strings.destinationLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Option 1: Existing Calendar (Recommended for Google Calendar)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { syncDestination = "EXISTING_CAL" },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = syncDestination == "EXISTING_CAL",
                                    onClick = { syncDestination = "EXISTING_CAL" }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(strings.destDeviceCalendar, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (strings.isHe) "מומלץ עבור סנכרון ל-Google Calendar" else "Recommended for Google Calendar sync",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (syncDestination == "EXISTING_CAL") {
                                if (!hasCalendarPermission) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 32.dp, top = 4.dp, bottom = 8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = strings.permissionRequiredMsg,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = onRequestCalendarPermission,
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier.testTag("grant_permission_button")
                                            ) {
                                                Text(strings.grantPermissionBtn)
                                            }
                                        }
                                    }
                                } else if (availableCalendars.isNotEmpty()) {
                                    CalendarDropdownPicker(
                                        calendars = availableCalendars,
                                        selectedCalendarId = selectedCalendarId,
                                        onSelect = { selectedCalendarId = it },
                                        modifier = Modifier.padding(start = 32.dp, top = 4.dp)
                                    )
                                } else {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.padding(start = 32.dp, top = 4.dp, end = 8.dp)
                                    ) {
                                        Text(
                                            text = if (strings.isHe) "טוען יומנים זמינים..." else "Loading available calendars...",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }

                            // Option 2: Create new calendar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { syncDestination = "NEW_CAL" },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = syncDestination == "NEW_CAL",
                                    onClick = { syncDestination = "NEW_CAL" }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(strings.destNewCalendar, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = if (strings.isHe) "יומן מקומי נפרד במכשיר (לא יומן ענן של גוגל)" else "Local device calendar (separate from Google cloud)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (syncDestination == "NEW_CAL") {
                                if (!hasCalendarPermission) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 32.dp, top = 4.dp, bottom = 8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = strings.permissionRequiredMsg,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = onRequestCalendarPermission,
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier.testTag("grant_permission_new_cal_button")
                                            ) {
                                                Text(strings.grantPermissionBtn)
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = newCalendarName,
                                    onValueChange = { newCalendarName = it },
                                    label = { Text(strings.destNewCalendar) },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 32.dp, top = 4.dp)
                                )
                                Text(
                                    text = if (strings.isHe) "שימו לב: יצירת יומן חדש יוצרת יומן במכשיר. לסנכרון שמופיע בכל המכשירים בחשבון גוגל שלכם, בחרו באפשרות הראשונה (סנכרון ישיר ליומן Google)." else "Note: creating a new calendar creates a local calendar on this device. To sync with your Google account across devices, choose the first option above.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 32.dp, top = 4.dp, end = 8.dp)
                                )
                            }

                            // Option 3: Export ICS only
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { syncDestination = "ICS_ONLY" },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = syncDestination == "ICS_ONLY",
                                    onClick = { syncDestination = "ICS_ONLY" }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.destIcsOnly, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // Live Preview of Occurrences
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = strings.previewOccurrencesTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                previewOccurrences.forEach { occ ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${occ.occurrenceIndex}. ${occ.gregorianDateFormatted}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = occ.hebrewDateFormatted,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            if (occ.note != null) {
                                                Text(
                                                    text = occ.note,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Secondary action: "Add another event" to queue in batch
                    OutlinedButton(
                        onClick = {
                            if (eventTitle.isBlank()) {
                                titleError = strings.fillTitleError
                                return@OutlinedButton
                            }
                            stagedEvents.add(
                                HebrewCalendarViewModel.EventDraft(
                                    title = eventTitle.trim(),
                                    eventType = eventType,
                                    recurrenceType = recurrenceType,
                                    hebrewDateInfo = currentHebrewDateInfo,
                                    leapYearRule = leapYearRule,
                                    yearsCount = yearsDuration
                                )
                            )
                            // Reset title and error for the next event
                            eventTitle = ""
                            titleError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (strings.isHe) "הוסף עוד אירוע לסנכרון יחד" else "Add another event to sync together",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (creationErrorFeedback != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = creationErrorFeedback ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(strings.cancel)
                        }

                        Button(
                            onClick = {
                                // Clear previous feedback
                                creationErrorFeedback = null

                                // Gather all events: queued items plus current form if title is filled
                                val allEventsToSave = stagedEvents.toMutableList()
                                if (eventTitle.isNotBlank()) {
                                    allEventsToSave.add(
                                        HebrewCalendarViewModel.EventDraft(
                                            title = eventTitle.trim(),
                                            eventType = eventType,
                                            recurrenceType = recurrenceType,
                                            hebrewDateInfo = currentHebrewDateInfo,
                                            leapYearRule = leapYearRule,
                                            yearsCount = yearsDuration
                                        )
                                    )
                                }

                                if (allEventsToSave.isEmpty()) {
                                    titleError = strings.fillTitleError
                                    return@Button
                                }

                                // Check permission if syncing to a calendar
                                if (syncDestination != "ICS_ONLY" && !hasCalendarPermission) {
                                    onRequestCalendarPermission()
                                    return@Button
                                }

                                if (syncDestination == "NEW_CAL") {
                                    coroutineScope.launch {
                                        isCreatingCal = true
                                        val newId = onCreateNewCalendar(newCalendarName)
                                        isCreatingCal = false
                                        if (newId != null) {
                                            onSaveBatch(
                                                allEventsToSave,
                                                newId,
                                                newCalendarName,
                                                false
                                            )
                                        } else {
                                            // Fallback: If device policy restricts creating a local calendar,
                                            // automatically sync to existing Google/Device calendar or export ICS!
                                            val fallbackCal = availableCalendars.find { it.accountType.contains("google", ignoreCase = true) }
                                                ?: availableCalendars.find { it.isPrimary }
                                                ?: availableCalendars.firstOrNull()

                                            if (fallbackCal != null) {
                                                creationErrorFeedback = strings.calendarCreateErrorFallback
                                                selectedCalendarId = fallbackCal.id
                                                onSaveBatch(
                                                    allEventsToSave,
                                                    fallbackCal.id,
                                                    fallbackCal.displayName,
                                                    false
                                                )
                                            } else {
                                                creationErrorFeedback = strings.calendarCreateFailedMsg
                                                onSaveBatch(
                                                    allEventsToSave,
                                                    null,
                                                    null,
                                                    true
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    val isIcs = syncDestination == "ICS_ONLY"
                                    val calId = if (!isIcs) selectedCalendarId else null
                                    val calName = availableCalendars.find { it.id == calId }?.displayName
                                    onSaveBatch(
                                        allEventsToSave,
                                        calId,
                                        calName,
                                        isIcs
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1.8f)
                                .testTag("save_sync_button"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isCreatingCal) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                val count = stagedEvents.size + (if (eventTitle.isNotBlank()) 1 else 0)
                                val syncText = if (count > 1) {
                                    if (syncDestination == "ICS_ONLY") {
                                        if (strings.isHe) "ייצא $count אירועים (ICS)" else "Export $count Events (ICS)"
                                    } else {
                                        if (strings.isHe) "סנכרן $count אירועים ליומן" else "Sync $count Events to Calendar"
                                    }
                                } else {
                                    if (syncDestination == "ICS_ONLY") strings.saveAndExportIcsBtn else strings.saveAndSyncBtn
                                }
                                Text(
                                    text = syncText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NumberWheelPicker(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }

        if (expanded) {
            AlertDialog(
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(label) },
                text = {
                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items(range.toList()) { num ->
                            Text(
                                text = num.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (num == value) FontWeight.Bold else FontWeight.Normal,
                                color = if (num == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onValueChange(num)
                                        expanded = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { expanded = false }, shape = RoundedCornerShape(50)) { Text("OK") }
                }
            )
        }
    }
}

@Composable
fun HebrewDayPicker(
    label: String,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Text(
                text = HebrewCalendarEngine.formatHebrewNumber(selectedDay),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }

        if (expanded) {
            AlertDialog(
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(label) },
                text = {
                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items((1..30).toList()) { d ->
                            val hebrewStr = HebrewCalendarEngine.formatHebrewNumber(d)
                            Text(
                                text = "$hebrewStr ($d)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (d == selectedDay) FontWeight.Bold else FontWeight.Normal,
                                color = if (d == selectedDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDaySelected(d)
                                        expanded = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { expanded = false }, shape = RoundedCornerShape(50)) { Text("OK") }
                }
            )
        }
    }
}

@Composable
fun HebrewMonthPicker(
    label: String,
    selectedMonth: Int,
    onMonthSelected: (Int) -> Unit,
    isLeap: Boolean,
    isHe: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val months = remember(isLeap) {
        if (isLeap) {
            listOf(
                JewishDate.TISHREI, JewishDate.CHESHVAN, JewishDate.KISLEV, JewishDate.TEVES,
                JewishDate.SHEVAT, JewishDate.ADAR, JewishDate.ADAR_II, JewishDate.NISSAN,
                JewishDate.IYAR, JewishDate.SIVAN, JewishDate.TAMMUZ, JewishDate.AV, JewishDate.ELUL
            )
        } else {
            listOf(
                JewishDate.TISHREI, JewishDate.CHESHVAN, JewishDate.KISLEV, JewishDate.TEVES,
                JewishDate.SHEVAT, JewishDate.ADAR, JewishDate.NISSAN, JewishDate.IYAR,
                JewishDate.SIVAN, JewishDate.TAMMUZ, JewishDate.AV, JewishDate.ELUL
            )
        }
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Text(
                text = HebrewCalendarEngine.getHebrewMonthName(selectedMonth, isLeap, isHe),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }

        if (expanded) {
            AlertDialog(
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(label) },
                text = {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(months) { m ->
                            val mName = HebrewCalendarEngine.getHebrewMonthName(m, isLeap, isHe)
                            Text(
                                text = mName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (m == selectedMonth) FontWeight.Bold else FontWeight.Normal,
                                color = if (m == selectedMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onMonthSelected(m)
                                        expanded = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { expanded = false }, shape = RoundedCornerShape(50)) { Text("OK") }
                }
            )
        }
    }
}

@Composable
fun HebrewYearPicker(
    label: String,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val years = remember { (5700..5850).toList() }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Text(
                text = "${HebrewCalendarEngine.formatHebrewNumber(selectedYear)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }

        if (expanded) {
            AlertDialog(
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(label) },
                text = {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(years) { y ->
                            val yName = HebrewCalendarEngine.formatHebrewNumber(y)
                            Text(
                                text = "$yName ($y)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (y == selectedYear) FontWeight.Bold else FontWeight.Normal,
                                color = if (y == selectedYear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onYearSelected(y)
                                        expanded = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { expanded = false }, shape = RoundedCornerShape(50)) { Text("OK") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDropdownPicker(
    calendars: List<DeviceCalendarInfo>,
    selectedCalendarId: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCal = calendars.find { it.id == selectedCalendarId } ?: calendars.firstOrNull()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCal?.let { "${it.displayName} (${it.accountName})" } ?: "Select Calendar",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            calendars.forEach { cal ->
                val isGoogle = cal.accountType.contains("google", ignoreCase = true)
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cal.displayName, fontWeight = FontWeight.Bold)
                                if (cal.accountName.isNotBlank()) {
                                    Text(cal.accountName, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (isGoogle) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Google",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        onSelect(cal.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

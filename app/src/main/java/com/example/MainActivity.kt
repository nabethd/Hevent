package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.HebrewDateInfo
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.screens.AddEditEventDialog
import com.example.ui.screens.CalendarViewScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.HebrewCalendarViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HebrewCalendarApp()
            }
        }
    }
}

@Composable
fun HebrewCalendarApp(
    viewModel: HebrewCalendarViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentLanguage by viewModel.language.collectAsStateWithLifecycle()
    val strings = remember(currentLanguage) { AppStrings(currentLanguage) }

    val events by viewModel.events.collectAsStateWithLifecycle()
    val calendars by viewModel.calendars.collectAsStateWithLifecycle()
    val hasCalendarPermission by viewModel.hasCalendarPermission.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    // Navigation tab: 0 = Events, 1 = Calendar, 2 = Settings
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }

    // Calendar View states
    val calViewYear by viewModel.calViewYear.collectAsStateWithLifecycle()
    val calViewMonth by viewModel.calViewMonth.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()

    // Permission launcher for Android Calendar
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[android.Manifest.permission.READ_CALENDAR] ?: false
        val writeGranted = permissions[android.Manifest.permission.WRITE_CALENDAR] ?: false
        if (readGranted && writeGranted) {
            viewModel.refreshCalendarPermissions()
            Toast.makeText(
                context,
                if (currentLanguage == AppLanguage.HEBREW) "הרשאות יומן ניתנו בהצלחה" else "Calendar permissions granted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Wrap whole UI in dynamic RTL / LTR based on language without app restart!
    val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = strings.appName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val newLang = if (currentLanguage == AppLanguage.HEBREW) AppLanguage.ENGLISH else AppLanguage.HEBREW
                                        viewModel.setLanguage(newLang)
                                    },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = if (currentLanguage == AppLanguage.HEBREW) "EN" else "עב",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                IconButton(
                                    onClick = { selectedTab = 2 },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = strings.navSettings,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            bottomBar = {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = strings.navEvents) },
                            label = { Text(strings.navEvents, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_events")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = strings.navCalendar) },
                            label = { Text(strings.navCalendar, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_calendar")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.Settings, contentDescription = strings.navSettings) },
                            label = { Text(strings.navSettings, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        strings = strings,
                        events = events,
                        onAddEventClick = { showAddDialog = true },
                        onDeleteEvent = { event -> viewModel.deleteEvent(event) },
                        onExportIcs = { event ->
                            val intent = viewModel.exportEventToIcs(event)
                            context.startActivity(Intent.createChooser(intent, strings.exportIcs))
                        }
                    )

                    1 -> CalendarViewScreen(
                        strings = strings,
                        year = calViewYear,
                        month = calViewMonth,
                        selectedDay = selectedDay,
                        events = events,
                        onPrevMonth = { viewModel.prevCalendarMonth() },
                        onNextMonth = { viewModel.nextCalendarMonth() },
                        onTodayClick = {
                            val now = java.util.Calendar.getInstance()
                            viewModel.setCalendarMonth(now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH) + 1)
                            viewModel.setSelectedDay(now.get(java.util.Calendar.DAY_OF_MONTH))
                        },
                        onDaySelect = { day -> viewModel.setSelectedDay(day) },
                        onAddEventForDate = { dateInfo ->
                            showAddDialog = true
                        }
                    )

                    2 -> SettingsScreen(
                        strings = strings,
                        currentLanguage = currentLanguage,
                        onLanguageChange = { newLang -> viewModel.setLanguage(newLang) },
                        events = events,
                        availableCalendars = calendars,
                        hasCalendarPermission = hasCalendarPermission,
                        onRequestCalendarPermission = {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.READ_CALENDAR,
                                    android.Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        },
                        onCreateNewCalendar = { name ->
                            viewModel.createNewHebrewCalendar(name)
                        },
                        onDeleteByName = { name ->
                            viewModel.deleteEventsByName(name) { count ->
                                Toast.makeText(
                                    context,
                                    "$count ${strings.eventsDeletedFromCal}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onExportAllIcs = {
                            val intent = viewModel.exportAllEventsToIcs(events)
                            context.startActivity(Intent.createChooser(intent, strings.exportAllIcsTitle))
                        }
                    )
                }

                // Add / Edit Event Dialog
                if (showAddDialog) {
                    AddEditEventDialog(
                        strings = strings,
                        availableCalendars = calendars,
                        hasCalendarPermission = hasCalendarPermission,
                        onRequestCalendarPermission = {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.READ_CALENDAR,
                                    android.Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        },
                        onCreateNewCalendar = { name ->
                            viewModel.createNewHebrewCalendar(name)
                        },
                        onDismiss = { showAddDialog = false },
                        onSaveBatch = { drafts, targetCalendarId, targetCalendarName, isIcsOnly ->
                            viewModel.saveBatchEvents(
                                events = drafts,
                                targetCalendarId = targetCalendarId,
                                targetCalendarName = targetCalendarName,
                                isIcsOnly = isIcsOnly,
                                onComplete = { success, shareIntent ->
                                    showAddDialog = false
                                    if (shareIntent != null) {
                                        context.startActivity(Intent.createChooser(shareIntent, strings.exportIcs))
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

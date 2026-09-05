package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.screens.AddEditEventDialog
import com.example.ui.screens.CalendarViewScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.HebrewCalendarViewModel
import com.example.ui.viewmodel.UiEvent

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

private const val TAB_EVENTS = 0
private const val TAB_CALENDAR = 1
private const val TAB_SETTINGS = 2

@Composable
fun HebrewCalendarApp(
    viewModel: HebrewCalendarViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val currentLanguage by viewModel.language.collectAsStateWithLifecycle()
    val strings = remember(currentLanguage) { AppStrings(currentLanguage) }

    val events by viewModel.events.collectAsStateWithLifecycle()
    val calendars by viewModel.calendars.collectAsStateWithLifecycle()
    val hasCalendarPermission by viewModel.hasCalendarPermission.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val stagedEvents by viewModel.stagedEvents.collectAsStateWithLifecycle()
    val prefillDate by viewModel.prefillDate.collectAsStateWithLifecycle()

    val calViewYear by viewModel.calViewYear.collectAsStateWithLifecycle()
    val calViewMonth by viewModel.calViewMonth.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()

    // rememberSaveable, so a rotation does not reset the tab or discard a half-filled dialog.
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_EVENTS) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    // Id rather than the entity, so it survives process death and always resolves to fresh data.
    var editingEventId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingEvent = remember(editingEventId, events) {
        editingEventId?.let { id -> events.firstOrNull { it.id == id } }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshCalendarPermissions()
    }

    fun requestCalendarPermission() {
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.READ_CALENDAR,
                android.Manifest.permission.WRITE_CALENDAR
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is UiEvent.Share ->
                    context.startActivity(Intent.createChooser(event.intent, event.chooserTitle))
                UiEvent.Saved -> {
                    showAddDialog = false
                    editingEventId = null
                }
            }
        }
    }

    // Opening the dialog from a tapped calendar day.
    LaunchedEffect(prefillDate) {
        if (prefillDate != null) showAddDialog = true
    }

    // The tabs are not a navigation graph, so Back has to be handled explicitly; otherwise it
    // closed the app from any tab instead of returning to the first one.
    BackHandler(enabled = selectedTab != TAB_EVENTS) { selectedTab = TAB_EVENTS }

    val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
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

                            // The settings icon that used to sit here duplicated the bottom tab.
                            OutlinedButton(
                                onClick = {
                                    viewModel.setLanguage(
                                        if (currentLanguage == AppLanguage.HEBREW) AppLanguage.ENGLISH
                                        else AppLanguage.HEBREW
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.HEBREW) "EN" else "עב",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
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
                        val tabs = listOf(
                            Triple(TAB_EVENTS, Icons.AutoMirrored.Filled.EventNote, strings.navEvents),
                            Triple(TAB_CALENDAR, Icons.Default.CalendarMonth, strings.navCalendar),
                            Triple(TAB_SETTINGS, Icons.Default.Settings, strings.navSettings)
                        )
                        val tags = listOf("nav_events", "nav_calendar", "nav_settings")
                        tabs.forEachIndexed { index, (tab, icon, label) ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(icon, contentDescription = null) },
                                label = {
                                    Text(
                                        label,
                                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tags[index])
                            )
                        }
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
                    TAB_EVENTS -> HomeScreen(
                        strings = strings,
                        events = events,
                        onAddEventClick = { showAddDialog = true },
                        onDeleteEvent = viewModel::deleteEvent,
                        onEditEvent = { event ->
                            editingEventId = event.id
                            showAddDialog = true
                        },
                        onExportIcs = { event ->
                            viewModel.exportEventsToIcs(listOf(event), singleTitle = event.title)
                        }
                    )

                    TAB_CALENDAR -> CalendarViewScreen(
                        strings = strings,
                        year = calViewYear,
                        month = calViewMonth,
                        selectedDay = selectedDay,
                        events = events,
                        onPrevMonth = viewModel::prevCalendarMonth,
                        onNextMonth = viewModel::nextCalendarMonth,
                        onTodayClick = {
                            val now = java.util.Calendar.getInstance()
                            viewModel.setCalendarMonth(
                                now.get(java.util.Calendar.YEAR),
                                now.get(java.util.Calendar.MONTH) + 1
                            )
                            viewModel.setSelectedDay(now.get(java.util.Calendar.DAY_OF_MONTH))
                        },
                        onDaySelect = viewModel::setSelectedDay,
                        // The tapped date is now carried into the dialog instead of being dropped.
                        onAddEventForDate = viewModel::requestAddEventForDate
                    )

                    TAB_SETTINGS -> SettingsScreen(
                        strings = strings,
                        currentLanguage = currentLanguage,
                        onLanguageChange = viewModel::setLanguage,
                        events = events,
                        availableCalendars = calendars,
                        hasCalendarPermission = hasCalendarPermission,
                        onRequestCalendarPermission = ::requestCalendarPermission,
                        onCreateNewCalendar = viewModel::createNewHebrewCalendar,
                        onDeleteByName = viewModel::deleteEventsByName,
                        onExportAllIcs = { viewModel.exportEventsToIcs(events) }
                    )
                }

                if (showAddDialog) {
                    // Keyed so switching between add and edit rebuilds the form state instead of
                    // reusing the previous row's remembered values.
                    key(editingEventId) {
                    AddEditEventDialog(
                        strings = strings,
                        availableCalendars = calendars,
                        hasCalendarPermission = hasCalendarPermission,
                        isSyncing = isSyncing,
                        stagedEvents = stagedEvents,
                        prefillDate = prefillDate,
                        editingEvent = editingEvent,
                        onRequestCalendarPermission = ::requestCalendarPermission,
                        onCreateNewCalendar = viewModel::createNewHebrewCalendar,
                        onStageEvent = viewModel::stageEvent,
                        onUnstageEvent = viewModel::unstageEvent,
                        onClearStaged = viewModel::clearStagedEvents,
                        onDismiss = {
                            showAddDialog = false
                            editingEventId = null
                            viewModel.consumePrefillDate()
                        },
                        onSave = { draft, calendarId, calendarName, isIcsOnly ->
                            val original = editingEvent
                            if (original != null && draft != null) {
                                viewModel.updateEvent(original, draft, calendarId, calendarName, isIcsOnly)
                            } else {
                                viewModel.saveEvents(draft, calendarId, calendarName, isIcsOnly)
                            }
                        }
                    )
                    }
                }
            }
        }
    }
}

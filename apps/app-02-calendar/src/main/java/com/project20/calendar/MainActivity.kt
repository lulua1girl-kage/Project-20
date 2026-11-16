package com.project20.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val PREFS = "project20_calendar"
private const val EVENTS_KEY = "events"

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startAt: Long,
    val endAt: Long,
    val allDay: Boolean,
    val location: String = "",
    val notes: String = "",
    val reminderMinutes: Int = 10
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
        createCalendarNotificationChannel(this)
        loadEvents(this).forEach { scheduleEventReminder(this, it) }
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CalendarHome(
                        applicationContext,
                        onEnableNotifications = {
                            if (Build.VERSION.SDK_INT >= 33) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }
        }
    }
}

internal fun loadEvents(context: Context): List<CalendarEvent> {
    val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(EVENTS_KEY, null) ?: return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    CalendarEvent(
                        id = item.getLong("id"),
                        title = item.getString("title"),
                        startAt = item.getLong("startAt"),
                        endAt = item.getLong("endAt"),
                        allDay = item.optBoolean("allDay", false),
                        location = item.optString("location"),
                        notes = item.optString("notes"),
                        reminderMinutes = item.optInt("reminderMinutes", 10)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun saveEvents(context: Context, events: List<CalendarEvent>) {
    val array = JSONArray()
    events.forEach { event ->
        array.put(
            JSONObject()
                .put("id", event.id)
                .put("title", event.title)
                .put("startAt", event.startAt)
                .put("endAt", event.endAt)
                .put("allDay", event.allDay)
                .put("location", event.location)
                .put("notes", event.notes)
                .put("reminderMinutes", event.reminderMinutes)
        )
    }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putString(EVENTS_KEY, array.toString()).apply()
}

private fun dayStart(time: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = time }
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private fun sameDay(a: Long, b: Long): Boolean = dayStart(a) == dayStart(b)

private fun monthStart(time: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = time }
    c.set(Calendar.DAY_OF_MONTH, 1)
    return dayStart(c.timeInMillis)
}

private fun monthLabel(time: Long): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(time))

private fun dateLabel(time: Long): String =
    SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(time))

private fun timeLabel(time: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))

@Composable
private fun CalendarHome(context: Context, onEnableNotifications: () -> Unit) {
    val events = remember {
        mutableStateListOf<CalendarEvent>().apply { addAll(loadEvents(context)) }
    }
    var selectedDay by remember { mutableStateOf(dayStart(System.currentTimeMillis())) }
    var displayedMonth by remember { mutableStateOf(monthStart(selectedDay)) }
    var activeView by remember { mutableStateOf("Month") }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val dayEvents = events.filter { sameDay(it.startAt, selectedDay) }.sortedBy { it.startAt }
    val today = dayStart(System.currentTimeMillis())

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Calendar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Your schedule, in one place", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = {
                selectedDay = today
                displayedMonth = monthStart(today)
            }) { Text("Today") }
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            TextButton(onClick = onEnableNotifications) { Text("Enable event reminders") }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Month", "Agenda").forEach { view ->
                AssistChip(
                    onClick = { activeView = view },
                    label = { Text(view) },
                    leadingIcon = if (activeView == view) {
                        { Text("✓") }
                    } else null
                )
            }
        }

        if (activeView == "Month") {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(monthLabel(displayedMonth), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row {
                    TextButton(onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = displayedMonth; add(Calendar.MONTH, -1) }
                        displayedMonth = monthStart(c.timeInMillis)
                    }) { Text("‹") }
                    TextButton(onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = displayedMonth; add(Calendar.MONTH, 1) }
                        displayedMonth = monthStart(c.timeInMillis)
                    }) { Text("›") }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                    Text(it, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val first = Calendar.getInstance().apply { timeInMillis = displayedMonth }
            val offset = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
            val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
            val cells = ((offset + daysInMonth + 6) / 7) * 7
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth().height(264.dp),
                userScrollEnabled = false
            ) {
                items(cells) { index ->
                    val dayNumber = index - offset + 1
                    if (dayNumber in 1..daysInMonth) {
                        val cell = Calendar.getInstance().apply {
                            timeInMillis = displayedMonth
                            set(Calendar.DAY_OF_MONTH, dayNumber)
                        }.timeInMillis
                        val selected = sameDay(cell, selectedDay)
                        val isToday = sameDay(cell, today)
                        val hasEvents = events.any { sameDay(it.startAt, cell) }
                        Column(
                            modifier = Modifier
                                .padding(2.dp)
                                .size(42.dp)
                                .background(
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    shape = CircleShape
                                )
                                .clickable { selectedDay = dayStart(cell) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                dayNumber.toString(),
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isToday || selected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (hasEvents) {
                                Box(Modifier.padding(top = 1.dp).size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            } else {
                                Spacer(Modifier.size(4.dp))
                            }
                        }
                    } else {
                        Spacer(Modifier.size(46.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(dateLabel(selectedDay), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        if (activeView == "Agenda") {
            val ordered = events.sortedBy { it.startAt }
            if (ordered.isEmpty()) {
                EmptySchedule("No events yet", "Add an event to start building your calendar.")
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ordered, key = { it.id }) { event ->
                        EventCard(event, onClick = {
                            editingEvent = event
                            showEditor = true
                        })
                    }
                }
            }
        } else {
            if (dayEvents.isEmpty()) {
                EmptySchedule("Nothing scheduled", "Your events for this day will appear here.")
                Spacer(Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dayEvents, key = { it.id }) { event ->
                        EventCard(event, onClick = {
                            editingEvent = event
                            showEditor = true
                        })
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingEvent = null
                showEditor = true
            },
            modifier = Modifier.align(Alignment.End)
        ) { Text("+", style = MaterialTheme.typography.headlineSmall) }
    }

    if (showEditor) {
        EventEditor(
            initial = editingEvent,
            selectedDay = selectedDay,
            onDismiss = { showEditor = false },
            onDelete = { event ->
                events.removeAll { it.id == event.id }
                cancelEventReminder(context, event.id)
                saveEvents(context, events)
                showEditor = false
            },
            onSave = { event ->
                val index = events.indexOfFirst { it.id == event.id }
                if (index >= 0) events[index] = event else events.add(event)
                events.sortBy { it.startAt }
                saveEvents(context, events)
                scheduleEventReminder(context, event)
                selectedDay = dayStart(event.startAt)
                displayedMonth = monthStart(selectedDay)
                showEditor = false
            }
        )
    }
}

@Composable
private fun EmptySchedule(title: String, message: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EventCard(event: CalendarEvent, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(4.dp, 42.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(event.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    if (event.allDay) "All day" else "${timeLabel(event.startAt)} – ${timeLabel(event.endAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.location.isNotBlank()) {
                    Text(event.location, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EventEditor(
    initial: CalendarEvent?,
    selectedDay: Long,
    onDismiss: () -> Unit,
    onDelete: (CalendarEvent) -> Unit,
    onSave: (CalendarEvent) -> Unit
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var location by remember(initial?.id) { mutableStateOf(initial?.location ?: "") }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes ?: "") }
    var startTime by remember(initial?.id) { mutableStateOf(initial?.let { timeLabel(it.startAt) } ?: "09:00") }
    var endTime by remember(initial?.id) { mutableStateOf(initial?.let { timeLabel(it.endAt) } ?: "10:00") }
    var allDay by remember(initial?.id) { mutableStateOf(initial?.allDay ?: false) }
    var reminderMinutes by remember(initial?.id) { mutableStateOf((initial?.reminderMinutes ?: 10).toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New event" else "Edit event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it; error = null }, label = { Text("Event title") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(startTime, { startTime = it }, label = { Text("Start (HH:mm)") }, modifier = Modifier.weight(1f), singleLine = true, enabled = !allDay)
                    OutlinedTextField(endTime, { endTime = it }, label = { Text("End (HH:mm)") }, modifier = Modifier.weight(1f), singleLine = true, enabled = !allDay)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(checked = allDay, onCheckedChange = { allDay = it })
                    Text("All day")
                }
                OutlinedTextField(location, { location = it }, label = { Text("Location (optional)") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, minLines = 2)
                OutlinedTextField(
                    value = reminderMinutes,
                    onValueChange = { reminderMinutes = it.filter { ch -> ch.isDigit() || ch == '-' }.take(6) },
                    label = { Text("Reminder minutes before") },
                    supportingText = { Text("Use -1 for no reminder. All-day reminders are based on 09:00.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank()) {
                    error = "Enter an event title."
                    return@Button
                }
                val reminder = reminderMinutes.toIntOrNull()
                if (reminder == null || reminder < -1 || reminder > 10080) {
                    error = "Reminder must be -1 (off) or between 0 and 10080 minutes."
                    return@Button
                }
                val start = Calendar.getInstance().apply { timeInMillis = dayStart(initial?.startAt ?: selectedDay) }
                val end = Calendar.getInstance().apply { timeInMillis = dayStart(initial?.startAt ?: selectedDay) }
                if (!allDay) {
                    val startParts = startTime.split(":")
                    val endParts = endTime.split(":")
                    val valid = startParts.size == 2 && endParts.size == 2 &&
                        startParts.all { it.toIntOrNull() != null } && endParts.all { it.toIntOrNull() != null } &&
                        startParts[0].toInt() in 0..23 && endParts[0].toInt() in 0..23 &&
                        startParts[1].toInt() in 0..59 && endParts[1].toInt() in 0..59
                    if (!valid) {
                        error = "Use 24-hour time, for example 09:30."
                        return@Button
                    }
                    start.set(Calendar.HOUR_OF_DAY, startParts[0].toInt())
                    start.set(Calendar.MINUTE, startParts[1].toInt())
                    end.set(Calendar.HOUR_OF_DAY, endParts[0].toInt())
                    end.set(Calendar.MINUTE, endParts[1].toInt())
                    if (end.timeInMillis <= start.timeInMillis) {
                        error = "End time must be after start time."
                        return@Button
                    }
                } else {
                    end.add(Calendar.DAY_OF_MONTH, 1)
                    end.add(Calendar.MILLISECOND, -1)
                }
                onSave(
                    CalendarEvent(
                        id = initial?.id ?: System.currentTimeMillis(),
                        title = title.trim(),
                        startAt = start.timeInMillis,
                        endAt = end.timeInMillis,
                        allDay = allDay,
                        location = location.trim(),
                        notes = notes.trim(),
                        reminderMinutes = reminder
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (initial != null) TextButton(onClick = { onDelete(initial) }) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

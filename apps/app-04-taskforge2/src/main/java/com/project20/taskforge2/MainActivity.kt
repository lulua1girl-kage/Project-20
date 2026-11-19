package com.project20.taskforge2

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val PREFS = "taskforge2_local"
private const val TASKS_KEY = "tasks"
private const val LEDGER_KEY = "ledger"
private const val CHANNEL_ID = "taskforge_accountability"

data class TaskItem(
    val id: String,
    val title: String,
    val dueAt: Long,
    val monitored: Boolean,
    val done: Boolean = false
)

data class LedgerEntry(
    val id: String,
    val sourceId: String,
    val kind: String,
    val amount: Int,
    val reason: String,
    val createdAt: Long
)

object LocalStore {
    fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun tasks(context: Context): MutableList<TaskItem> {
        val array = JSONArray(prefs(context).getString(TASKS_KEY, "[]"))
        return MutableList(array.length()) { i ->
            val o = array.getJSONObject(i)
            TaskItem(o.getString("id"), o.getString("title"), o.getLong("dueAt"),
                o.optBoolean("monitored"), o.optBoolean("done"))
        }
    }

    fun saveTasks(context: Context, tasks: List<TaskItem>) {
        val array = JSONArray()
        tasks.forEach { t -> array.put(JSONObject().put("id", t.id).put("title", t.title)
            .put("dueAt", t.dueAt).put("monitored", t.monitored).put("done", t.done)) }
        prefs(context).edit().putString(TASKS_KEY, array.toString()).apply()
    }

    fun ledger(context: Context): MutableList<LedgerEntry> {
        val array = JSONArray(prefs(context).getString(LEDGER_KEY, "[]"))
        return MutableList(array.length()) { i ->
            val o = array.getJSONObject(i)
            LedgerEntry(o.getString("id"), o.getString("sourceId"), o.getString("kind"),
                o.getInt("amount"), o.getString("reason"), o.getLong("createdAt"))
        }
    }

    fun balance(context: Context) = ledger(context).sumOf { it.amount }

    @Synchronized fun post(context: Context, sourceId: String, kind: String, amount: Int, reason: String): Boolean {
        val entries = ledger(context)
        if (entries.any { it.sourceId == sourceId && it.kind == kind }) return false
        val actual = if (amount < 0) -minOf(balance(context), -amount) else amount
        if (actual == 0) return false
        val entry = LedgerEntry(UUID.randomUUID().toString(), sourceId, kind, actual, reason, System.currentTimeMillis())
        val o = JSONObject().put("id", entry.id).put("sourceId", entry.sourceId).put("kind", entry.kind)
            .put("amount", entry.amount).put("reason", entry.reason).put("createdAt", entry.createdAt)
        val array = JSONArray()
        entries.forEach { e -> array.put(JSONObject().put("id", e.id).put("sourceId", e.sourceId)
            .put("kind", e.kind).put("amount", e.amount).put("reason", e.reason).put("createdAt", e.createdAt)) }
        array.put(o)
        prefs(context).edit().putString(LEDGER_KEY, array.toString()).commit()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createChannel()
        setContent { TaskForgeApp(this) }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Task accountability", NotificationManager.IMPORTANCE_HIGH))
    }
}

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("taskId") ?: return
        val task = LocalStore.tasks(context).firstOrNull { it.id == taskId && !it.done } ?: return
        val doneIntent = actionIntent(context, taskId, "DONE", 10)
        val missIntent = actionIntent(context, taskId, "MISSED", 11)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Is it done?")
            .setContentText("You said you would complete ${task.title}.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Done", doneIntent)
            .addAction(0, "Not Complete", missIntent)
            .build()
        if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
        }
    }
}

class TaskActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("taskId") ?: return
        when (intent.action) {
            "DONE" -> {
                val tasks = LocalStore.tasks(context)
                val index = tasks.indexOfFirst { it.id == id }
                if (index >= 0 && !tasks[index].done) {
                    tasks[index] = tasks[index].copy(done = true)
                    LocalStore.saveTasks(context, tasks)
                    LocalStore.post(context, id, "TASK_DONE", 20, "Completed: ${tasks[index].title}")
                }
            }
            "MISSED" -> LocalStore.post(context, id, "MERIT_DEDUCTION", -50, "Task not completed")
        }
    }
}

private fun actionIntent(context: Context, taskId: String, action: String, request: Int): PendingIntent {
    val intent = Intent(context, TaskActionReceiver::class.java).setAction(action).putExtra("taskId", taskId)
    return PendingIntent.getBroadcast(context, request + taskId.hashCode(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

private fun scheduleTask(context: Context, task: TaskItem) {
    if (!task.monitored || task.done) return
    val intent = Intent(context, TaskAlarmReceiver::class.java).putExtra("taskId", task.id)
    val pending = PendingIntent.getBroadcast(context, task.id.hashCode(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (Build.VERSION.SDK_INT >= 31 && !alarm.canScheduleExactAlarms()) return
    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueAt, pending)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskForgeApp(context: Context) {
    var refresh by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf("Home") }
    var showAdd by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var dueText by remember { mutableStateOf("6:00 PM") }
    var monitored by remember { mutableStateOf(true) }
    val tasks = remember(refresh) { LocalStore.tasks(context).sortedBy { it.dueAt } }
    val balance = remember(refresh) { LocalStore.balance(context) }
    val ledger = remember(refresh) { LocalStore.ledger(context).sortedByDescending { it.createdAt } }

    MaterialTheme(colorScheme = darkColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF9B6BFF))) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("TaskForge 2.0") }) },
            bottomBar = {
                NavigationBar {
                    listOf("Home", "Tasks", "Wallet", "Study", "Settings").forEach { item ->
                        NavigationBarItem(selected = tab == item, onClick = { tab = item },
                            icon = { Text(when(item) { "Home" -> "⌂"; "Tasks" -> "✓"; "Wallet" -> "★"; "Study" -> "◷"; else -> "⚙" }) },
                            label = { Text(item) })
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (tab) {
                    "Home" -> {
                        Text("Your accountability dashboard", style = MaterialTheme.typography.titleLarge)
                        Card(shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(18.dp)) {
                                Text("NP BALANCE")
                                Text("$balance NP", style = MaterialTheme.typography.headlineMedium)
                                Text("Merit is your accountability system; NP is the shared points balance.")
                            }
                        }
                        Text("Today's tasks")
                        tasks.take(5).forEach { TaskRow(it, context) { refresh++ } }
                        Button(onClick = { tab = "Tasks" }) { Text("Manage tasks") }
                    }
                    "Tasks" -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tasks", style = MaterialTheme.typography.titleLarge)
                            Button(onClick = { showAdd = true }) { Text("Add task") }
                        }
                        if (tasks.isEmpty()) Text("No tasks yet. Add your first task.")
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tasks, key = { it.id }) { task -> TaskRow(task, context) { refresh++ } }
                        }
                    }
                    "Wallet" -> {
                        Text("NP Wallet", style = MaterialTheme.typography.titleLarge)
                        Text("$balance NP", style = MaterialTheme.typography.headlineLarge)
                        Text("Permanent transaction ledger")
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ledger, key = { it.id }) { entry ->
                                Card { Column(Modifier.padding(12.dp)) {
                                    Text("${if(entry.amount >= 0) "+" else ""}${entry.amount} NP • ${entry.kind}")
                                    Text(entry.reason)
                                    Text(SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(entry.createdAt)),
                                        style = MaterialTheme.typography.bodySmall)
                                } }
                            }
                        }
                    }
                    "Study" -> StudyScreen(context) { refresh++ }
                    "Settings" -> {
                        Text("Settings", style = MaterialTheme.typography.titleLarge)
                        Text("Phone time: " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()))
                        Text("Time zone: " + java.util.TimeZone.getDefault().id)
                        Button(onClick = {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }) { Text("Grant screen-time access") }
                        Button(onClick = {
                            if (Build.VERSION.SDK_INT >= 33) {
                                val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                context.startActivity(i)
                            }
                        }) { Text("Notification settings") }
                        Text("Local-only storage is enabled. Data remains on this device.")
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("New task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Task name") })
                    OutlinedTextField(dueText, { dueText = it }, label = { Text("Due time (e.g. 6:00 PM)") })
                    Row {
                        Checkbox(checked = monitored, onCheckedChange = { monitored = it })
                        Text("Monitor with notification")
                    }
                    Text("For this first build, tasks are scheduled for today.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = try {
                        val date = SimpleDateFormat("h:mm a", Locale.US).parse(dueText.trim())!!
                        val now = java.util.Calendar.getInstance()
                        val time = java.util.Calendar.getInstance().apply { time = date }
                        now.set(java.util.Calendar.HOUR_OF_DAY, time.get(java.util.Calendar.HOUR_OF_DAY))
                        now.set(java.util.Calendar.MINUTE, time.get(java.util.Calendar.MINUTE))
                        now.set(java.util.Calendar.SECOND, 0)
                        now.timeInMillis
                    } catch (_: Exception) { System.currentTimeMillis() + 60_000 }
                    if (title.isNotBlank()) {
                        val task = TaskItem(UUID.randomUUID().toString(), title.trim(), parsed, monitored)
                        val all = LocalStore.tasks(context).apply { add(task) }
                        LocalStore.saveTasks(context, all)
                        scheduleTask(context, task)
                        title = ""
                        showAdd = false
                        refresh++
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TaskRow(task: TaskItem, context: Context, onChanged: () -> Unit) {
    Card {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                Text(SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(Date(task.dueAt)))
                Text(if(task.monitored) "Monitored" else "Not monitored")
            }
            if (task.done) Text("Done")
            else Button(onClick = {
                val all = LocalStore.tasks(context)
                val i = all.indexOfFirst { it.id == task.id }
                if (i >= 0 && !all[i].done) {
                    all[i] = all[i].copy(done = true)
                    LocalStore.saveTasks(context, all)
                    LocalStore.post(context, task.id, "TASK_DONE", 20, "Completed: ${task.title}")
                    onChanged()
                }
            }) { Text("Complete") }
        }
    }
}

@Composable
private fun StudyScreen(context: Context, onChanged: () -> Unit) {
    var minutes by remember { mutableStateOf("60") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Study Tracker", style = MaterialTheme.typography.titleLarge)
        Text("Record a completed study session. Rewards are credited once per session.")
        OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(4) }, label = { Text("Minutes studied") })
        Button(onClick = {
            val value = minutes.toIntOrNull() ?: 0
            if (value in 1..600) {
                val sessionId = UUID.randomUUID().toString()
                val reward = (value / 60) * 10
                if (reward > 0) LocalStore.post(context, sessionId, "STUDY_REWARD", reward, "$value-minute study session")
                onChanged()
            }
        }) { Text("Save study session") }
        Text("Initial reward rule: 10 NP per completed hour. Ten hours is a ceiling, not a target.")
    }
}

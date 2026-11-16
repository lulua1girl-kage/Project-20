package com.project20.calendar

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.util.Calendar

private const val CHANNEL_ID = "calendar_event_reminders"
private const val ACTION_EVENT_REMINDER = "com.project20.calendar.EVENT_REMINDER"
private const val EXTRA_EVENT_ID = "event_id"
private const val EXTRA_EVENT_TITLE = "event_title"
private const val EXTRA_EVENT_START = "event_start"
private const val EXTRA_ALL_DAY = "all_day"

fun createCalendarNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Calendar reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts for events saved in Calendar"
        }
        manager.createNotificationChannel(channel)
    }
}

private fun reminderTriggerAt(event: CalendarEvent): Long {
    val baseTime = if (event.allDay) {
        Calendar.getInstance().apply {
            timeInMillis = event.startAt
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    } else {
        event.startAt
    }
    return baseTime - event.reminderMinutes * 60_000L
}

private fun reminderPendingIntent(
    context: Context,
    eventId: Long,
    title: String = "",
    startAt: Long = 0L,
    allDay: Boolean = false
): PendingIntent {
    val intent = Intent(context, EventReminderReceiver::class.java).apply {
        action = ACTION_EVENT_REMINDER
        data = android.net.Uri.parse("project20-calendar://reminder/$eventId")
        putExtra(EXTRA_EVENT_ID, eventId)
        putExtra(EXTRA_EVENT_TITLE, title)
        putExtra(EXTRA_EVENT_START, startAt)
        putExtra(EXTRA_ALL_DAY, allDay)
    }
    return PendingIntent.getBroadcast(
        context,
        eventId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

fun scheduleEventReminder(context: Context, event: CalendarEvent) {
    cancelEventReminder(context, event.id)
    if (event.reminderMinutes < 0) return

    val triggerAt = reminderTriggerAt(event)
    if (triggerAt <= System.currentTimeMillis()) return

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = reminderPendingIntent(
        context, event.id, event.title, event.startAt, event.allDay
    )
    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
}

fun cancelEventReminder(context: Context, eventId: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = reminderPendingIntent(context, eventId)
    alarmManager.cancel(pendingIntent)
    pendingIntent.cancel()
}

class EventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val title = intent.getStringExtra(EXTRA_EVENT_TITLE).orEmpty().ifBlank { "Calendar event" }
        val startAt = intent.getLongExtra(EXTRA_EVENT_START, 0L)
        val allDay = intent.getBooleanExtra(EXTRA_ALL_DAY, false)
        val detail = if (allDay) {
            "All-day event today"
        } else {
            "Starts at " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(startAt))
        }

        val openApp = PendingIntent.getActivity(
            context,
            intent.getLongExtra(EXTRA_EVENT_ID, 0L).hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            Notification.Builder(context).setPriority(Notification.PRIORITY_DEFAULT)
        }

        val notification = builder
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(detail)
            .setStyle(Notification.BigTextStyle().bigText(detail))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        manager.notify(intent.getLongExtra(EXTRA_EVENT_ID, 0L).hashCode(), notification)
    }
}

class CalendarBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            createCalendarNotificationChannel(context)
            loadEvents(context).forEach { scheduleEventReminder(context, it) }
        }
    }
}

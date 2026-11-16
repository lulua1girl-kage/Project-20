package com.project20.clock

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
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class ClockAlarm(
    val id: Long, val hour: Int, val minute: Int, val label: String,
    val days: Set<Int>, val enabled: Boolean
)
data class ClockTimer(
    val id: Long, val label: String, val totalMs: Long,
    val remainingMs: Long, val endAt: Long, val running: Boolean
)

private const val PREFS = "nori_clock_store"
private const val ALARMS_KEY = "alarms"
private const val TIMERS_KEY = "timers"
private const val CHANNEL_ALARM = "nori_alarm_alerts"
private const val CHANNEL_TIMER = "nori_timer_alerts"

fun loadAlarms(c: Context): List<ClockAlarm> = runCatching {
    val a = JSONArray(c.getSharedPreferences(PREFS, 0).getString(ALARMS_KEY, "[]"))
    buildList {
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            val daysJson = o.optJSONArray("days") ?: JSONArray()
            val days = buildSet { for (j in 0 until daysJson.length()) add(daysJson.getInt(j)) }
            add(ClockAlarm(o.getLong("id"),o.getInt("hour"),o.getInt("minute"),o.optString("label"),days,o.optBoolean("enabled",true)))
        }
    }
}.getOrDefault(emptyList())

fun saveAlarms(c: Context, list: List<ClockAlarm>) {
    val a = JSONArray()
    list.forEach { x ->
        val d = JSONArray(); x.days.sorted().forEach { d.put(it) }
        a.put(JSONObject().put("id",x.id).put("hour",x.hour).put("minute",x.minute).put("label",x.label).put("days",d).put("enabled",x.enabled))
    }
    c.getSharedPreferences(PREFS,0).edit().putString(ALARMS_KEY,a.toString()).apply()
}

fun loadTimers(c: Context): List<ClockTimer> = runCatching {
    val a = JSONArray(c.getSharedPreferences(PREFS,0).getString(TIMERS_KEY,"[]"))
    buildList {
        for (i in 0 until a.length()) {
            val o=a.getJSONObject(i)
            add(ClockTimer(o.getLong("id"),o.optString("label"),o.getLong("total"),o.getLong("remaining"),o.getLong("end"),o.optBoolean("running")))
        }
    }
}.getOrDefault(emptyList())

fun saveTimers(c: Context, list: List<ClockTimer>) {
    val a=JSONArray()
    list.forEach { a.put(JSONObject().put("id",it.id).put("label",it.label).put("total",it.totalMs).put("remaining",it.remainingMs).put("end",it.endAt).put("running",it.running)) }
    c.getSharedPreferences(PREFS,0).edit().putString(TIMERS_KEY,a.toString()).apply()
}

fun createClockChannels(c: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        val nm=c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ALARM,"Alarm alerts",NotificationManager.IMPORTANCE_HIGH).apply { description="Alerts for alarms you create in Nori Clock"; setBypassDnd(false) })
        nm.createNotificationChannel(NotificationChannel(CHANNEL_TIMER,"Timer alerts",NotificationManager.IMPORTANCE_HIGH).apply { description="Timer completion alerts" })
    }
}

private fun alarmPi(c: Context, x: ClockAlarm): PendingIntent {
    val i=Intent(c,ClockAlarmReceiver::class.java).setAction("com.project20.clock.ALARM").setData(android.net.Uri.parse("noriclock://alarm/"+x.id))
    i.putExtra("alarm_id",x.id); i.putExtra("alarm_label",x.label)
    return PendingIntent.getBroadcast(c,x.id.toInt(),i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
fun cancelAlarm(c: Context,x:ClockAlarm) { val p=alarmPi(c,x); (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(p); p.cancel() }

fun nextAlarmTime(x: ClockAlarm, after: Long = System.currentTimeMillis()): Long {
    val cal=Calendar.getInstance().apply {
        timeInMillis=after; set(Calendar.HOUR_OF_DAY,x.hour); set(Calendar.MINUTE,x.minute); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0)
    }
    if (x.days.isEmpty()) {
        if (cal.timeInMillis<=after) cal.add(Calendar.DAY_OF_YEAR,1)
        return cal.timeInMillis
    }
    for (step in 0..7) {
        val day=cal.get(Calendar.DAY_OF_WEEK)
        if (day in x.days && cal.timeInMillis>after) return cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR,1)
    }
    return cal.timeInMillis
}
fun scheduleAlarm(c: Context,x:ClockAlarm) {
    cancelAlarm(c,x); if (!x.enabled) return
    val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi=alarmPi(c,x); val at=nextAlarmTime(x)
    try {
        if (Build.VERSION.SDK_INT >= 31 && am.canScheduleExactAlarms()) {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(at,pi),pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi)
        }
    } catch (_: SecurityException) {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi)
    }
}

private fun timerPi(c:Context,x:ClockTimer):PendingIntent {
    val i=Intent(c,ClockTimerReceiver::class.java).setAction("com.project20.clock.TIMER").setData(android.net.Uri.parse("noriclock://timer/"+x.id))
    i.putExtra("timer_id",x.id); i.putExtra("timer_label",x.label)
    return PendingIntent.getBroadcast(c,x.id.toInt(),i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
fun scheduleTimer(c:Context,x:ClockTimer) {
    val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val p=timerPi(c,x); am.cancel(p)
    if (!x.running) return
    try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,x.endAt,p) } catch (_:Exception) { }
}
fun cancelTimer(c:Context,x:ClockTimer) { val p=timerPi(c,x); (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(p); p.cancel() }

private fun showAlert(c:Context,channel:String,id:Int,title:String,body:String) {
    if (Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return
    val open=PendingIntent.getActivity(c,id,Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val b=if(Build.VERSION.SDK_INT>=26) Notification.Builder(c,channel) else Notification.Builder(c)
    val n=b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(title).setContentText(body).setContentIntent(open).setAutoCancel(true).setCategory(Notification.CATEGORY_ALARM).setPriority(Notification.PRIORITY_HIGH).build()
    (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id,n)
}
class ClockAlarmReceiver:BroadcastReceiver() {
    override fun onReceive(c:Context,i:Intent) {
        val id=i.getLongExtra("alarm_id",0L)
        val label=i.getStringExtra("alarm_label").orEmpty().ifBlank { "Alarm" }
        showAlert(c,CHANNEL_ALARM,id.toInt(),"Nori Clock · "+label,"Your alarm is due.")
        val a=loadAlarms(c).firstOrNull { it.id==id } ?: return
        if(a.days.isNotEmpty()) scheduleAlarm(c,a) else saveAlarms(c,loadAlarms(c).map { if(it.id==id) it.copy(enabled=false) else it })
    }
}
class ClockTimerReceiver:BroadcastReceiver() {
    override fun onReceive(c:Context,i:Intent) {
        val id=i.getLongExtra("timer_id",0L)
        val label=i.getStringExtra("timer_label").orEmpty().ifBlank { "Timer" }
        showAlert(c,CHANNEL_TIMER,id.toInt(),"Timer finished · "+label,"Your countdown has finished.")
        saveTimers(c,loadTimers(c).filterNot { it.id==id })
    }
}
class ClockBootReceiver:BroadcastReceiver() {
    override fun onReceive(c:Context,i:Intent) {
        if(i.action==Intent.ACTION_BOOT_COMPLETED) {
            createClockChannels(c)
            loadAlarms(c).forEach { scheduleAlarm(c,it) }
            val now=System.currentTimeMillis()
            val repaired=loadTimers(c).mapNotNull { t ->
                if(!t.running) t else if(t.endAt<=now) null else t
            }
            saveTimers(c,repaired); repaired.forEach { scheduleTimer(c,it) }
        }
    }
}

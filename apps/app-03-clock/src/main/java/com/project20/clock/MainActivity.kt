package com.project20.clock

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val Navy=Color(0xFF101B2B)
private val Panel=Color(0xFF1A2A40)
private val Accent=Color(0xFF65C7F2)
private data class City(val name:String,val country:String,val zone:String)
private val cityCatalog=listOf(
    City("Addis Ababa","Ethiopia","Africa/Addis_Ababa"),City("Nairobi","Kenya","Africa/Nairobi"),
    City("London","United Kingdom","Europe/London"),City("Paris","France","Europe/Paris"),
    City("New York","United States","America/New_York"),City("Los Angeles","United States","America/Los_Angeles"),
    City("Tokyo","Japan","Asia/Tokyo"),City("Seoul","South Korea","Asia/Seoul"),
    City("Dubai","United Arab Emirates","Asia/Dubai"),City("Mumbai","India","Asia/Kolkata"),
    City("Singapore","Singapore","Asia/Singapore"),City("Sydney","Australia","Australia/Sydney"),
    City("Cairo","Egypt","Africa/Cairo"),City("Johannesburg","South Africa","Africa/Johannesburg"),
    City("Toronto","Canada","America/Toronto"),City("Berlin","Germany","Europe/Berlin")
)
private fun clockText(ms:Long,zone:String=TimeZone.getDefault().id,pattern:String="HH:mm"):String =
    SimpleDateFormat(pattern,Locale.getDefault()).apply { timeZone=TimeZone.getTimeZone(zone) }.format(Date(ms))
private fun offsetText(zone:String,at:Long=System.currentTimeMillis()):String {
    val off=TimeZone.getTimeZone(zone).getOffset(at)/60000
    val sign=if(off>=0) "+" else "−"; val abs= kotlin.math.abs(off)
    return "UTC "+sign+String.format(Locale.ROOT,"%02d:%02d",abs/60,abs%60)
}
private fun fmt(ms:Long):String {
    val s=(ms.coerceAtLeast(0L)+999)/1000
    return String.format(Locale.ROOT,"%02d:%02d:%02d",s/3600,(s%3600)/60,s%60)
}

class MainActivity:ComponentActivity() {
    private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        createClockChannels(this)
        setContent {
            MaterialTheme(colorScheme=darkColorScheme(primary=Accent,background=Navy,surface=Panel,onBackground=Color(0xFFEAF2FA),onSurface=Color(0xFFEAF2FA))) {
                ClockApp(this,
                    onNotificationPermission={ if(Build.VERSION.SDK_INT>=33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    onExactAlarmPermission={ if(Build.VERSION.SDK_INT>=31) runCatching { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+packageName))) } }
                )
            }
        }
    }
}

@Composable
private fun ClockApp(context:Context,onNotificationPermission:()->Unit,onExactAlarmPermission:()->Unit) {
    var tab by remember { mutableStateOf("Alarm") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while(true) { now=System.currentTimeMillis(); delay(500) } }
    val alarms= remember { mutableStateListOf<ClockAlarm>().apply { addAll(loadAlarms(context)) } }
    val timers= remember { mutableStateListOf<ClockTimer>().apply { addAll(loadTimers(context)) } }
    val cities= remember { mutableStateListOf<City>().apply { addAll(listOf(cityCatalog[0],cityCatalog[2],cityCatalog[6],cityCatalog[4])) } }
    var showAlarm by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    var showCitySearch by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var editingAlarm by remember { mutableStateOf<ClockAlarm?>(null) }
    var editingTimer by remember { mutableStateOf<ClockTimer?>(null) }

    Surface(Modifier.fillMaxSize(),color=Navy) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().padding(start=20.dp,end=20.dp,top=20.dp,bottom=10.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(Panel,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) { Text("◷",color=Accent,style=MaterialTheme.typography.headlineMedium) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("NORI CLOCK",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                        Text("Time, organised.",style=MaterialTheme.typography.labelSmall,color=Color(0xFF9FB1C7))
                    }
                    Text(clockText(now),style=MaterialTheme.typography.titleMedium,color=Accent,fontWeight=FontWeight.SemiBold)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                listOf("Alarm","World","Stopwatch","Timer").forEach { name ->
                    val label=if(name=="World") "World" else name
                    Box(Modifier.weight(1f).background(if(tab==name) Panel else Color.Transparent,RoundedCornerShape(12.dp)).clickable { tab=name }.padding(vertical=12.dp),contentAlignment=Alignment.Center) {
                        Text(label,style=MaterialTheme.typography.labelMedium,color=if(tab==name) Accent else Color(0xFF9FB1C7),fontWeight=if(tab==name) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            when(tab) {
                "Alarm" -> AlarmScreen(context,alarms,now,{editingAlarm=null;showAlarm=true},{editingAlarm=it;showAlarm=true},onNotificationPermission,onExactAlarmPermission)
                "World" -> WorldScreen(cities,now,{showCitySearch=true})
                "Stopwatch" -> StopwatchScreen(context)
                else -> TimerScreen(context,timers,now,{editingTimer=null;showTimer=true},{editingTimer=it;showTimer=true},onNotificationPermission)
            }
        }
    }
    if(showAlarm) AlarmEditor(editingAlarm,onDismiss={showAlarm=false},onSave={a ->
        val idx=alarms.indexOfFirst { it.id==a.id }; if(idx>=0) alarms[idx]=a else alarms.add(a)
        saveAlarms(context,alarms); scheduleAlarm(context,a); showAlarm=false
    })
    if(showTimer) TimerEditor(editingTimer,onDismiss={showTimer=false},onSave={label,duration ->
        val t=ClockTimer(System.currentTimeMillis(),label,duration,duration,0L,false)
        timers.add(t); saveTimers(context,timers); showTimer=false
    })
    if(showCitySearch) CitySearch(cities,search,{search=it},{showCitySearch=false},{c ->
        if(cities.none { it.zone==c.zone }) cities.add(c); showCitySearch=false; search=""
    })
}

@Composable
private fun ScreenHeader(title:String,subtitle:String,action:String,click:()->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,color=Color(0xFF9FB1C7))
        }
        Button(onClick=click,contentPadding=PaddingValues(horizontal=14.dp,vertical=8.dp)) { Text(action) }
    }
}

@Composable
private fun AlarmScreen(context:Context,alarms:MutableList<ClockAlarm>,now:Long,add:()->Unit,edit:(ClockAlarm)->Unit,askNotify:()->Unit,askExact:()->Unit) {
    val am=context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val exact=Build.VERSION.SDK_INT<31 || am.canScheduleExactAlarms()
    Column(Modifier.fillMaxSize().padding(horizontal=18.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        ScreenHeader("Alarms","Create one-time or repeating alarms.","+ Add",add)
        if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
            OutlinedButton(onClick=askNotify,modifier=Modifier.fillMaxWidth()) { Text("Enable alarm notifications") }
        if(!exact) OutlinedButton(onClick=askExact,modifier=Modifier.fillMaxWidth()) { Text("Allow precise alarm scheduling") }
        if(alarms.isEmpty()) EmptyPanel("No alarms yet","Add an alarm to get started.")
        alarms.sortedBy { it.hour*60+it.minute }.forEach { a ->
            Card(colors=CardDefaults.cardColors(containerColor=Panel),modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { edit(a) }) {
                        Text(String.format(Locale.ROOT,"%02d:%02d",a.hour,a.minute),style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Light)
                        Text(a.label.ifBlank { "Alarm" },style=MaterialTheme.typography.titleSmall)
                        Text(if(a.days.isEmpty()) "One time" else a.days.sorted().joinToString(" · ") { listOf("","Sun","Mon","Tue","Wed","Thu","Fri","Sat")[it] },style=MaterialTheme.typography.labelSmall,color=Color(0xFF9FB1C7))
                        Text("Next: "+clockText(nextAlarmTime(a,now),TimeZone.getDefault().id,"EEE, d MMM"),style=MaterialTheme.typography.labelSmall,color=Accent)
                    }
                    Switch(checked=a.enabled,onCheckedChange={v ->
                        val updated=a.copy(enabled=v); val i=alarms.indexOfFirst { it.id==a.id }; if(i>=0) alarms[i]=updated
                        saveAlarms(context,alarms); scheduleAlarm(context,updated)
                    })
                    TextButton(onClick={edit(a)}) { Text("Edit") }
                }
            }
        }
        Text("Alarm delivery uses Android system scheduling. Battery settings and notification permission can affect alerts.",style=MaterialTheme.typography.labelSmall,color=Color(0xFF9FB1C7))
    }
}

@Composable
private fun AlarmEditor(initial:ClockAlarm?,onDismiss:()->Unit,onSave:(ClockAlarm)->Unit) {
    var hour by remember { mutableIntStateOf(initial?.hour?:7) }
    var minute by remember { mutableIntStateOf(initial?.minute?:0) }
    var label by remember { mutableStateOf(initial?.label?:"") }
    val days= remember { mutableStateListOf<Int>().apply { addAll(initial?.days?: emptySet()) } }
    val ctx=androidx.compose.ui.platform.LocalContext.current
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(initial==null) "New alarm" else "Edit alarm")},text={
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(String.format(Locale.ROOT,"%02d:%02d",hour,minute),style=MaterialTheme.typography.displayMedium,color=Accent,modifier=Modifier.fillMaxWidth().clickable {
                TimePickerDialog(ctx,{_,h,m -> hour=h;minute=m},hour,minute,true).show()
            })
            OutlinedTextField(value=label,onValueChange={label=it},label={Text("Alarm label")},singleLine=true)
            Text("Repeat",style=MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                listOf("S","M","T","W","T","F","S").forEachIndexed { i,s ->
                    val day=i+1
                    FilterChip(selected=days.contains(day),onClick={if(days.contains(day)) days.remove(day) else days.add(day)},label={Text(s)})
                }
            }
            Text("Tap the time to change it. No selected days means a one-time alarm.",style=MaterialTheme.typography.labelSmall,color=Color.Gray)
        }
    },confirmButton={TextButton(onClick={onSave(ClockAlarm(initial?.id?:System.currentTimeMillis(),hour,minute,label,days.toSet(),true))}) { Text("Save") }},dismissButton={
        Row {
            if(initial!=null) TextButton(onClick={onDismiss}) { Text("Cancel") }
            else TextButton(onClick=onDismiss) { Text("Cancel") }
        }
    })
}

@Composable
private fun WorldScreen(cities:MutableList<City>,now:Long,add:()->Unit) {
    var source by remember { mutableStateOf("Africa/Addis_Ababa") }
    var input by remember { mutableStateOf("09:00") }
    var showCompare by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal=18.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        ScreenHeader("World Clock","Time across your saved cities.","+ City",add)
        cities.forEach { c ->
            Card(colors=CardDefaults.cardColors(containerColor=Panel),modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
                        Text(c.country+" · "+offsetText(c.zone,now),style=MaterialTheme.typography.labelSmall,color=Color(0xFF9FB1C7))
                        Text(clockText(now,c.zone,"EEE, d MMM yyyy"),style=MaterialTheme.typography.labelSmall,color=Color(0xFF9FB1C7))
                    }
                    Column(horizontalAlignment=Alignment.End) {
                        Text(clockText(now,c.zone),style=MaterialTheme.typography.headlineMedium,color=Accent,fontWeight=FontWeight.Light)
                        Text(if(clockText(now,c.zone,"a")=="AM") "Day" else "Evening",style=MaterialTheme.typography.labelSmall,color=Color(0xFF9FB1C7))
                    }
                    TextButton(onClick={cities.remove(c)}) { Text("×") }
                }
            }
        }
        Card(colors=CardDefaults.cardColors(containerColor=Panel),modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Time-zone converter",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                Text("Choose a source city and local time.",style=MaterialTheme.typography.bodySmall,color=Color(0xFF9FB1C7))
                var chosen by remember { mutableStateOf(cities.firstOrNull()?.name?:"Addis Ababa") }
                var time by remember { mutableStateOf(input) }
                Text("From: "+chosen,color=Accent)
                OutlinedTextField(value=time,onValueChange={time=it;input=it},label={Text("Local time (HH:mm)")},singleLine=true)
                Button(onClick={showCompare=!showCompare},modifier=Modifier.fillMaxWidth()) { Text(if(showCompare) "Hide comparison" else "Convert time") }
                if(showCompare) {
                    val src=cities.firstOrNull { it.name==chosen } ?: cityCatalog[0]
                    val parts=time.split(":")
                    val valid=parts.size==2 && parts[0].toIntOrNull() in 0..23 && parts[1].toIntOrNull() in 0..59
                    if(valid) {
                        val cal=Calendar.getInstance(TimeZone.getTimeZone(src.zone)).apply { set(Calendar.HOUR_OF_DAY,parts[0].toInt());set(Calendar.MINUTE,parts[1].toInt());set(Calendar.SECOND,0) }
                        cities.filter { it.zone!=src.zone }.forEach { c ->
                            Text(c.name+"   "+clockText(cal.timeInMillis,c.zone,"HH:mm  EEE, d MMM"),color=Color(0xFFEAF2FA))
                        }
                    } else Text("Enter a valid 24-hour time, such as 14:30.",color=Color(0xFFFFB4AB))
                }
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    cities.take(4).forEach { c -> FilterChip(selected=chosen==c.name,onClick={chosen=c.name},label={Text(c.name)}) }
                }
            }
        }
    }
}

@Composable
private fun CitySearch(cities:List<City>,query:String,onQuery:(String)->Unit,onDismiss:()->Unit,onAdd:(City)->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={Text("Add a city")},text={
        Column {
            OutlinedTextField(value=query,onValueChange=onQuery,label={Text("Search city or country")},singleLine=true)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState())) {
                cityCatalog.filter { (it.name+" "+it.country).contains(query,true) && cities.none { c->c.zone==it.zone } }.forEach { c ->
                    TextButton(onClick={onAdd(c)},modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) { Text(c.name);Text(c.country+" · "+c.zone,style=MaterialTheme.typography.labelSmall,color=Color.Gray) }
                    }
                }
            }
        }
    },confirmButton={TextButton(onClick=onDismiss){Text("Close")}})
}

@Composable
private fun StopwatchScreen(context:Context) {
    val prefs=context.getSharedPreferences("nori_stopwatch",0)
    var elapsed by remember { mutableLongStateOf(prefs.getLong("elapsed",0L)) }
    var runningSince by remember { mutableLongStateOf(prefs.getLong("running_since",0L)) }
    val running=runningSince>0L
    var laps by remember { mutableStateOf(prefs.getString("laps","")!!.split("|").filter { it.isNotBlank() }) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) { while(true) { now=SystemClock.elapsedRealtime();delay(30) } }
    val total=elapsed+if(running) (now-runningSince).coerceAtLeast(0L) else 0L
    fun persist(e:Long,r:Long,l:List<String>) { prefs.edit().putLong("elapsed",e).putLong("running_since",r).putString("laps",l.joinToString("|")).apply() }
    Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
        ScreenHeader("Stopwatch","Precise elapsed time and lap records.","Lap") {
            if(running) { val stamp=fmt(total)+"."+String.format(Locale.ROOT,"%02d",(total%1000)/10);laps=laps+stamp;persist(elapsed,runningSince,laps) }
        }
        Spacer(Modifier.height(30.dp))
        Text(String.format(Locale.ROOT,"%02d:%02d:%02d.%02d",total/3600000,(total/60000)%60,(total/1000)%60,(total%1000)/10),style=MaterialTheme.typography.displayLarge,color=Accent,fontWeight=FontWeight.Light)
        Text(if(running) "STOPWATCH RUNNING" else "READY",style=MaterialTheme.typography.labelMedium,color=Color(0xFF9FB1C7))
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick={
                if(running) { elapsed=total;runningSince=0L;persist(elapsed,0L,laps) }
                else { runningSince=SystemClock.elapsedRealtime();persist(elapsed,runningSince,laps) }
            }) { Text(if(running) "Pause" else if(total>0) "Resume" else "Start") }
            Button(onClick={elapsed=0L;runningSince=0L;laps= emptyList();persist(0L,0L,laps)}) { Text("Reset") }
        }
        Text("Laps",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.Start))
        if(laps.isEmpty()) EmptyPanel("No laps recorded","Press Lap while the stopwatch is running.")
        else LazyColumn(Modifier.fillMaxWidth().weight(1f)) { items(laps.reversed()) { Text(it,modifier=Modifier.fillMaxWidth().padding(12.dp),color=Color(0xFFEAF2FA)) } }
    }
}

@Composable
private fun TimerScreen(context:Context,timers:MutableList<ClockTimer>,now:Long,add:()->Unit,edit:(ClockTimer)->Unit,askNotify:()->Unit) {
    val updated=timers.map { if(it.running) it.copy(remainingMs=(it.endAt-now).coerceAtLeast(0L)) else it }
    Column(Modifier.fillMaxSize().padding(horizontal=18.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        ScreenHeader("Timers","Multiple independent countdowns.","+ New",add)
        if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
            OutlinedButton(onClick=askNotify,modifier=Modifier.fillMaxWidth()) { Text("Enable timer notifications") }
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf(1,5,10,15,25,30,45,60).forEach { m ->
                AssistChip(onClick={val d=m*60000L;val t=ClockTimer(System.currentTimeMillis()+m,m.toString()+" min",d,d,0L,false);timers.add(t);saveTimers(context,timers)},label={Text(m.toString())})
            }
        }
        if(timers.isEmpty()) EmptyPanel("No active timers","Create a timer or start from a quick preset.")
        updated.forEach { t ->
            val remaining=if(t.running) (t.endAt-now).coerceAtLeast(0L) else t.remainingMs
            Card(colors=CardDefaults.cardColors(containerColor=Panel),modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(t.label,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text(if(t.running) "Running" else "Paused",style=MaterialTheme.typography.labelSmall,color=Color(0xFF9FB1C7)) }
                        Text(fmt(remaining),style=MaterialTheme.typography.headlineMedium,color=Accent)
                    }
                    LinearProgressIndicator(progress={if(t.totalMs>0) 1f-(remaining.toFloat()/t.totalMs).coerceIn(0f,1f) else 0f},modifier=Modifier.fillMaxWidth())
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        Button(onClick={
                            val n=if(t.running) t.copy(running=false,remainingMs=remaining,endAt=0L) else t.copy(running=true,remainingMs=remaining,endAt=now+remaining)
                            val idx=timers.indexOfFirst { it.id==t.id };if(idx>=0) timers[idx]=n
                            saveTimers(context,timers);scheduleTimer(context,n)
                        },modifier=Modifier.weight(1f)) { Text(if(t.running) "Pause" else "Start") }
                        OutlinedButton(onClick={val idx=timers.indexOfFirst { it.id==t.id };if(idx>=0) {val old=timers.removeAt(idx);cancelTimer(context,old);saveTimers(context,timers)}},modifier=Modifier.weight(1f)) { Text("Remove") }
                    }
                }
            }
        }
        Text("Timers continue in the background. Android will deliver a completion notification when the scheduled timer expires.",style=MaterialTheme.typography.labelSmall,color=Color(0xFF9FB1C7))
    }
}

@Composable
private fun TimerEditor(initial:ClockTimer?,onDismiss:()->Unit,onSave:(String,Long)->Unit) {
    var label by remember { mutableStateOf("Timer") }
    var hours by remember { mutableStateOf("0") }
    var minutes by remember { mutableStateOf("5") }
    var seconds by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest=onDismiss,title={Text("New timer")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value=label,onValueChange={label=it},label={Text("Timer name")},singleLine=true)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value=hours,onValueChange={hours=it.filter(Char::isDigit).take(2)},label={Text("Hours")},modifier=Modifier.weight(1f),singleLine=true)
                OutlinedTextField(value=minutes,onValueChange={minutes=it.filter(Char::isDigit).take(2)},label={Text("Minutes")},modifier=Modifier.weight(1f),singleLine=true)
                OutlinedTextField(value=seconds,onValueChange={seconds=it.filter(Char::isDigit).take(2)},label={Text("Seconds")},modifier=Modifier.weight(1f),singleLine=true)
            }
            if(error) Text("Enter a duration greater than zero. Minutes and seconds must be below 60.",color=Color(0xFFFFB4AB))
        }
    },confirmButton={TextButton(onClick={
        val h=hours.toLongOrNull()?:0L;val m=minutes.toLongOrNull()?:0L;val s=seconds.toLongOrNull()?:0L
        val valid=h<=99 && m<60 && s<60 && h*3600+m*60+s>0
        if(valid) onSave(label.ifBlank { "Timer" },(h*3600+m*60+s)*1000L) else error=true
    }) { Text("Create") }},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

@Composable
private fun EmptyPanel(title:String,subtitle:String) {
    Column(Modifier.fillMaxWidth().padding(vertical=28.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Text(title,style=MaterialTheme.typography.titleMedium)
        Text(subtitle,style=MaterialTheme.typography.bodySmall,color=Color(0xFF9FB1C7))
    }
}

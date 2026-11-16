# Nori Clock — App 03

Independent offline-first Android clock utility for Project-20.

## Features
- Alarm: multiple one-time or weekly alarms, labels, enable/disable, next occurrence display.
- World Clock: saved searchable cities, live local time, current UTC offset, DST-aware time-zone conversion.
- Stopwatch: start, pause, resume, reset, hundredth-second display, lap capture; elapsed state is persisted.
- Timer: multiple independent countdowns, named custom durations, quick presets, pause/resume, progress, background completion notifications.
- Local persistence using Android SharedPreferences; no account, analytics, network, or location access.

## Android behavior
- Uses AlarmManager for alarm and timer completion scheduling.
- Uses setAlarmClock when exact-alarm access is available; otherwise uses setAndAllowWhileIdle as a fallback. Android may delay fallback alarms under Doze.
- Android 13+ notification permission is requested by the user from the relevant screen.
- Exact alarm access can be enabled from the Alarm screen. The app does not silently grant itself access.
- Restores alarms and active timers after reboot.
- City times use IANA time-zone identifiers and platform time-zone rules.

## Build
`gradle :apps:app-03-clock:assembleDebug`

Application ID: `com.project20.clock`

## Known limitations in this first build
- Alarm delivery uses a high-importance notification rather than a full-screen ringing experience. Sound behavior follows Android notification-channel settings.
- City search uses a curated offline city list, not a global geocoder.
- Stopwatch state is stored locally; if Android kills the process while running, the elapsed duration is recovered from monotonic timestamps only while the same boot session remains active.
- No Nori account integration is included. Shared account architecture remains undecided.

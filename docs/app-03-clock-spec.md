# App 03 — Nori Clock

## Product objective

Build an independent native Android clock utility informed by the documented workflows of Samsung Clock. Match the broad interaction model and user-visible capabilities, not Samsung source code, trademarks, icons, proprietary artwork, or branding. The app must remain usable offline except for explicitly optional online enhancements.

Target reference device: Samsung Galaxy A12-class phone running Android 13. The implementation must also handle the project's supported Android range.

## Research basis

Primary references:
- Samsung support, "Use your Galaxy phone's Calculator, Calendar, and Clock apps": https://www.samsung.com/us/support/answer/ANS10002547/
- Samsung support, "Set and edit alarms on your Galaxy phone or tablet": https://www.samsung.com/us/support/answer/ANS10002623/
- Samsung Clock feature overview: https://www.samsung.com/ae/support/mobile-devices/what-are-the-features-of-the-samsung-clock/
- Samsung Clock Google Play listing: https://play.google.com/store/apps/details?id=com.sec.android.app.clockpackage
- Android Developers, "Schedule alarms": https://developer.android.com/develop/background-work/services/alarms
- Android 14 exact-alarm behavior: https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- Android 14 full-screen intent behavior: https://developer.android.com/about/versions/14/behavior-changes-14

Observed feature families:
1. Alarm: multiple alarms, one-time and recurring days, optional date assignment, labels, alarm tone and volume, vibration, configurable snooze interval/repeat count, enable/disable, delete, alarm background, sleep/bedtime schedule.
2. World Clock: searchable cities, multiple saved cities, local times, time-zone conversion, list/map presentation, optional weather and temperature units.
3. Stopwatch: start, pause, resume, reset, lap capture, hundredth-second display, copy lap results.
4. Timer: hour/minute/second entry up to the documented 99:59:59 range, start/pause/resume/reset, background operation, completion alert, named presets, multiple concurrent timers.
5. Settings: time display, alert sound/vibration, timer behavior, and permission controls.

Samsung feature availability and presentation can vary by app version, device, region, and software version. This project will prioritize stable documented workflows and verify details on the target phone.

## Delivery phases

### Phase 1 — core clock and local persistence
- Four primary destinations: Alarm, World Clock, Stopwatch, Timer.
- Live local clock using Android system time and time zone.
- Alarm list, add/edit/delete, enable/disable, label, one-time and weekly repeat.
- Timer creation, start, pause, resume, cancel, and completion notification.
- Stopwatch start, pause, resume, reset, and lap list.
- World Clock city list with time-zone-aware live clocks.
- Persistent local storage; state survives app process death and relaunch.
- Material 3 interface, dark/light theme, accessible controls, empty states, and input validation.

### Phase 2 — alarm reliability and advanced controls
- Exact alarm scheduling through AlarmManager for user-created alarms.
- Use the alarm-clock scheduling API for user-visible wake-up alarms.
- Handle Android 12+ exact-alarm access correctly; explain the permission before directing the user to system settings where needed.
- Handle Android 13+ notification permission.
- Alarm notification, ringing screen, snooze and dismiss actions.
- Sound selection from safe built-in/system-provided tones, configurable volume, vibration, configurable snooze interval and repeat count.
- Repeating-day alarms, one-off date alarms, missed-alarm state.
- Restore schedules after reboot; reschedule after time/time-zone changes and exact-alarm access changes.
- Timer completion alert and multiple simultaneous timers.
- No continuous polling service for clock/timer display; derive remaining time from monotonic/system timestamps and schedule system alarms for completion.

### Phase 3 — world clock and convenience
- City search backed by a bundled, attribution-compliant city/time-zone dataset.
- Add, remove, and reorder cities.
- Time-zone converter: choose a date/time in one city and show corresponding times in saved cities.
- Daylight-saving transitions handled through IANA time-zone identifiers.
- Stopwatch hundredth-second display and lap copy/share.
- Named timer presets and multiple active timers.
- Optional map presentation only if it can be implemented without unnecessary location collection or proprietary assets.
- Weather is deferred; it requires a data provider, network access, and a separate privacy decision.

### Phase 4 — polish and Nori ecosystem integration
- Alarm backgrounds chosen from user-provided local media, only if the permission and storage model are justified.
- Sleep schedule/reminder interface. Do not silently change Do Not Disturb, display, or system settings; any system action must be explicit, permission-aware, and reversible.
- Home-screen widgets where practical.
- Localization, accessibility review, and responsive layouts.
- Nori Account settings entry only when the shared Nori account broker and signing model are implemented. Do not display a fake connected state or store account passwords in Clock.
- Per-app connection permissions; Clock shares only explicitly approved data with other Nori apps.

## Technical architecture

- Independent Gradle module: `:apps:app-03-clock`.
- Application ID: `com.project20.clock`.
- Kotlin + Jetpack Compose + Material 3.
- Offline-first local repository. Use Room for alarms, timer presets, saved cities, and durable state; DataStore for simple preferences.
- Separate UI, domain logic, persistence, and Android scheduling/notification adapters.
- Use stable alarm IDs and immutable PendingIntents to avoid collisions between alarms and timers.
- Use `SystemClock.elapsedRealtime()` for in-process stopwatch/timer elapsed-time calculations. Persist enough wall-clock state to recover timers across process death and reboot, then reconcile carefully after reboot.
- Use `java.time` with core-library desugaring if required by the minimum supported Android version.
- Minimum SDK 23; target/compile SDK follow repository conventions.
- No account sign-in, analytics, or network access in the initial Clock milestone.

## Android reliability and permission constraints

- An alarm clock's core purpose requires precise delivery. Prefer the alarm-clock scheduling API and declare only the exact-alarm permission appropriate to a genuine alarm-clock use case. Do not assume permission is granted; check access, explain it, and handle denial without crashing.
- Android 13+ notification permission is runtime-controlled. Explain why notifications are needed and provide a recoverable settings path if denied.
- Full-screen alarm UI is subject to platform permission and policy. Request/use it only for an actual user-configured alarm, check whether it is allowed, and always provide a normal high-priority notification fallback.
- Restore scheduled alarms after reboot and after relevant system time/time-zone changes.
- Battery management, notification settings, muted volume, Do Not Disturb, and vendor-specific power management can affect audible delivery. Show clear diagnostics/help rather than promising delivery in every system state.
- Do not request location, contacts, calendar, microphone, or internet permission for the basic offline clock.

## Non-goals and constraints

- No Samsung trademarks, icons, proprietary artwork, source code, or claim of affiliation.
- No Bixby or Spotify integration.
- No cloud account or data synchronization in the first Clock milestone.
- No automatic control of Do Not Disturb, grayscale, power saving, or other system settings.
- Weather and map features are optional/deferred, not prerequisites for a reliable offline clock.
- Exact visual parity with every Samsung Clock version is not an acceptance criterion.

## Nori account integration contract

Project-20 is intended to become a connected Nori app ecosystem. Apps remain independently installable, but shared identity cannot be achieved by copying a local account screen into each app.

The ecosystem requires a separately designed Nori account broker/service, a stable shared signing certificate for release builds, explicit inter-app authorization, and a documented data-sharing API. Until that exists:
- Clock stores its own data locally.
- Clock must not invent a connected account state.
- The Clock settings design reserves a Nori Account section for a later real integration.
- Cross-app sharing is opt-in, scoped, revocable, and limited to the data the user approves.
- No passwords or long-lived credentials are shared between apps.

## Acceptance criteria

- App installs and launches on the target Samsung Android phone.
- All four primary sections open and operate.
- Current time and world clocks follow system time/time-zone changes.
- Alarm CRUD, repeat schedule, enable/disable, snooze, and dismiss work.
- A scheduled alarm fires at the intended time while the app is backgrounded and after device reboot, subject to documented system restrictions and granted permissions.
- Timer completion is delivered while app is backgrounded; multiple timers do not collide.
- Stopwatch laps are measured from a monotonic clock and remain internally consistent through pause/resume.
- City time conversion handles daylight-saving transitions.
- All user data persists after app restart.
- Permission denial is handled without crashes or misleading success messages.
- Existing TaskForge and Calendar modules still build.
- Mark the app complete only after CI build, automated checks, and physical-device verification pass.

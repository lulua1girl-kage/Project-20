# App 02 — Calendar

## Product objective
Build an independent, offline-first Android calendar whose interaction model covers the documented core workflows of Samsung Calendar. This is a functional reference implementation, not a brand, source-code, or asset clone.

## Reference research
Primary references:
- Samsung support: adding, editing, deleting, repeating, and sharing events.
- Samsung support: month navigation, date selection, current-day shortcut, alternate calendars, week numbers, and account synchronization.
- Samsung Galaxy Store listing: year/month/week/day/task views, recurrence patterns, time zones, widgets, and event notifications.
- Samsung support: configurable event alert styles.

Observed feature families:
1. Calendar navigation: year, month, week, day, and agenda/task-oriented views; swipe between periods; jump to a date; return to today; configurable week start and week numbers.
2. Event lifecycle: quick-add and detailed creation; edit; delete/trash; search; share as text or calendar file.
3. Event data: title, start/end date and time, all-day, location, description, calendar assignment, time zone, recurrence, and one or more alerts.
4. Calendar organization: multiple local calendars, colour, visibility, and account-backed calendars where provider integration is configured.
5. Convenience: home-screen widgets, launcher quick action, and event notifications.
6. Settings: first day of week, alternate calendar display where a suitable calendar implementation exists, week numbers, alert behavior, and display preferences.

Exact Samsung screens and options vary by device, region, app version, and connected account.

## Delivery phases
### Phase 1 — usable local calendar
- Month grid with current-day and selected-day indicators.
- Month navigation and Today action.
- Agenda list for the selected day.
- Add, edit, and delete events.
- Event title, start/end date and time, all-day, location, and notes.
- Local persistence; data survives process/app restarts.
- Empty states, validation, and basic accessibility labels.

### Phase 2 — navigation and event depth
- Week, day, and year views.
- Search events.
- Recurrence rules: daily, weekly, monthly, yearly, custom interval, and end conditions.
- Multiple alerts and time-zone-aware event editing.
- Event series operations (this occurrence / this and future / entire series).
- Undo or trash behavior.

### Phase 3 — Android integration
- Scheduled notifications using Android notification channels and appropriate runtime permissions.
- Calendar home-screen widget.
- Share event text and export/import iCalendar (.ics).
- Android Calendar Provider import/export integration only with explicit user permission.

### Phase 4 — organization and polish
- Multiple local calendars, colours, visibility filters.
- Week-number and first-day-of-week settings.
- Alternate calendar display only when a reliable library/data source is available.
- Dark theme, responsive layouts, accessibility and localization.
- Optional sync adapters only after a provider, authentication model, privacy policy, and conflict strategy are defined.

## Technical architecture
- Independent Gradle module: `:apps:app-02-calendar`.
- Kotlin + Jetpack Compose + Material 3.
- Offline-first local event repository. Prefer Room once the event/recurrence schema is introduced; do not store complex event data as ad-hoc UI state.
- UI state separated from persistence and scheduling.
- Unique application ID: `com.project20.calendar`.
- No network access or account sign-in in the initial milestone.

## Non-goals and constraints
- Do not use Samsung trademarks, icons, proprietary artwork, source code, or claim affiliation.
- Match interaction patterns and user-visible capabilities, not proprietary implementation.
- Samsung/Google/Microsoft account synchronization, shared-calendar invitations, weather, contact birthdays, and cloud conflict resolution are not implied by local functionality; they require explicit integrations.
- Exact parity across every Samsung device/software version is not a realistic acceptance criterion.

## Acceptance criteria
- App installs and launches on the target Android device.
- Month grid dates align correctly across month/year boundaries and leap years.
- Creating, editing, and deleting an event updates the calendar immediately.
- Events remain after force-close/relaunch.
- All-day and timed events display correctly.
- Invalid or empty event titles cannot be saved.
- Existing TaskForge build remains unaffected.
- Every phase is marked complete only after its tests and APK build pass.

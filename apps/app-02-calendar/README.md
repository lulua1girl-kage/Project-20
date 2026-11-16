# Calendar — App 02

An independent, offline-first Android calendar based on the documented core workflows of Samsung Calendar.

## Current implementation
- Month grid with selected-day and current-day states.
- Previous/next month navigation and Today shortcut.
- Agenda view.
- Create and edit timed or all-day events.
- Event title, time, location, and notes.
- Delete events.
- Local persistence across app restarts.

## Build
`gradle :apps:app-02-calendar:assembleDebug`

Application ID: `com.project20.calendar`

## Status
Phase 1 implementation is committed. A successful CI build and physical-device test are still required before this app can be marked complete. See [the full specification](../../../docs/app-02-calendar-spec.md).

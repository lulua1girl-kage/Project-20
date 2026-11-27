# UX Flow and Visual Direction

## Launch
Locally bundled splash art for about 2–3 seconds; skip/reduced-motion support. Initialize local database and cached app state in parallel. Dashboard must not wait for network. Sync in background; offline mode remains usable.

## Navigation
Today / Inbox / Projects / Calendar / Focus / Progress / Challenges / Settings.

## Today hierarchy
1. Date and simple greeting
2. Main priority / next action
3. Planned time blocks
4. Task list grouped by priority or time
5. Progress summary
6. Small dismissible quote panel

## Visual system
- Background #09090B
- Surface #151519
- Elevated surface #202026
- Dark red #8F1727
- Crimson accent #B3263A
- Primary text #F3E9EA
- Secondary text #A7A1A4
- UI typography: clear sans-serif; display serif only for occasional quote/certificate emphasis
- No neon overload, excessive glow, or ornamental clutter
- Motion purposeful and reduced-motion aware

## Required states
Every screen needs first-use, empty, loading, offline, recoverable error, and destructive confirmation states.

## Certificate
Preview -> validate rule -> issue unique certificate ID -> export/share -> optionally revoke public visibility. Do not include private task titles or personal schedules.

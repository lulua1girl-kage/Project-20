# MVP test plan

Automated checks cover Python unit tests, HTTP API integration, database CRUD, input validation, JS syntax and HTML parsing. Manual test checklist:
- Create task, refresh, complete, filter, delete
- Export tasks and inspect JSON
- Add due time, opt into notifications, verify while app active
- Start/pause/reset focus timer
- Install PWA and reload offline after first successful online load
- Test narrow phone viewport, keyboard controls and screen reader labels
- Verify no network requests to third-party analytics or paid APIs

Before multi-user/public release add: cross-account object authorization tests, admin privilege tests, token expiry/revocation, rate limits, audit logs, backup restore, MASVS/MASTG verification, and platform notification testing.

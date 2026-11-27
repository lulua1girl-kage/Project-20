# App 04 — P-Exceptional

**Status:** functional PWA/local API MVP v0.5 prototype; production native clients and authenticated multi-user service remain future phases.

P-Exceptional is an offline-first productivity client with a dark-red design system. It is independent from App 01 TaskForge and follows Project 20 shared identity direction.

## Delivered source archive
The current runnable source archive is attached in this conversation as `P-Exceptional-MVP-v0.5.zip`. The archive includes the responsive PWA, local API, SQLite store, opening artwork, 100 original principles, task capture and priority, filters, focus timer, optional due-time reminders, export, validated merge-only backup restore including daily review notes, capacity planner, tests and research notes.

## Run
Python 3.10+:
```bash
python backend/server.py
```
Then open `http://127.0.0.1:8765`.

## Current features
- Task capture, priority, project label, estimate, completion, deletion and All/Open/Done filters
- Browser localStorage persistence
- Optional due-time reminder with explicit notification permission (best-effort while app is active)
- 25-minute focus timer with pause/reset
- 100 original principles
- Capacity-aware daily workload (default 240 min, user-configurable 30–720; overload is a warning, never auto-reschedules), date-scoped daily review notes, progress metrics and JSON export
- Backup restore with app/schema validation, size and record caps, field allowlisting, duplicate-ID skipping, and merge-only behavior
- PWA shell cache and offline reload after first successful load
- SQLite API: `GET /api/health`, `GET/POST /api/tasks`, `PATCH/DELETE /api/tasks/{id}`
- Loopback-only API with Host/Origin checks, payload limits, CSP and security headers

## Validation
- GitHub Actions CI passed: Python API/UI tests, Node unit tests and JS syntax checks
- Operational/privacy/accessibility research memo added in `OPERATIONAL_RESEARCH.md`
- Local archive validation: 10/10 Python tests and 12/12 Node tests passed
- Node backup validation tests: 4/4 passed
- JavaScript syntax checks and ZIP integrity passed

## Limits
Browser task storage and SQLite API are not yet synchronized. This is a single local profile; no account registration, cloud sync, native reminder scheduling, Android/iOS/desktop binary, certificate issuer, or admin console is implemented. Do not expose the development API to a network. Production needs authentication, object-level authorization, rate limiting, audit, backup/restore, platform security verification and native notification adapters.

# App 04 — P-Exceptional

**Status:** functional PWA/local API MVP v0.2 (prototype); production native clients and authenticated multi-user service remain future phases.

P-Exceptional is an offline-first productivity client with a dark-red design system. It is independent from App 01 TaskForge and follows the Project 20 shared identity direction.

## MVP package
The complete runnable source archive is provided as `P-Exceptional-MVP-v0.2.zip` in the current delivery. The package contains:
- `web/`: responsive PWA, task capture, filters, local storage, quote rotation, focus timer, due-time reminder UI, service worker, manifest and local art
- `backend/`: loopback-only Python server and SQLite task CRUD API
- `tests/`: unit and HTTP API integration tests
- `docs/`: research decisions, operational/security test notes, and 100 original quotes

## Run
Python 3.10+:
```bash
python backend/server.py
```
Then open `http://127.0.0.1:8765`.

## MVP functionality
- Task capture, priority, completion, deletion and All/Open/Done filters
- Browser localStorage persistence
- Optional due-time reminder with explicit notification permission
- 25-minute focus timer with pause/reset
- 100 quote/principle rotation
- Progress metrics and JSON export
- PWA shell cache and offline reload after first successful load
- SQLite API: `GET /api/health`, `GET/POST /api/tasks`, `PATCH/DELETE /api/tasks/{id}`
- Loopback binding only, CSP and basic security headers

## Next implementation steps
1. Connect browser local store to SQLite API using durable outbox/idempotency.
2. Add project/task domain models, recurrence, calendar and timeboxing.
3. Implement native Android client inside Project 20 conventions.
4. Add cross-platform Kotlin domain and platform-specific local notification adapters.
5. Add authenticated Project 20 identity, per-user authorization, challenge publishing and certificates.
6. Complete threat model, MASVS/MASTG verification and platform release tests.

## Security boundary
This prototype is not a production multi-user server. It binds to localhost and must not be exposed to the internet. No public deployment until authentication, object-level authorization, rate limits, audit, backup/restore and security testing are implemented.

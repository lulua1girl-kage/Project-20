# Local API

Python standard-library HTTP server + SQLite. Binds only to `127.0.0.1`.

- `GET /api/health`
- `GET /api/tasks`
- `POST /api/tasks` (title, priority)
- `PATCH /api/tasks/{id}` (done boolean)
- `DELETE /api/tasks/{id}`

Development-only. No authentication or multi-user authorization; do not expose publicly. The browser client still uses localStorage and does not yet synchronize with the SQLite API.

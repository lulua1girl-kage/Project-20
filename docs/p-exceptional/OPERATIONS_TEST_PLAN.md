# P-Exceptional Operational Test Plan

## Functional
- Task/project CRUD, archive/restore, recurrence, search/filter/sort
- Offline edits and later sync
- Duplicate retry/idempotency
- Concurrent edit conflict behavior
- App kill/restart during writes and sync
- Database migrations and export/import round-trip
- Large task sets and recurring schedules

## Notifications
- Permission denied/enabled/revoked
- Reboot, timezone change, DST
- Battery saver/background restrictions
- Exact-alarm unavailable
- Duplicate/stale reminder cancellation
- Quiet hours and user pause
- Document OS delivery limitations

## Resilience
- No/slow network, timeout, 429/5xx, expired session
- Low storage, backup restore, migration failure
- App update with pending sync
- Cold start to cached dashboard
- Crash-free startup and recovery after process termination

## Authorized non-destructive security checks
- Unauthenticated access denied
- User A cannot access User B records by changing IDs
- Ordinary user cannot invoke admin endpoints
- Revoked/expired tokens rejected
- Rate limits and login backoff verified
- Invalid/oversized payloads rejected
- Secrets absent from client binaries, logs, and Git history
- Admin actions audited
- Export links expire and are requester-scoped
- Static analysis, dependency audit, secret scan, API tests

## Release gates
Domain unit tests; database/sync integration tests; UI smoke tests; physical-device notification checks where possible; accessibility review; backup/restore drill; privacy notice and data inventory review; documented known issues. Do not claim independent certification without assessment.

# P-Exceptional Research Notes

## Product references reviewed
- Super Productivity: https://github.com/super-productivity/super-productivity — capture, daily planning, focus methods, time tracking, local/offline data, project/subtask/tag organization and broad platform support.
- Vikunja: https://github.com/go-vikunja/vikunja — self-hosted task/project management, API and frontend; AGPL/GPL license boundary must be reviewed before code reuse.
- Tasks.org: https://tasks.org/ — further reference for Android task/reminder and open sync formats; deep repository review pending.
- Offline-first sync example: https://github.com/Rashikapoonacha/Task-Board — local source of truth, durable outbox, retries and conflict resolution described in README.

## Architecture sources
- Compose Multiplatform: https://github.com/JetBrains/compose-multiplatform — Android, iOS and desktop (Windows/macOS/Linux); web is beta.
- Android offline-first architecture: https://developer.android.com/topic/architecture/data-layer/offline-first
- Android AlarmManager: https://developer.android.com/develop/background-work/services/alarms
- Apple local notifications: https://developer.apple.com/documentation/UserNotifications/scheduling-a-notification-locally-from-your-app

## Security and privacy sources
- OWASP MAS: https://owasp.org/projects/mobile-application-security
- MASVS: https://mas.owasp.org/MASVS/02-Frontispiece/
- MASVS control usage: https://mas.owasp.org/MASVS/03-Using_the_MASVS/
- OWASP API Security: https://owasp.org/projects/api-security-project
- Supabase RLS: https://supabase.com/docs/guides/database/postgres/row-level-security
- WCAG mobile guidance: https://www.w3.org/TR/wcag2mobile-22/
- MDN mobile accessibility: https://developer.mozilla.org/en-US/docs/Web/Accessibility/Guides/Mobile_accessibility_checklist

## Key decisions
- Offline/local data first; online sync must be asynchronous and conflict-aware.
- Notifications must be native scheduled local notifications in production; web timers are not dependable when suspended.
- Per-user object authorization is mandatory for every task/project ID.
- Admin analytics are aggregate by default; sensitive individual activity requires explicit disclosure, limited purpose and access audit.
- Core functionality must not depend on paid APIs.
- Do not adopt source code until its license and attribution are reviewed.

## Research counts
The requested 200 productivity websites, 100 repositories, 150 operational sources, 300 security sources and 50 trend sources remain in progress. This file lists sources actually reviewed; it does not inflate counts from search results.

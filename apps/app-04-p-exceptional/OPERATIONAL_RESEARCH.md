# P-Exceptional — operational, privacy and accessibility research

Reviewed 2026-11-27. Official platform and standards sources checked; full requested source-count targets remain open.

## Operational decisions
- Core task, project, estimate, plan and daily-review data must be local-first and render without waiting for network sync.
- Native Android sync should use persistent WorkManager jobs, unique work, constraints, retries and exponential backoff.
- Android reminders require runtime notification permission on Android 13+; exact alarms require special access in applicable cases and should be used only for genuinely precise user-facing actions.
- iOS reminders use system UserNotifications local scheduling.
- Request notification permission when the user first enables/schedules a reminder, explain why, and provide settings to disable it.
- Export, merge-only restore, schema versioning and migration tests are reliability requirements.
- Admin metrics default to aggregate operational data; task content and individual usage timelines are not default analytics.

## Privacy and store disclosures
- Maintain an inventory of account identifiers, task/project/review content, diagnostics, notification settings, admin activity and optional telemetry.
- No third-party analytics or advertising SDK is included in the MVP.
- Review future SDK collection/sharing and update privacy policy and store disclosures.
- Provide in-app privacy notice, export, account deletion and associated-data deletion before account release.
- Admin access must be role/purpose limited and audited.

## Accessibility and engagement
- WCAG 2.2 AA baseline: keyboard navigation, visible focus, contrast, reflow, target size, labels, status announcements and reduced motion.
- Color alone must not communicate priority or completion; preserve focus outlines on dark-red surfaces.
- Keep reminders configurable. Avoid shame, streak-loss pressure, fake urgency, confusing opt-outs and manipulative reward loops.
- Engagement should come from useful feedback, clarity and meaningful user-controlled milestones.

## Security verification
- Mobile: OWASP MASVS/MASTG control groups for storage, crypto, auth, network, platform, code, resilience and privacy.
- Web/backend: OWASP ASVS and API Security Top 10; object-level authorization for user-owned records, function-level authorization and audit for admin actions.
- Enforce request-size limits, rate limits, pagination, bounded imports, allowlisted fields, safe file handling, dependency scanning and no client secrets.
- Current local API is loopback-only and rejects hostile Host/Origin and oversized payloads; it is not production-ready or multi-user secure.

## Sources
- Android WorkManager: https://developer.android.com/develop/background-work/background-tasks/persistent
- Android retry/backoff: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work
- Android notification permission: https://developer.android.com/develop/ui/compose/notifications/notification-permission
- Android exact alarms: https://developer.android.com/develop/background-work/services/alarms
- Apple local notification scheduling: https://developer.apple.com/documentation/usernotifications/scheduling-a-notification-locally-from-your-app
- Apple notification authorization: https://developer.apple.com/documentation/usernotifications/asking-permission-to-use-notifications
- Google Play Data safety: https://support.google.com/googleplay/android-developer/answer/10144311
- Apple App Privacy: https://developer.apple.com/app-store/app-privacy-details/
- WCAG 2.2: https://www.w3.org/WAI/WCAG22/Understanding/
- WCAG 2.2 target size/focus: https://www.w3.org/WAI/standards-guidelines/wcag/new-in-22/
- FTC dark patterns report: https://www.ftc.gov/system/files/ftc_gov/pdf/P214800%20Dark%20Patterns%20Report%209.14.2022%20-%20FINAL.pdf
- OWASP ASVS: https://asvs.owasp.org/
- OWASP MASVS: https://mas.owasp.org/MASVS/
- OWASP API Security Top 10: https://api-security.owasp.org/editions/2023/en/0x11-t10/

## Status
This memo records decisions supported by these sources. The full 150 operational, 300 security and 50 trend-source targets remain in progress and will be tracked source by source, not inferred from search-result counts.

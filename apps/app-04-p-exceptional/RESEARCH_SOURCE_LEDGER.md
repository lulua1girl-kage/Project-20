# Research source ledger — P-Exceptional

Updated 2026-11-27. This ledger records sources actually consulted. It does not represent the requested 200/100/150/300/50 source targets as completed.

## Offline-first and synchronization
- Android Developers, Build an offline-first app: https://developer.android.com/topic/architecture/data-layer/offline-first
  - Local source of truth, offline reads/writes, queues, retry/backoff, synchronization and conflict-resolution concerns.
- Android Developers, Data layer: https://developer.android.com/topic/architecture/data-layer
  - Repository responsibilities and single source of truth.

## Native reminders
- Android Developers, Schedule alarms: https://developer.android.com/develop/background-work/services/alarms
  - Exact alarm special access and use only when precision is genuinely user-facing.
- Apple Developer, Schedule local notifications: https://developer.apple.com/documentation/usernotifications/scheduling-a-notification-locally-from-your-app
  - OS-managed local notification delivery and permission model.

## Security and privacy baseline
- OWASP MAS: https://owasp.org/projects/mobile-application-security
- OWASP MASVS: https://mas.owasp.org/MASVS/02-Frontispiece/
- OWASP MASVS usage: https://mas.owasp.org/MASVS/03-Using_the_MASVS/
- OWASP API Security Project: https://owasp.org/www-project-api-security/

## Repository and competitor review queue
- Super Productivity: https://github.com/super-productivity/super-productivity
- Vikunja: https://github.com/go-vikunja/vikunja
- Tasks.org: https://tasks.org/
- Compose Multiplatform: https://github.com/JetBrains/compose-multiplatform

## Research quality rule
For every candidate source/repository, record date checked, product scope, evidence observed, implementation decision, license, maintenance status, and confidence. Search result counts are not equivalent to sources reviewed. Repository code must not be copied before license and security review.

## Additional security sources checked
- OWASP MASVS control groups: https://mas.owasp.org/MASVS/
  - Storage, crypto, authentication, network, platform, code, resilience and privacy controls.
- OWASP API Security Top 10 2023: https://api-security.owasp.org/editions/2023/en/0x11-t10/
  - Object-level authorization, authentication, property authorization, resource limits, function-level authorization and security misconfiguration.
- MDN Secure Contexts: https://developer.mozilla.org/en-US/docs/Web/Security/Defenses/Secure_Contexts
  - Localhost/loopback can be treated as potentially trustworthy for local PWA development; service workers still require a secure context.
- MDN Service Workers: https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API/Using_Service_Workers
  - HTTPS requirement for deployed service workers and localhost exception for development.

## MVP hardening added
- API validates Host against the expected loopback host/port.
- API rejects cross-origin Origin headers on loopback endpoints.
- Integration checks verify hostile Origin and Host rejection and confirm no task mutation.
- This mitigates browser-based DNS-rebinding/CSRF risks for this local development server; it is not a substitute for production authentication or authorization.

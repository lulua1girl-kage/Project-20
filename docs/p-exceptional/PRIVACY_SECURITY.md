# P-Exceptional Privacy and Security Requirements

## Assets
Accounts/sessions; task and schedule data; habits/focus history; challenge participation; certificates; admin privileges; sync API and file storage.

## Minimum controls
- Server-side authorization on every resource; never trust client-supplied owner IDs
- Shared Project 20 identity; short-lived access sessions, refresh rotation/revocation
- MFA for owner/admin; safe recovery
- Rate limits on authentication, sync, exports, challenge publishing, and certificate APIs
- TLS; platform secure storage for tokens; no service-role keys in clients
- Input validation, output encoding, parameterized SQL, upload limits
- Least privilege and separated user/admin roles
- Audit admin actions and role changes
- Dependency pinning, vulnerability/secret scanning, SBOM, signed releases
- Encrypted backups, restore drills, migration rollback
- Web CSP/security headers and CSRF protections where applicable
- Data export/deletion lifecycle and documented retention

## Privacy analytics
- Aggregate metrics by default
- No task text, notes, keystrokes, contacts, or precise location collection
- No private-task-content dashboard or productivity score
- Per-user usage timelines require clear disclosure, purpose, narrow access, audit, and short retention
- Users can review data categories and opt out of optional analytics

## Cookies and third parties
Native apps should explain app storage/session tokens separately from browser cookies. Web/PWA must list cookie name, purpose, first/third-party status, expiry, provider, essential/optional classification, and opt-out. Optional analytics are blocked until consent. No deceptive consent UI.

## Verification
Use OWASP MASVS/MASTG for mobile controls and test cases; map backend/API requirements to OWASP ASVS and API security testing. No product is unhackable; reduce attack surface, detect failures, and respond safely.

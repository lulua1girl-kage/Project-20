# Nori Account — Cross-App Architecture

## Objective

Provide one user-controlled Nori identity and shared settings across independently installed Project-20 apps, without making app data automatically visible to every other app.

## Important platform constraint

Android applications normally run in separate sandboxes. A shared Kotlin library can share code at build time, but it does not create shared runtime storage or a shared login when apps are installed independently.

A real shared local account therefore needs a trusted broker component or another explicitly designed identity provider. The broker must own the canonical local account profile and mediate access. Apps must never exchange passwords or copy private account databases.

## Proposed model

1. **Nori Account Broker:** a small Android application/service that owns the local Nori identity, profile preferences, app connection registry, and authorization decisions.
2. **Client apps:** each Nori app remains independently installable and requests only the account fields or shared capabilities it needs.
3. **Authorization:** users approve each app connection and each data scope. Access can be revoked.
4. **Offline-first:** the local identity works without a network connection. Optional cloud identity/sync is a separate later design.
5. **Data ownership:** each app remains the source of truth for its own private data. Cross-app sharing uses explicit APIs, not shared database files.
6. **Shared settings:** only genuinely global preferences (such as theme or display language) are centralized; app-specific settings remain local.

## Android security requirements

- Use a stable release signing certificate across the broker and all client apps.
- Protect exported broker components with signature-level permissions and validate calling package identity.
- Do not trust a package name alone as proof of identity.
- Use short-lived, scoped grants/tokens where appropriate; never expose long-lived secrets to client apps.
- Validate every request and return only the approved fields.
- Provide a visible connected-app list, per-app scopes, revocation, and account reset/export controls.
- Do not use deprecated shared-UID mechanisms.
- Do not store passwords in app preferences or pass credentials between apps.
- Debug builds intended to test cross-app calls must be signed consistently. CI-generated debug signing keys that differ between workflow runs cannot validate signature-protected communication across separately built APKs. Use a controlled test/release keystore stored as a protected CI secret; never commit the keystore or passwords to the repository.

## Rollout

### Stage A — contract and threat model
- Define account profile fields, connection states, scopes, request/response schemas, revocation, and recovery.
- Identify the broker app/module and its lifecycle in the 20-app roadmap.
- Decide whether the broker is one of the 20 apps or a shared companion app.

### Stage B — local broker
- Create local account without requiring internet.
- Store identity and preferences securely.
- Expose a signature-protected API.
- Add client connection UI and explicit consent.

### Stage C — integrate apps
- Integrate one app at a time.
- Verify denied/revoked permissions, wrong-signature clients, broker unavailable, and corrupted/missing account state.
- Keep all apps fully usable without connecting the Nori account.

### Stage D — optional synchronization
- Only after defining provider, authentication, encryption, conflict resolution, export/deletion, privacy notice, and account recovery.
- No cloud sync is implied by the local account.

## Current status

Architecture proposal only. No app should show itself as connected until the broker and shared signing process exist and the integration has passed cross-app tests.

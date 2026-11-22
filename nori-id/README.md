# Nori ID — Local Foundation 0.1.0

Nori ID is the shared identity and access-control foundation planned for Project-20 and future Nori applications. This first implementation is local-first: the API and SQLite database run on the owner's PC. It is not a public identity provider and must not be exposed to the internet.

## Included
- Country and territory numbering metadata from Google's libphonenumber through the maintained Python port.
- Country/territory names, calling codes and flag emoji.
- E.164 normalization and numbering-plan validation. A valid format does not prove ownership.
- Development-only phone challenge flow. A test code is returned only when development mode and the explicit development OTP switch are enabled.
- Registration after challenge verification, unique Nori handles, Argon2id password hashing and short-lived signed access tokens.
- SQLite persistence, account/security activity events, account suspension and app-specific access policies.
- One-time local owner bootstrap and a browser registration/control interface.
- Automated API tests and CI workflow.

## Not included yet
Real SMS delivery, carrier/SIM ownership checks, production recovery, guardian invitation/consent workflows, distributed rate limiting, hosted operations or production key management. The local prototype is not a production identity provider.

## Requirements
Python 3.11 or newer on Windows, macOS or Linux.

## Run on Windows PowerShell
From the repository root:

~~~powershell
cd nori-id
py -3 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -e ".[dev]"
Copy-Item .env.example .env
~~~

Edit .env and set a unique NORI_SECRET_KEY (at least 32 characters) and a different NORI_BOOTSTRAP_TOKEN. Keep .env private; never commit it.

Start the service:

~~~powershell
uvicorn nori_id.main:app --host 127.0.0.1 --port 8765
~~~

Open http://127.0.0.1:8765. Use the one-time bootstrap token to create the local owner account, then sign in.

Default database: nori-id/data/nori_id.sqlite3. Set NORI_DATA_DIR to choose another local folder. The service binds to loopback by default. Do not expose the port or change the bind address for public access.

## Development verification
The sample uses a development-only code and does not send SMS. The code is returned by the challenge endpoint only when NORI_ENV=development and NORI_ALLOW_DEV_OTP=true. Never enable this setting in a deployed environment. A production deployment must use a verified SMS provider and must never return OTP values to clients.

## API
- GET /api/v1/health
- GET /api/v1/phone/regions
- POST /api/v1/phone/challenges
- POST /api/v1/phone/verify-preview
- POST /api/v1/accounts/register
- POST /api/v1/auth/login
- GET /api/v1/me
- GET /api/v1/apps
- GET /api/v1/apps/{app_key}/access
- POST /api/v1/admin/bootstrap (one time)
- GET /api/v1/admin/accounts
- PATCH /api/v1/admin/accounts/{account_id}/status
- GET /api/v1/admin/activity
- PUT /api/v1/admin/apps/{app_key}/policy
- POST /api/v1/admin/accounts/{account_id}/apps/{app_key}/grant

Interactive API docs: http://127.0.0.1:8765/docs.

## Data and privacy
The SQLite file stays on the PC. Passwords are Argon2id hashes, never plaintext. OTP values and bearer tokens are not written to activity logs. The owner console is limited to account/security events and app access administration; it is not a facility for reading private app content. Use fictional test identities until production security, privacy and legal reviews are complete.

SQLite is not encrypted by default. Protect the Windows user account and local disk, keep backups private, and do not store real user data in this prototype.

## Production boundary
A hosted deployment will require HTTPS, real SMS delivery, secure secret management, migrations, tested backups, monitoring, abuse/rate-limit controls, account recovery, guardian-consent workflows, and security/privacy review. Nori clients must integrate through the versioned API and must never connect directly to SQLite.

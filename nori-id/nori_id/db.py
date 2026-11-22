from __future__ import annotations

import sqlite3
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


SCHEMA = """
PRAGMA foreign_keys = ON;
CREATE TABLE IF NOT EXISTS accounts (
 id TEXT PRIMARY KEY, phone_e164 TEXT UNIQUE, nori_handle TEXT NOT NULL UNIQUE COLLATE NOCASE,
 first_name TEXT NOT NULL, last_name TEXT NOT NULL, date_of_birth TEXT, gender TEXT,
 password_hash TEXT NOT NULL, role TEXT NOT NULL DEFAULT 'user' CHECK(role IN ('owner','admin','user')),
 status TEXT NOT NULL DEFAULT 'active' CHECK(status IN ('active','suspended')), created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS phone_challenges (
 id TEXT PRIMARY KEY, phone_e164 TEXT NOT NULL, code_digest TEXT NOT NULL, expires_at TEXT NOT NULL,
 attempts INTEGER NOT NULL DEFAULT 0, consumed_at TEXT, created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_phone_challenges_phone ON phone_challenges(phone_e164,created_at);
CREATE TABLE IF NOT EXISTS activity_events (
 id INTEGER PRIMARY KEY AUTOINCREMENT, account_id TEXT, event_type TEXT NOT NULL,
 details_json TEXT NOT NULL DEFAULT '{}', created_at TEXT NOT NULL,
 FOREIGN KEY(account_id) REFERENCES accounts(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_activity_created ON activity_events(created_at DESC);
CREATE TABLE IF NOT EXISTS app_policies (
 app_key TEXT PRIMARY KEY, display_name TEXT NOT NULL, enabled INTEGER NOT NULL DEFAULT 1,
 registration_mode TEXT NOT NULL DEFAULT 'invite' CHECK(registration_mode IN ('open','invite','closed')),
 min_age INTEGER NOT NULL DEFAULT 0 CHECK(min_age BETWEEN 0 AND 120),
 guardian_required INTEGER NOT NULL DEFAULT 0, updated_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS app_grants (
 account_id TEXT NOT NULL, app_key TEXT NOT NULL, granted_by TEXT NOT NULL, created_at TEXT NOT NULL,
 PRIMARY KEY(account_id,app_key),
 FOREIGN KEY(account_id) REFERENCES accounts(id) ON DELETE CASCADE,
 FOREIGN KEY(granted_by) REFERENCES accounts(id)
);
CREATE TABLE IF NOT EXISTS guardian_approvals (
 account_id TEXT NOT NULL, app_key TEXT NOT NULL, guardian_account_id TEXT NOT NULL, approved_at TEXT NOT NULL,
 PRIMARY KEY(account_id,app_key),
 FOREIGN KEY(account_id) REFERENCES accounts(id) ON DELETE CASCADE,
 FOREIGN KEY(guardian_account_id) REFERENCES accounts(id)
);
"""


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def connect(path: Path) -> sqlite3.Connection:
    path.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(path, timeout=10, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    conn.execute("PRAGMA journal_mode = WAL")
    conn.execute("PRAGMA busy_timeout = 10000")
    return conn


def initialize(path: Path) -> None:
    conn = connect(path)
    try:
        conn.executescript(SCHEMA)
        now = utc_now()
        conn.executemany(
            """INSERT OR IGNORE INTO app_policies
               (app_key,display_name,enabled,registration_mode,min_age,guardian_required,updated_at)
               VALUES(?,?,?,?,?,?,?)""",
            [
                ("taskforge", "TaskForge", 1, "open", 0, 0, now),
                ("calendar", "Nori Calendar", 1, "invite", 0, 0, now),
                ("clock", "Nori Clock", 1, "invite", 0, 0, now),
            ],
        )
        conn.commit()
    finally:
        conn.close()


def record_event(conn: sqlite3.Connection, event_type: str, account_id: str | None = None,
                 details: dict[str, Any] | None = None) -> None:
    import json
    conn.execute(
        "INSERT INTO activity_events(account_id,event_type,details_json,created_at) VALUES(?,?,?,?)",
        (account_id, event_type, json.dumps(details or {}, separators=(",", ":")), utc_now()),
    )


def calculate_age(date_of_birth: str) -> int:
    today = datetime.now(timezone.utc).date()
    born = datetime.strptime(date_of_birth, "%Y-%m-%d").date()
    return today.year - born.year - ((today.month, today.day) < (born.month, born.day))

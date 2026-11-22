from __future__ import annotations

import hmac
import json
import secrets
import sqlite3
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Generator

from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.responses import FileResponse
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field

from .config import Settings
from .db import calculate_age, connect, initialize, record_event, utc_now
from .phones import parse_and_validate_phone, region_catalog
from .security import (
    create_access_token, decode_access_token, hash_password, normalize_handle,
    otp_digest, password_feedback, random_otp, verify_password,
)


class ChallengeRequest(BaseModel):
    phone_number: str = Field(min_length=5, max_length=32)
    region: str | None = Field(default=None, min_length=2, max_length=2)


class VerifyRequest(BaseModel):
    challenge_id: str
    verification_code: str = Field(pattern=r"^\d{6}$")


class RegisterRequest(VerifyRequest):
    first_name: str = Field(min_length=1, max_length=60)
    last_name: str = Field(min_length=1, max_length=60)
    date_of_birth: str
    gender: str | None = Field(default=None, max_length=40)
    password: str = Field(min_length=1, max_length=256)
    nori_handle: str = Field(min_length=3, max_length=24)


class LoginRequest(BaseModel):
    nori_handle: str
    password: str


class BootstrapRequest(BaseModel):
    nori_handle: str = Field(min_length=3, max_length=24)
    first_name: str = Field(min_length=1, max_length=60)
    last_name: str = Field(min_length=1, max_length=60)
    password: str = Field(min_length=12, max_length=256)


class StatusRequest(BaseModel):
    status: str


class AppPolicyRequest(BaseModel):
    display_name: str = Field(min_length=1, max_length=80)
    enabled: bool = True
    registration_mode: str = "invite"
    min_age: int = Field(default=0, ge=0, le=120)
    guardian_required: bool = False


bearer = HTTPBearer(auto_error=False)


def create_app(
    database_path: str | Path | None = None,
    *,
    settings: Settings | None = None,
    secret_key: str | None = None,
    bootstrap_token: str | None = None,
    environment: str | None = None,
    allow_dev_otp: bool | None = None,
) -> FastAPI:
    base = settings or Settings.from_env()
    db_path = Path(database_path) if database_path else base.database_path
    cfg = Settings(
        data_dir=db_path.parent,
        secret_key=secret_key or base.secret_key,
        bootstrap_token=bootstrap_token if bootstrap_token is not None else base.bootstrap_token,
        environment=environment or base.environment,
        allow_dev_otp=base.allow_dev_otp if allow_dev_otp is None else allow_dev_otp,
        access_token_minutes=base.access_token_minutes,
    )
    if len(cfg.secret_key) < 32:
        raise RuntimeError("NORI_SECRET_KEY must contain at least 32 characters.")
    initialize(db_path)

    app = FastAPI(
        title="Nori ID",
        version="0.1.0",
        description="Local-first identity and access-control API. Not a production identity provider.",
    )
    app.state.settings = cfg
    app.state.database_path = db_path
    static_dir = Path(__file__).parent / "static"
    app.mount("/static", StaticFiles(directory=static_dir), name="static")

    def db_dependency() -> Generator[sqlite3.Connection, None, None]:
        conn = connect(db_path)
        try:
            yield conn
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        finally:
            conn.close()

    def current_account(
        credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
        conn: sqlite3.Connection = Depends(db_dependency),
    ) -> sqlite3.Row:
        if credentials is None:
            raise HTTPException(status_code=401, detail="Sign in is required.")
        try:
            claims = decode_access_token(credentials.credentials, cfg.secret_key)
        except ValueError:
            raise HTTPException(status_code=401, detail="Your session is invalid or expired.")
        account = conn.execute("SELECT * FROM accounts WHERE id=?", (claims["sub"],)).fetchone()
        if account is None or account["status"] != "active":
            raise HTTPException(status_code=401, detail="This account is unavailable.")
        return account

    def admin_account(account: sqlite3.Row = Depends(current_account)) -> sqlite3.Row:
        if account["role"] not in {"owner", "admin"}:
            raise HTTPException(status_code=403, detail="Administrator permission is required.")
        return account

    @app.get("/", include_in_schema=False)
    def home():
        return FileResponse(static_dir / "index.html")

    @app.get("/api/v1/health")
    def health():
        return {"service": "nori-id", "version": "0.1.0", "status": "local-development"}

    @app.get("/api/v1/phone/regions")
    def phone_regions():
        return {"regions": region_catalog()}

    @app.post("/api/v1/phone/challenges", status_code=201)
    def create_phone_challenge(payload: ChallengeRequest, conn: sqlite3.Connection = Depends(db_dependency)):
        if not (cfg.environment == "development" and cfg.allow_dev_otp):
            raise HTTPException(status_code=503, detail="No SMS provider is configured. Real phone verification is unavailable.")
        try:
            phone = parse_and_validate_phone(payload.phone_number, payload.region)
        except ValueError as exc:
            raise HTTPException(status_code=422, detail=str(exc))
        if conn.execute("SELECT 1 FROM accounts WHERE phone_e164=?", (phone["phone_e164"],)).fetchone():
            raise HTTPException(status_code=409, detail="This phone number is already associated with a Nori account.")
        recent = conn.execute(
            "SELECT COUNT(*) FROM phone_challenges WHERE phone_e164=? AND created_at>?",
            (phone["phone_e164"], (datetime.now(timezone.utc) - timedelta(hours=1)).isoformat()),
        ).fetchone()[0]
        if recent >= 3:
            raise HTTPException(status_code=429, detail="Too many verification requests. Try again later.")
        now = datetime.now(timezone.utc)
        challenge_id, code = str(uuid.uuid4()), random_otp()
        conn.execute(
            "INSERT INTO phone_challenges(id,phone_e164,code_digest,expires_at,created_at) VALUES(?,?,?,?,?)",
            (challenge_id, phone["phone_e164"], otp_digest(cfg.secret_key, challenge_id, code),
             (now + timedelta(minutes=10)).isoformat(timespec="seconds"), now.isoformat(timespec="seconds")),
        )
        record_event(conn, "phone_challenge_created", details={"challenge_id": challenge_id})
        return {
            "challenge_id": challenge_id, "phone": phone, "expires_in_seconds": 600,
            "delivery": "development-only", "development_code": code,
        }

    def verify_challenge(conn: sqlite3.Connection, challenge_id: str, code: str) -> sqlite3.Row:
        challenge = conn.execute("SELECT * FROM phone_challenges WHERE id=?", (challenge_id,)).fetchone()
        if challenge is None or challenge["consumed_at"]:
            raise HTTPException(status_code=400, detail="Verification challenge is missing or already used.")
        if datetime.fromisoformat(challenge["expires_at"]) < datetime.now(timezone.utc):
            raise HTTPException(status_code=400, detail="Verification code expired. Request a new one.")
        if challenge["attempts"] >= 5:
            raise HTTPException(status_code=429, detail="Too many incorrect verification attempts.")
        expected = otp_digest(cfg.secret_key, challenge_id, code)
        if not hmac.compare_digest(challenge["code_digest"], expected):
            conn.execute("UPDATE phone_challenges SET attempts=attempts+1 WHERE id=?", (challenge_id,))
            record_event(conn, "phone_verification_failed", details={"challenge_id": challenge_id})
            conn.commit()
            raise HTTPException(status_code=400, detail="Incorrect verification code.")
        return challenge

    @app.post("/api/v1/phone/verify-preview")
    def verify_preview(payload: VerifyRequest, conn: sqlite3.Connection = Depends(db_dependency)):
        challenge = verify_challenge(conn, payload.challenge_id, payload.verification_code)
        return {"verified": True, "phone_e164": challenge["phone_e164"],
                "note": "This local preview does not replace real SMS ownership verification."}

    @app.post("/api/v1/accounts/register", status_code=201)
    def register(payload: RegisterRequest, conn: sqlite3.Connection = Depends(db_dependency)):
        challenge = verify_challenge(conn, payload.challenge_id, payload.verification_code)
        try:
            handle = normalize_handle(payload.nori_handle)
            dob = datetime.strptime(payload.date_of_birth, "%Y-%m-%d").date()
        except ValueError as exc:
            raise HTTPException(status_code=422, detail=str(exc))
        if dob >= datetime.now(timezone.utc).date():
            raise HTTPException(status_code=422, detail="Date of birth must be in the past.")
        if not password_feedback(payload.password)["acceptable"]:
            raise HTTPException(status_code=422, detail="Use a password of at least 12 characters that is not a common password.")
        if conn.execute("SELECT 1 FROM accounts WHERE nori_handle=? COLLATE NOCASE", (handle,)).fetchone():
            raise HTTPException(status_code=409, detail="That Nori address is already taken.")
        if conn.execute("SELECT 1 FROM accounts WHERE phone_e164=?", (challenge["phone_e164"],)).fetchone():
            raise HTTPException(status_code=409, detail="This phone number is already registered.")
        account_id = str(uuid.uuid4())
        conn.execute(
            """INSERT INTO accounts
               (id,phone_e164,nori_handle,first_name,last_name,date_of_birth,gender,password_hash,created_at)
               VALUES(?,?,?,?,?,?,?,?,?)""",
            (account_id, challenge["phone_e164"], handle, payload.first_name.strip(),
             payload.last_name.strip(), dob.isoformat(), payload.gender,
             hash_password(payload.password), utc_now()),
        )
        conn.execute("UPDATE phone_challenges SET consumed_at=? WHERE id=?", (utc_now(), payload.challenge_id))
        record_event(conn, "account_registered", account_id, {"nori_address": f"{handle}@nori"})
        token = create_access_token(account_id, "user", cfg.secret_key, cfg.access_token_minutes)
        return {
            "account": {"id": account_id, "nori_address": f"{handle}@nori", "first_name": payload.first_name.strip()},
            "access_token": token, "token_type": "bearer", "expires_in": cfg.access_token_minutes * 60,
        }

    @app.post("/api/v1/auth/login")
    def login(payload: LoginRequest, conn: sqlite3.Connection = Depends(db_dependency)):
        handle = payload.nori_handle.strip().lower()
        if handle.endswith("@nori"):
            handle = handle[:-5]
        account = conn.execute("SELECT * FROM accounts WHERE nori_handle=? COLLATE NOCASE", (handle,)).fetchone()
        if account is None or not verify_password(account["password_hash"], payload.password):
            record_event(conn, "login_failed", details={"nori_handle": handle[:24]})
            conn.commit()
            raise HTTPException(status_code=401, detail="Nori address or password is incorrect.")
        if account["status"] != "active":
            raise HTTPException(status_code=403, detail="This account is suspended.")
        record_event(conn, "login_succeeded", account["id"])
        token = create_access_token(account["id"], account["role"], cfg.secret_key, cfg.access_token_minutes)
        return {"access_token": token, "token_type": "bearer", "expires_in": cfg.access_token_minutes * 60}

    @app.get("/api/v1/me")
    def me(account: sqlite3.Row = Depends(current_account)):
        return {
            "id": account["id"], "nori_address": f"{account['nori_handle']}@nori",
            "first_name": account["first_name"], "last_name": account["last_name"],
            "date_of_birth": account["date_of_birth"], "role": account["role"], "status": account["status"],
        }

    @app.get("/api/v1/apps")
    def list_apps(conn: sqlite3.Connection = Depends(db_dependency)):
        rows = conn.execute("SELECT * FROM app_policies ORDER BY display_name").fetchall()
        return {"apps": [dict(row) for row in rows]}

    @app.get("/api/v1/apps/{app_key}/access")
    def app_access(app_key: str, account: sqlite3.Row = Depends(current_account),
                   conn: sqlite3.Connection = Depends(db_dependency)):
        policy = conn.execute("SELECT * FROM app_policies WHERE app_key=?", (app_key,)).fetchone()
        if policy is None:
            return {"app_key": app_key, "allowed": False, "reason": "No access policy exists."}
        if not policy["enabled"]:
            allowed, reason = False, "This application is disabled."
        elif policy["registration_mode"] == "closed":
            allowed, reason = False, "Registration is closed for this application."
        elif policy["registration_mode"] == "invite" and not conn.execute(
            "SELECT 1 FROM app_grants WHERE account_id=? AND app_key=?", (account["id"], app_key)
        ).fetchone():
            allowed, reason = False, "An invitation is required."
        else:
            age = calculate_age(account["date_of_birth"]) if account["date_of_birth"] else 0
            if age < policy["min_age"]:
                allowed, reason = False, "The account does not meet this application's minimum age."
            elif policy["guardian_required"] and age < 18 and not conn.execute(
                "SELECT 1 FROM guardian_approvals WHERE account_id=? AND app_key=?", (account["id"], app_key)
            ).fetchone():
                allowed, reason = False, "Guardian approval is required."
            else:
                allowed, reason = True, "Access policy satisfied."
        record_event(conn, "app_access_checked", account["id"], {"app_key": app_key, "allowed": allowed})
        return {"app_key": app_key, "allowed": allowed, "reason": reason}

    @app.post("/api/v1/admin/bootstrap", status_code=201)
    def bootstrap_owner(payload: BootstrapRequest, x_nori_bootstrap_token: str | None = Header(default=None),
                        conn: sqlite3.Connection = Depends(db_dependency)):
        if not cfg.bootstrap_token:
            raise HTTPException(status_code=503, detail="Set NORI_BOOTSTRAP_TOKEN before creating the owner.")
        if not x_nori_bootstrap_token or not secrets.compare_digest(x_nori_bootstrap_token, cfg.bootstrap_token):
            raise HTTPException(status_code=403, detail="Bootstrap token is incorrect.")
        if conn.execute("SELECT 1 FROM accounts WHERE role IN ('owner','admin') LIMIT 1").fetchone():
            raise HTTPException(status_code=409, detail="An owner or administrator already exists.")
        try:
            handle = normalize_handle(payload.nori_handle)
        except ValueError as exc:
            raise HTTPException(status_code=422, detail=str(exc))
        if not password_feedback(payload.password)["acceptable"]:
            raise HTTPException(status_code=422, detail="Use a password of at least 12 characters.")
        account_id = str(uuid.uuid4())
        conn.execute(
            """INSERT INTO accounts(id,phone_e164,nori_handle,first_name,last_name,password_hash,role,created_at)
               VALUES(?,NULL,?,?,?,?, 'owner',?)""",
            (account_id, handle, payload.first_name.strip(), payload.last_name.strip(),
             hash_password(payload.password), utc_now()),
        )
        record_event(conn, "owner_bootstrapped", account_id)
        token = create_access_token(account_id, "owner", cfg.secret_key, cfg.access_token_minutes)
        return {"account": {"id": account_id, "nori_address": f"{handle}@nori", "role": "owner"},
                "access_token": token, "token_type": "bearer"}

    @app.get("/api/v1/admin/accounts")
    def admin_accounts(actor: sqlite3.Row = Depends(admin_account),
                       conn: sqlite3.Connection = Depends(db_dependency)):
        rows = conn.execute(
            """SELECT id,phone_e164,nori_handle,first_name,last_name,date_of_birth,role,status,created_at
               FROM accounts ORDER BY created_at DESC"""
        ).fetchall()
        result = []
        for row in rows:
            item = dict(row)
            phone = item.pop("phone_e164")
            item["phone_masked"] = (phone[:4] + "••••" + phone[-2:]) if phone and len(phone) > 8 else ("—" if not phone else "••••")
            result.append(item)
        return {"accounts": result}

    @app.patch("/api/v1/admin/accounts/{account_id}/status")
    def set_account_status(account_id: str, payload: StatusRequest,
                           actor: sqlite3.Row = Depends(admin_account),
                           conn: sqlite3.Connection = Depends(db_dependency)):
        if payload.status not in {"active", "suspended"}:
            raise HTTPException(status_code=422, detail="Status must be active or suspended.")
        target = conn.execute("SELECT role FROM accounts WHERE id=?", (account_id,)).fetchone()
        if target is None:
            raise HTTPException(status_code=404, detail="Account not found.")
        if target["role"] == "owner":
            raise HTTPException(status_code=403, detail="The owner account cannot be suspended from this console.")
        conn.execute("UPDATE accounts SET status=? WHERE id=?", (payload.status, account_id))
        record_event(conn, "account_status_changed", account_id,
                     {"status": payload.status, "changed_by": actor["id"]})
        return {"account_id": account_id, "status": payload.status}

    @app.get("/api/v1/admin/activity")
    def admin_activity(limit: int = 100, actor: sqlite3.Row = Depends(admin_account),
                       conn: sqlite3.Connection = Depends(db_dependency)):
        bounded = max(1, min(limit, 250))
        rows = conn.execute(
            """SELECT e.id,e.account_id,e.event_type,e.details_json,e.created_at,a.nori_handle
               FROM activity_events e LEFT JOIN accounts a ON a.id=e.account_id
               ORDER BY e.id DESC LIMIT ?""", (bounded,)
        ).fetchall()
        return {"events": [{
            "id": row["id"], "account_id": row["account_id"], "nori_handle": row["nori_handle"],
            "event_type": row["event_type"], "details": json.loads(row["details_json"]),
            "created_at": row["created_at"],
        } for row in rows]}

    @app.put("/api/v1/admin/apps/{app_key}/policy")
    def set_app_policy(app_key: str, payload: AppPolicyRequest,
                       actor: sqlite3.Row = Depends(admin_account),
                       conn: sqlite3.Connection = Depends(db_dependency)):
        if not app_key.replace("-", "").replace("_", "").isalnum() or len(app_key) > 48:
            raise HTTPException(status_code=422, detail="Invalid application key.")
        if payload.registration_mode not in {"open", "invite", "closed"}:
            raise HTTPException(status_code=422, detail="Registration mode must be open, invite or closed.")
        conn.execute(
            """INSERT INTO app_policies(app_key,display_name,enabled,registration_mode,min_age,guardian_required,updated_at)
               VALUES(?,?,?,?,?,?,?)
               ON CONFLICT(app_key) DO UPDATE SET display_name=excluded.display_name,
               enabled=excluded.enabled,registration_mode=excluded.registration_mode,
               min_age=excluded.min_age,guardian_required=excluded.guardian_required,updated_at=excluded.updated_at""",
            (app_key, payload.display_name, int(payload.enabled), payload.registration_mode,
             payload.min_age, int(payload.guardian_required), utc_now()),
        )
        record_event(conn, "app_policy_changed", actor["id"],
                     {"app_key": app_key, "registration_mode": payload.registration_mode,
                      "min_age": payload.min_age, "guardian_required": payload.guardian_required})
        return {"app_key": app_key, "updated": True}

    @app.post("/api/v1/admin/accounts/{account_id}/apps/{app_key}/grant", status_code=201)
    def grant_app(account_id: str, app_key: str, actor: sqlite3.Row = Depends(admin_account),
                  conn: sqlite3.Connection = Depends(db_dependency)):
        if conn.execute("SELECT 1 FROM accounts WHERE id=?", (account_id,)).fetchone() is None:
            raise HTTPException(status_code=404, detail="Account not found.")
        if conn.execute("SELECT 1 FROM app_policies WHERE app_key=?", (app_key,)).fetchone() is None:
            raise HTTPException(status_code=404, detail="Application policy not found.")
        conn.execute(
            "INSERT OR IGNORE INTO app_grants(account_id,app_key,granted_by,created_at) VALUES(?,?,?,?)",
            (account_id, app_key, actor["id"], utc_now()),
        )
        record_event(conn, "app_access_granted", account_id,
                     {"app_key": app_key, "granted_by": actor["id"]})
        return {"account_id": account_id, "app_key": app_key, "granted": True}

    return app


app = create_app()

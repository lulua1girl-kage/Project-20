from __future__ import annotations

import base64
import hashlib
import hmac
import json
import re
import secrets
import time

from argon2 import PasswordHasher
from argon2.exceptions import VerifyMismatchError, VerificationError

_password_hasher = PasswordHasher(time_cost=3, memory_cost=65536, parallelism=2, hash_len=32, salt_len=16)
_RESERVED = {"admin", "administrator", "support", "security", "nori", "root", "owner", "help", "system"}


def hash_password(password: str) -> str:
    return _password_hasher.hash(password)


def verify_password(password_hash: str, password: str) -> bool:
    try:
        return _password_hasher.verify(password_hash, password)
    except (VerifyMismatchError, VerificationError):
        return False


def password_feedback(password: str) -> dict:
    checks = {
        "length": len(password) >= 12,
        "lowercase": bool(re.search(r"[a-z]", password)),
        "uppercase": bool(re.search(r"[A-Z]", password)),
        "number": bool(re.search(r"\d", password)),
        "symbol": bool(re.search(r"[^A-Za-z0-9]", password)),
        "not_common": password.lower() not in {"password123!", "qwerty123456", "letmein123456", "welcome12345!"},
    }
    score = sum(checks.values())
    return {"checks": checks, "score": score,
            "strength": "weak" if score <= 2 else "fair" if score <= 4 else "strong",
            "acceptable": checks["length"] and checks["not_common"]}


def normalize_handle(value: str) -> str:
    handle = value.strip().lower()
    if not re.fullmatch(r"[a-z0-9](?:[a-z0-9._-]{1,22}[a-z0-9])?", handle):
        raise ValueError("Use 3–24 lowercase letters, numbers, dots, underscores or hyphens.")
    if handle in _RESERVED or ".." in handle or "__" in handle or "--" in handle:
        raise ValueError("That Nori address is reserved or uses invalid repeated punctuation.")
    return handle


def otp_digest(secret: str, challenge_id: str, code: str) -> str:
    return hmac.new(secret.encode(), f"{challenge_id}:{code}".encode(), hashlib.sha256).hexdigest()


def _b64(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def create_access_token(account_id: str, role: str, secret: str, minutes: int) -> str:
    header = _b64(json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(",", ":")).encode())
    payload = _b64(json.dumps({"sub": account_id, "role": role, "iat": int(time.time()),
                               "exp": int(time.time()) + minutes * 60}, separators=(",", ":")).encode())
    message = f"{header}.{payload}".encode()
    signature = _b64(hmac.new(secret.encode(), message, hashlib.sha256).digest())
    return f"{header}.{payload}.{signature}"


def decode_access_token(token: str, secret: str) -> dict:
    try:
        header, payload, signature = token.split(".")
        message = f"{header}.{payload}".encode()
        expected = _b64(hmac.new(secret.encode(), message, hashlib.sha256).digest())
        if not hmac.compare_digest(signature, expected):
            raise ValueError("Bad signature")
        decoded = json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))
        if decoded.get("exp", 0) < int(time.time()) or not decoded.get("sub"):
            raise ValueError("Expired or incomplete")
        if decoded.get("role") not in {"owner", "admin", "user"}:
            raise ValueError("Invalid role")
        return decoded
    except Exception as exc:
        raise ValueError("Invalid or expired access token") from exc


def random_otp() -> str:
    return f"{secrets.randbelow(1_000_000):06d}"

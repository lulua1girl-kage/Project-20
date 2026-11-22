from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


def _bool(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    return default if raw is None else raw.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class Settings:
    data_dir: Path
    secret_key: str
    bootstrap_token: str
    environment: str
    allow_dev_otp: bool
    access_token_minutes: int

    @property
    def database_path(self) -> Path:
        return self.data_dir / "nori_id.sqlite3"

    @classmethod
    def from_env(cls) -> "Settings":
        return cls(
            data_dir=Path(os.getenv("NORI_DATA_DIR", "./data")).expanduser(),
            secret_key=os.getenv("NORI_SECRET_KEY", "local-development-only-change-this-secret"),
            bootstrap_token=os.getenv("NORI_BOOTSTRAP_TOKEN", ""),
            environment=os.getenv("NORI_ENV", "development").lower(),
            allow_dev_otp=_bool("NORI_ALLOW_DEV_OTP", False),
            access_token_minutes=max(5, min(120, int(os.getenv("NORI_ACCESS_TOKEN_MINUTES", "30")))),
        )

from __future__ import annotations

import sqlite3

from fastapi.testclient import TestClient

from nori_id.main import create_app
from nori_id.phones import parse_and_validate_phone, region_catalog


SECRET = "test-secret-key-that-is-at-least-32-characters-long"
BOOTSTRAP = "test-bootstrap-token-that-is-long-enough"


def make_client(tmp_path):
    app = create_app(
        tmp_path / "nori-test.sqlite3",
        secret_key=SECRET,
        bootstrap_token=BOOTSTRAP,
        environment="development",
        allow_dev_otp=True,
    )
    return TestClient(app)


def bootstrap(client):
    response = client.post(
        "/api/v1/admin/bootstrap",
        headers={"X-Nori-Bootstrap-Token": BOOTSTRAP},
        json={
            "nori_handle": "founder",
            "first_name": "Nori",
            "last_name": "Owner",
            "password": "A-very-long-test-password-2026!",
        },
    )
    assert response.status_code == 201, response.text
    return response.json()["access_token"]


def register_user(client, phone="+251911234567", handle="test.user"):
    challenge = client.post("/api/v1/phone/challenges", json={"phone_number": phone})
    assert challenge.status_code == 201, challenge.text
    data = challenge.json()
    return client.post(
        "/api/v1/accounts/register",
        json={
            "challenge_id": data["challenge_id"],
            "verification_code": data["development_code"],
            "first_name": "Test",
            "last_name": "User",
            "date_of_birth": "2000-01-15",
            "gender": None,
            "password": "A-different-long-password-2026!",
            "nori_handle": handle,
        },
    )


def test_region_catalog_contains_ethiopia_and_global_metadata():
    regions = region_catalog()
    by_region = {item["region"]: item for item in regions}
    assert "ET" in by_region
    assert by_region["ET"]["calling_code"] == "+251"
    assert by_region["ET"]["flag"] == "🇪🇹"
    assert len(regions) > 200


def test_phone_validation_normalizes_to_e164():
    parsed = parse_and_validate_phone("+251911234567")
    assert parsed["phone_e164"] == "+251911234567"
    assert parsed["region"] == "ET"


def test_registration_persists_argon2_hash(tmp_path):
    client = make_client(tmp_path)
    response = register_user(client)
    assert response.status_code == 201, response.text
    assert response.json()["account"]["nori_address"] == "test.user@nori"
    conn = sqlite3.connect(tmp_path / "nori-test.sqlite3")
    stored = conn.execute("SELECT password_hash FROM accounts WHERE nori_handle='test.user'").fetchone()[0]
    conn.close()
    assert stored.startswith("$argon2id$")
    assert "A-different-long-password-2026!" not in stored


def test_duplicate_phone_is_rejected(tmp_path):
    client = make_client(tmp_path)
    assert register_user(client).status_code == 201
    response = client.post("/api/v1/phone/challenges", json={"phone_number": "+251911234567"})
    assert response.status_code == 409


def test_duplicate_nori_handle_is_rejected(tmp_path):
    client = make_client(tmp_path)
    assert register_user(client).status_code == 201
    response = register_user(client, phone="+251912345678")
    assert response.status_code == 409


def test_owner_can_manage_app_policy_and_inspect_security_events(tmp_path):
    client = make_client(tmp_path)
    token = bootstrap(client)
    headers = {"Authorization": f"Bearer {token}"}
    assert register_user(client).status_code == 201
    accounts = client.get("/api/v1/admin/accounts", headers=headers)
    assert accounts.status_code == 200
    assert len(accounts.json()["accounts"]) == 2
    assert "phone_masked" in accounts.json()["accounts"][0]
    policy = client.put(
        "/api/v1/admin/apps/taskforge/policy",
        headers=headers,
        json={
            "display_name": "TaskForge",
            "enabled": True,
            "registration_mode": "open",
            "min_age": 13,
            "guardian_required": True,
        },
    )
    assert policy.status_code == 200
    activity = client.get("/api/v1/admin/activity", headers=headers)
    assert activity.status_code == 200
    assert any(item["event_type"] == "account_registered" for item in activity.json()["events"])


def test_non_admin_cannot_open_control_endpoints(tmp_path):
    client = make_client(tmp_path)
    response = register_user(client)
    token = response.json()["access_token"]
    result = client.get("/api/v1/admin/accounts", headers={"Authorization": f"Bearer {token}"})
    assert result.status_code == 403


def test_bootstrap_is_one_time_only(tmp_path):
    client = make_client(tmp_path)
    bootstrap(client)
    response = client.post(
        "/api/v1/admin/bootstrap",
        headers={"X-Nori-Bootstrap-Token": BOOTSTRAP},
        json={"nori_handle": "secondowner", "first_name": "Second", "last_name": "Owner",
              "password": "Another-long-password-2026!"},
    )
    assert response.status_code == 409


def test_wrong_otp_attempts_are_counted(tmp_path):
    client = make_client(tmp_path)
    challenge = client.post("/api/v1/phone/challenges", json={"phone_number": "+251911234567"}).json()
    for _ in range(5):
        response = client.post(
            "/api/v1/phone/verify-preview",
            json={"challenge_id": challenge["challenge_id"], "verification_code": "000000"},
        )
        assert response.status_code == 400
    blocked = client.post(
        "/api/v1/phone/verify-preview",
        json={"challenge_id": challenge["challenge_id"], "verification_code": challenge["development_code"]},
    )
    assert blocked.status_code == 429


def test_development_otp_is_disabled_outside_development(tmp_path):
    app = create_app(
        tmp_path / "prod.sqlite3", secret_key=SECRET, bootstrap_token=BOOTSTRAP,
        environment="production", allow_dev_otp=True,
    )
    client = TestClient(app)
    response = client.post("/api/v1/phone/challenges", json={"phone_number": "+251911234567"})
    assert response.status_code == 503

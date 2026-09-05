"""Testes para pg_loader, incluindo preservacao do admin.

Rode: python -m pytest tests/test_pg_loader.py -v
"""
import sys
from pathlib import Path
from unittest.mock import MagicMock, call

sys.path.insert(0, str(Path(__file__).parent.parent))

import pytest
from pg_loader import fetch_admin, restore_admin, truncate_all


class TestAdminPreservation:
    def test_fetch_admin_returns_none_when_no_admin(self):
        conn = MagicMock()
        cursor = MagicMock()
        cursor.fetchone.return_value = None
        conn.cursor.return_value.__enter__.return_value = cursor

        result = fetch_admin(conn)

        assert result is None

    def test_fetch_admin_returns_admin_data(self):
        conn = MagicMock()
        cursor = MagicMock()
        cursor.fetchone.return_value = (
            "admin-uuid", "admin", "pass", "pin", True, None, "",
            "System Administrator", "00000000000", "admin@rentafit.com.br", "",
            "ADM", 99,
        )
        cursor.fetchall.return_value = [("ADMIN",), ("MANAGER",)]
        conn.cursor.return_value.__enter__.return_value = cursor

        result = fetch_admin(conn)

        assert result["id"] == "admin-uuid"
        assert result["username"] == "admin"
        assert result["roles"] == ["ADMIN", "MANAGER"]

    def test_restore_admin_inserts_all_records(self):
        conn = MagicMock()
        cursor = MagicMock()
        conn.cursor.return_value.__enter__.return_value = cursor
        # Document is free
        cursor.fetchone.return_value = None

        admin = {
            "id": "admin-uuid",
            "username": "admin",
            "password": "pass",
            "pin": None,
            "is_active": True,
            "password_changed_at": None,
            "issuer_cnpj": "",
            "name": "System Administrator",
            "document": "00000000000",
            "email": "admin@rentafit.com.br",
            "legacy_id": "",
            "initials": "ADM",
            "role_level": 99,
            "roles": ["ADMIN"],
        }

        restore_admin(conn, admin)

        cursor.execute.assert_any_call("""
            INSERT INTO people (id, legacy_id, name, document, email, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, NOW(), NOW())
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                document = EXCLUDED.document,
                email = EXCLUDED.email,
                updated_at = NOW()
        """, ("admin-uuid", "", "System Administrator", "00000000000", "admin@rentafit.com.br"))

        cursor.execute.assert_any_call("""
            INSERT INTO user_accounts (id, username, password, pin, is_active, password_changed_at, issuer_cnpj)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (id) DO UPDATE SET
                username = EXCLUDED.username,
                password = EXCLUDED.password,
                pin = EXCLUDED.pin,
                is_active = EXCLUDED.is_active,
                password_changed_at = EXCLUDED.password_changed_at,
                issuer_cnpj = EXCLUDED.issuer_cnpj
        """, ("admin-uuid", "admin", "pass", None, True, None, ""))

        conn.commit.assert_called_once()

    def test_restore_admin_does_nothing_when_none(self):
        conn = MagicMock()
        restore_admin(conn, None)
        conn.cursor.assert_not_called()
        conn.commit.assert_not_called()

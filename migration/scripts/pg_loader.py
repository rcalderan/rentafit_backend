"""Carga de CSVs no PostgreSQL via COPY."""

import csv
import time
from pathlib import Path
from typing import Any

import psycopg2


def connect(host: str, port: str, db: str, user: str, password: str):
    return psycopg2.connect(
        host=host,
        port=port,
        dbname=db,
        user=user,
        password=password,
    )


def truncate_all(conn, tables: list[str]) -> None:
    """Trunca todas as tabelas em ordem para evitar conflitos de FK."""
    with conn.cursor() as cursor:
        cursor.execute(f"TRUNCATE TABLE {', '.join(tables)} RESTART IDENTITY CASCADE")
    conn.commit()


def load_csv(conn, csv_path: Path, table_name: str) -> int:
    with open(csv_path, "r", encoding="utf-8") as f:
        with conn.cursor() as cursor:
            cursor.copy_expert(f"COPY {table_name} FROM STDIN WITH (FORMAT CSV, HEADER true, ENCODING 'UTF-8', NULL '\\N')", f)
    conn.commit()

    with conn.cursor() as cursor:
        cursor.execute(f"SELECT COUNT(*) FROM {table_name}")
        return cursor.fetchone()[0]


def fetch_admin(conn) -> dict[str, Any] | None:
    """Busca o usuario admin existente no banco antes da migracao.

    Preserva pessoa, funcionario, user_account e roles.
    """
    with conn.cursor() as cursor:
        cursor.execute("""
            SELECT ua.id, ua.username, ua.password, ua.pin, ua.is_active,
                   ua.password_changed_at, ua.issuer_cnpj,
                   p.name, p.document, p.email, p.legacy_id,
                   e.initials, e.role_level
            FROM user_accounts ua
            JOIN people p ON p.id = ua.id
            LEFT JOIN employees e ON e.id = ua.id
            WHERE ua.username = 'admin'
            LIMIT 1
        """)
        row = cursor.fetchone()
        if not row:
            return None

        admin_id = row[0]
        cursor.execute("""
            SELECT r.role FROM roles r
            JOIN user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = %s
        """, (admin_id,))
        roles = [r[0] for r in cursor.fetchall()]

        return {
            "id": admin_id,
            "username": row[1],
            "password": row[2],
            "pin": row[3],
            "is_active": row[4],
            "password_changed_at": row[5],
            "issuer_cnpj": row[6],
            "name": row[7],
            "document": row[8],
            "email": row[9],
            "legacy_id": row[10],
            "initials": row[11],
            "role_level": row[12],
            "roles": roles,
        }


def restore_admin(conn, admin: dict[str, Any] | None) -> None:
    """Re-insere o admin preservado apos a carga dos dados migrados.

    Se o documento do admin ja foi ocupado por outra pessoa migrada,
    limpa o documento do admin para evitar violacao de unique constraint.
    """
    if not admin:
        return

    with conn.cursor() as cursor:
        # Verificar se o documento ja existe em outra pessoa
        if admin.get("document"):
            cursor.execute("SELECT id FROM people WHERE document = %s LIMIT 1", (admin["document"],))
            existing = cursor.fetchone()
            if existing and existing[0] != admin["id"]:
                admin = dict(admin)
                admin["document"] = ""

        # Pessoa: ON CONFLICT evita falhar se id colidir (improvavel em banco truncado)
        cursor.execute("""
            INSERT INTO people (id, legacy_id, name, document, email, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, NOW(), NOW())
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                document = EXCLUDED.document,
                email = EXCLUDED.email,
                updated_at = NOW()
        """, (admin["id"], admin["legacy_id"], admin["name"],
              admin["document"], admin["email"]))

        # Funcionario: limpa initials conflitantes em outras pessoas migradas
        admin_initials = admin["initials"] or 'ADM'
        cursor.execute(
            "UPDATE employees SET initials = NULL WHERE initials = %s AND id <> %s",
            (admin_initials, admin["id"]),
        )
        cursor.execute("""
            INSERT INTO employees (id, initials, role_level)
            VALUES (%s, %s, %s)
            ON CONFLICT (id) DO UPDATE SET
                initials = EXCLUDED.initials,
                role_level = EXCLUDED.role_level
        """, (admin["id"], admin_initials, admin["role_level"] or 99))

        cursor.execute("""
            INSERT INTO customers (id, is_authenticated, notes, created_by_id)
            VALUES (%s, false, 'Admin account preserved during migration', NULL)
            ON CONFLICT (id) DO NOTHING
        """, (admin["id"],))

        cursor.execute("""
            INSERT INTO user_accounts (id, username, password, pin, is_active, password_changed_at, issuer_cnpj)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (id) DO UPDATE SET
                username = EXCLUDED.username,
                password = EXCLUDED.password,
                pin = EXCLUDED.pin,
                is_active = EXCLUDED.is_active,
                password_changed_at = EXCLUDED.password_changed_at,
                issuer_cnpj = EXCLUDED.issuer_cnpj
        """, (admin["id"], admin["username"], admin["password"], admin["pin"],
              admin["is_active"], admin["password_changed_at"], admin["issuer_cnpj"]))

        if admin["roles"]:
            for role_name in admin["roles"]:
                cursor.execute("""
                    INSERT INTO user_roles (user_id, role_id)
                    SELECT %s, id FROM roles WHERE role = %s
                    ON CONFLICT DO NOTHING
                """, (admin["id"], role_name))

    conn.commit()

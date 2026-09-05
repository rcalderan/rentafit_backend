#!/usr/bin/env python3
"""Migra dados de .bson do noivabd para PostgreSQL via CSV + COPY."""

import argparse
import sys
import time
from pathlib import Path

import psycopg2

from config import TABLES
from csv_writer import write_csv
from pg_loader import connect, load_csv, truncate_all
from readers import read_all_bson_files
from report import build_report, write_report
from transformers import (
    EPOCH,
    extract_issuer_cnpj,
    new_uuid,
    transform_categories,
    transform_contracts,
    transform_people_from_cliente,
    transform_people_from_funcionario,
    transform_product_categories,
    transform_products_and_rental_items,
)


def parse_args():
    parser = argparse.ArgumentParser(description="Migração noivabd -> Rentafit")
    parser.add_argument("--session-dir", required=True)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--pg-host", required=True)
    parser.add_argument("--pg-port", required=True)
    parser.add_argument("--pg-db", required=True)
    parser.add_argument("--pg-user", required=True)
    parser.add_argument("--pg-password", required=True)
    return parser.parse_args()


def main():
    started_at = time.time()
    args = parse_args()
    session_dir = Path(args.session_dir)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    errors = []
    warnings = []
    tables = []

    try:
        all_docs = read_all_bson_files(str(session_dir))
        source_counts = {name: len(docs) for name, docs in all_docs.items()}

        # 1. Categories — apenas roupa_tipo (categorias de produto), nao categoria (pagamentos)
        default_category_id = new_uuid()
        category_rows = [{
            "id": default_category_id,
            "name": "SEM CATEGORIA",
            "display_name": "SEM CATEGORIA",
            "description": "Categoria default para produtos sem categoria legado",
            "product_type": "RENTAL",
            "active": "true",
            "created_at": EPOCH,
            "updated_at": EPOCH,
            "legacy_id": "",
        }]
        category_legacy_map = {}
        if "roupa_tipo" in all_docs:
            cat_rows, category_legacy_map = transform_product_categories(all_docs["roupa_tipo"], default_category_id)
            category_rows.extend(cat_rows)

        # 2. People + customers + employees + user_accounts
        people_rows = []
        customer_rows = []
        employee_rows = []
        user_account_rows = []
        address_rows = []
        phone_rows = []
        pad_rows = []
        customer_map = {}
        customer_name_map = {}
        employee_map = {}

        # Cliente default para contratos órfãos
        default_customer_id = new_uuid()
        people_rows.append({
            "id": default_customer_id,
            "legacy_id": "",
            "name": "CLIENTE LEGADO",
            "document": "",
            "email": "",
            "created_at": "1970-01-01 00:00:00+00",
            "updated_at": "1970-01-01 00:00:00+00",
        })
        customer_rows.append({
            "id": default_customer_id,
            "is_authenticated": "false",
            "notes": "Cliente default para contratos sem cliente mapeado",
            "created_by_id": "",
        })

        if "cliente" in all_docs:
            p, c, a, ph, pad, cmap = transform_people_from_cliente(all_docs["cliente"])
            people_rows.extend(p)
            customer_rows.extend(c)
            address_rows.extend(a)
            phone_rows.extend(ph)
            pad_rows.extend(pad)
            customer_map.update({k: v[0] for k, v in cmap.items()})
            customer_name_map.update({k: v[1] for k, v in cmap.items()})

        if "fornecedor" in all_docs:
            p, c, a, ph, pad, cmap = transform_people_from_cliente(all_docs["fornecedor"])
            people_rows.extend(p)
            customer_rows.extend(c)
            address_rows.extend(a)
            phone_rows.extend(ph)
            pad_rows.extend(pad)
            customer_map.update({k: v[0] for k, v in cmap.items()})
            customer_name_map.update({k: v[1] for k, v in cmap.items()})

        if "funcionario" in all_docs:
            p, e, u, emap = transform_people_from_funcionario(all_docs["funcionario"])
            people_rows.extend(p)
            employee_rows.extend(e)
            user_account_rows.extend(u)
            employee_map.update(emap)

        # Aplicar issuer_cnpj do conf no admin
        issuer_cnpj = extract_issuer_cnpj(all_docs.get("conf", []))
        if issuer_cnpj and user_account_rows:
            user_account_rows[0]["issuer_cnpj"] = issuer_cnpj

        # 3. Products + rental_items
        product_rows = []
        rental_item_rows = []
        if "roupa" in all_docs:
            product_rows, rental_item_rows = transform_products_and_rental_items(all_docs["roupa"], default_category_id, category_legacy_map)

        # 4. Mapas legacy_id -> uuid para FKs (já construídos acima)
        rental_item_map = {}
        for r in rental_item_rows:
            legacy = r.get("legacy_id")
            if legacy != "":
                rental_item_map[str(legacy)] = r["id"]

        # 5. Contracts
        contract_rows = []
        contract_item_rows = []
        payment_rows = []
        meta_rows = []
        if "contrato" in all_docs:
            contract_rows, contract_item_rows, payment_rows, meta_rows = transform_contracts(
                all_docs["contrato"], customer_map, customer_name_map, employee_map, rental_item_map, default_customer_id
            )

        # 6. Deduplicar documents e legacy_ids em people (unique constraints)
        seen_docs = set()
        seen_legacy = set()
        for p in people_rows:
            doc = p.get("document", "")
            if doc and doc in seen_docs:
                p["document"] = ""
            elif doc:
                seen_docs.add(doc)
            legacy = p.get("legacy_id", "")
            if legacy and legacy in seen_legacy:
                p["legacy_id"] = ""
            elif legacy:
                seen_legacy.add(legacy)

        # Agrupar todas as linhas por tabela
        all_rows = {
            "categories": category_rows,
            "people": people_rows,
            "customers": customer_rows,
            "employees": employee_rows,
            "user_accounts": user_account_rows,
            "addresses": address_rows,
            "person_address_details": pad_rows,
            "customer_phones": phone_rows,
            "products": product_rows,
            "rental_items": rental_item_rows,
            "rental_contracts": contract_rows,
            "rental_contract_items": contract_item_rows,
            "rental_payments": payment_rows,
            "rental_contract_item_meta": meta_rows,
        }

        # 6. Escrever CSVs
        csv_paths = {}
        for table in TABLES:
            csv_paths[table] = write_csv(output_dir, table, all_rows.get(table, []))

        # 7. COPY em ordem topológica
        conn = connect(
            host=args.pg_host,
            port=args.pg_port,
            db=args.pg_db,
            user=args.pg_user,
            password=args.pg_password,
        )

        # Limpar tabelas antes do COPY (banco foi clonado, pode ter dados)
        truncate_all(conn, TABLES)

        for table in TABLES:
            inserted = 0
            table_errors = []
            rows = all_rows.get(table, [])
            if not rows:
                tables.append({"name": table, "source_count": 0, "inserted_count": 0, "errors": []})
                continue
            try:
                inserted = load_csv(conn, csv_paths[table], table)
            except psycopg2.Error as e:
                table_errors.append(str(e))
                errors.append(f"COPY {table} falhou: {e}")
                warnings.append(f"Continuando após erro em {table}")
                conn = connect(
                    host=args.pg_host,
                    port=args.pg_port,
                    db=args.pg_db,
                    user=args.pg_user,
                    password=args.pg_password,
                )

            source = sum(
                count for name, count in source_counts.items()
                if name == "roupa_tipo" and table == "categories"
                or name in {"cliente", "funcionario", "fornecedor"} and table == "people"
                or name == "roupa" and table in {"products", "rental_items"}
                or name == "contrato" and table == "rental_contracts"
            )
            tables.append({"name": table, "source_count": len(rows), "inserted_count": inserted, "errors": table_errors})

        conn.close()

    except Exception as e:
        import traceback
        errors.append(str(e))
        errors.append(traceback.format_exc())

    finished_at = time.time()
    report = build_report(tables, errors, warnings, started_at, finished_at)
    write_report(output_dir, report)

    if errors:
        sys.exit(1)

    print(f"Migração concluída em {report['duration_seconds']}s")
    sys.exit(0)


if __name__ == "__main__":
    main()

"""Teste end-to-end da migracao usando fixtures mock.

Simula o fluxo completo: ler BSON -> transformar -> escrever CSV -> validar CSVs.

NAO conecta ao PostgreSQL (usa FakePgLoader para mockar a carga).
Rode: python -m pytest tests/test_migration_e2e.py -v
"""
import sys
import json
import csv
from pathlib import Path
from unittest.mock import patch, MagicMock

sys.path.insert(0, str(Path(__file__).parent.parent))

import pytest
from config import TABLES, CSV_HEADERS
from csv_writer import write_csv
from readers import read_all_bson_files
from transformers import (
    EPOCH,
    new_uuid,
    transform_product_categories,
    transform_people_from_cliente,
    transform_people_from_funcionario,
    transform_products_and_rental_items,
    transform_contracts,
)

FIXTURES_DIR = Path(__file__).parent / "fixtures"


def load_fixtures() -> dict:
    return read_all_bson_files(str(FIXTURES_DIR))


class FakeMigration:
    """Executa o pipeline de transformacao sem conectar ao PostgreSQL."""

    def __init__(self, fixtures_dir: Path):
        self.fixtures = read_all_bson_files(str(fixtures_dir))
        self.output_dir = None

    def run_transform(self, output_dir: Path) -> dict:
        """Executa transformacao e escreve CSVs. Retorna dict com all_rows e source_counts."""
        output_dir.mkdir(parents=True, exist_ok=True)
        self.output_dir = output_dir

        all_docs = self.fixtures
        source_counts = {name: len(docs) for name, docs in all_docs.items()}

        # 1. Categories
        default_category_id = new_uuid()
        category_rows = [{
            "id": default_category_id,
            "name": "SEM CATEGORIA",
            "display_name": "SEM CATEGORIA",
            "description": "Categoria default",
            "product_type": "RENTAL",
            "active": "true",
            "created_at": EPOCH,
            "updated_at": EPOCH,
            "legacy_id": "",
        }]
        category_legacy_map = {}
        if "roupa_tipo" in all_docs:
            cat_rows, category_legacy_map = transform_product_categories(
                all_docs["roupa_tipo"], default_category_id
            )
            category_rows.extend(cat_rows)

        # 2. People + customers
        people_rows = []
        customer_rows = []
        address_rows = []
        phone_rows = []
        pad_rows = []
        customer_map = {}
        customer_name_map = {}

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
            "notes": "Cliente default",
            "created_by_id": "",
        })

        if "cliente" in all_docs:
            p, c, a, ph, pa, cm = transform_people_from_cliente(all_docs["cliente"])
            people_rows.extend(p)
            customer_rows.extend(c)
            address_rows.extend(a)
            phone_rows.extend(ph)
            pad_rows.extend(pa)
            customer_map.update(cm)
            for lid, (uuid, name) in cm.items():
                customer_name_map[lid] = name

        # 3. Employees
        employee_rows = []
        user_account_rows = []
        employee_map = {}
        if "funcionario" in all_docs:
            ep, ee, eu, em = transform_people_from_funcionario(all_docs["funcionario"])
            people_rows.extend(ep)
            employee_rows.extend(ee)
            user_account_rows.extend(eu)
            employee_map.update(em)

        # 4. Products + rental items
        product_rows = []
        rental_item_rows = []
        if "roupa" in all_docs:
            product_rows, rental_item_rows = transform_products_and_rental_items(
                all_docs["roupa"], default_category_id, category_legacy_map
            )

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
                all_docs["contrato"], customer_map, customer_name_map,
                employee_map, rental_item_map, default_customer_id
            )

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

        # Escrever CSVs
        csv_paths = {}
        for table in TABLES:
            csv_paths[table] = write_csv(output_dir, table, all_rows.get(table, []))

        return {
            "all_rows": all_rows,
            "source_counts": source_counts,
            "csv_paths": csv_paths,
        }


class TestMigrationE2E:
    """Teste end-to-end com fixtures mock: transforma e valida CSVs."""

    @pytest.fixture
    def migration_result(self, tmp_path):
        mig = FakeMigration(FIXTURES_DIR)
        return mig.run_transform(tmp_path / "output")

    def test_all_14_tables_have_csvs(self, migration_result):
        csv_paths = migration_result["csv_paths"]
        for table in TABLES:
            assert table in csv_paths, f"CSV faltando para tabela {table}"
            assert csv_paths[table].exists(), f"Arquivo CSV nao existe: {table}"

    def test_categories_has_default_plus_roupa_tipo(self, migration_result):
        rows = migration_result["all_rows"]["categories"]
        # 1 default + 1 roupa_tipo (Terno _id=4)
        assert len(rows) == 2
        names = [r["name"] for r in rows]
        assert "SEM CATEGORIA" in names
        assert "Terno" in names

    def test_people_has_default_plus_cliente_plus_funcionario(self, migration_result):
        rows = migration_result["all_rows"]["people"]
        # 1 default + 1 cliente + 1 funcionario
        assert len(rows) == 3
        names = [r["name"] for r in rows]
        assert "CLIENTE LEGADO" in names
        assert "Joao Mock da Silva" in names
        assert "Funcionario Mock" in names

    def test_customers_has_default_plus_cliente(self, migration_result):
        rows = migration_result["all_rows"]["customers"]
        assert len(rows) == 2

    def test_employees_has_one(self, migration_result):
        rows = migration_result["all_rows"]["employees"]
        assert len(rows) == 1

    def test_products_has_one_with_correct_category(self, migration_result):
        rows = migration_result["all_rows"]["products"]
        assert len(rows) == 1
        cat_rows = migration_result["all_rows"]["categories"]
        terno = next(r for r in cat_rows if r["name"] == "Terno")
        assert rows[0]["category_id"] == terno["id"]

    def test_rental_items_has_one(self, migration_result):
        rows = migration_result["all_rows"]["rental_items"]
        assert len(rows) == 1
        assert rows[0]["legacy_id"] == 9001

    def test_contracts_has_one(self, migration_result):
        rows = migration_result["all_rows"]["rental_contracts"]
        assert len(rows) == 1
        c = rows[0]
        assert c["legacy_id"] == "5001"
        assert c["customer_name"] == "Joao Mock da Silva"
        assert c["status"] == "FINALIZED"
        assert c["is_returned"] == "true"
        # Datas ordenadas
        assert c["pickup_date"] <= c["event_date"] <= c["return_date"]

    def test_contract_items_has_one(self, migration_result):
        rows = migration_result["all_rows"]["rental_contract_items"]
        assert len(rows) == 1
        assert rows[0]["value"] == 350.0

    def test_payments_has_two(self, migration_result):
        rows = migration_result["all_rows"]["rental_payments"]
        assert len(rows) == 2
        values = [r["value"] for r in rows]
        assert 250.0 in values
        assert 100.0 in values

    def test_meta_has_three(self, migration_result):
        rows = migration_result["all_rows"]["rental_contract_item_meta"]
        assert len(rows) == 3
        descs = [r["description"] for r in rows]
        assert "M44" in descs
        assert "SEM SAPATO" in descs

    def test_csv_headers_match_config(self, migration_result):
        """Cada CSV deve ter o header definido em config.CSV_HEADERS."""
        csv_paths = migration_result["csv_paths"]
        for table, path in csv_paths.items():
            if not path.exists():
                continue
            with open(path, "r", encoding="utf-8") as f:
                reader = csv.reader(f)
                header = next(reader, None)
            expected = CSV_HEADERS.get(table, [])
            assert header == expected, (
                f"Header do CSV {table} nao bate com config. "
                f"Esperado: {expected}, Atual: {header}"
            )

    def test_csv_row_count_matches_transformed(self, migration_result):
        """Numero de linhas no CSV (excluindo header) deve bater com all_rows."""
        csv_paths = migration_result["csv_paths"]
        all_rows = migration_result["all_rows"]
        for table, path in csv_paths.items():
            if not path.exists():
                continue
            with open(path, "r", encoding="utf-8") as f:
                reader = csv.reader(f)
                next(reader, None)  # skip header
                csv_count = sum(1 for _ in reader)
            row_count = len(all_rows.get(table, []))
            assert csv_count == row_count, (
                f"CSV {table}: {csv_count} linhas, mas all_rows tem {row_count}"
            )

    def test_no_empty_mandatory_csvs(self, migration_result):
        """Tabelas com FKs (products, contracts) nao devem estar vazias."""
        all_rows = migration_result["all_rows"]
        assert len(all_rows["products"]) > 0, "products nao deveria estar vazio"
        assert len(all_rows["rental_contracts"]) > 0, "rental_contracts nao deveria estar vazio"
        assert len(all_rows["rental_contract_items"]) > 0, "rental_contract_items nao deveria estar vazio"

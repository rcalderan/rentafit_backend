"""Testes unitarios para transformers.py usando fixtures mock.

Rode: python -m pytest tests/test_transformers.py -v
"""
import sys
import os
from pathlib import Path
from datetime import datetime

# Adicionar diretorio pai ao path para importar modulos
sys.path.insert(0, str(Path(__file__).parent.parent))

import pytest
from transformers import (
    fmt_date,
    fmt_datetime,
    normalize_text,
    normalize_document,
    transform_product_categories,
    transform_categories,
    transform_people_from_cliente,
    transform_people_from_funcionario,
    transform_products_and_rental_items,
    transform_contracts,
)
from readers import read_all_bson_files


FIXTURES_DIR = Path(__file__).parent / "fixtures"


def load_fixtures() -> dict:
    """Carrega todos os BSONs mock em um dict nome -> lista de docs."""
    return read_all_bson_files(str(FIXTURES_DIR))


# ---------- fmt_date / fmt_datetime ----------

class TestFmtDate:
    def test_datetime_normal(self):
        d = datetime(2010, 12, 2, 2, 0, 0)
        assert fmt_date(d) == "2010-12-02"

    def test_datetime_year_below_100(self):
        """Regression: ano legado 11 deve virar 2011, nao 0011."""
        d = datetime(11, 12, 26, 2, 0, 0)
        assert fmt_date(d) == "2011-12-26"

    def test_none_returns_empty(self):
        assert fmt_date(None) == ""

    def test_string_yyyy_mm_dd(self):
        assert fmt_date("2010-12-02") == "2010-12-02"

    def test_string_dd_mm_yyyy(self):
        assert fmt_date("02/12/2010") == "2010-12-02"

    def test_string_mm_dd_yy(self):
        assert fmt_date("12-02-10") == "2010-12-02"


class TestFmtDatetime:
    def test_datetime_normal(self):
        d = datetime(2010, 12, 2, 2, 0, 0)
        assert fmt_datetime(d) == "2010-12-02 02:00:00"

    def test_datetime_year_below_100(self):
        """Regression: ano 11 deve virar 2011."""
        d = datetime(11, 1, 29, 2, 0, 0)
        assert fmt_datetime(d) == "2011-01-29 02:00:00"

    def test_none_returns_empty(self):
        assert fmt_datetime(None) == ""


# ---------- normalize_text / normalize_document ----------

class TestNormalize:
    def test_strips_whitespace(self):
        assert normalize_text("  hello  ") == "hello"

    def test_none_returns_empty(self):
        assert normalize_text(None) == ""

    def test_removes_null_bytes(self):
        assert normalize_text("ab\x00cd") == "abcd"

    def test_document_removes_punctuation(self):
        assert normalize_document("111,222,333-44") == "11122233344"

    def test_document_none_returns_empty(self):
        assert normalize_document(None) == ""


# ---------- transform_product_categories ----------

class TestTransformProductCategories:
    def test_returns_rows_and_legacy_map(self):
        docs = [{"_id": 4, "codigo": "T", "nome": "Terno", "descricao": "Terno Masculino"}]
        rows, legacy_map = transform_product_categories(docs, "default-uuid")

        assert len(rows) == 1
        row = rows[0]
        assert row["name"] == "Terno"
        assert row["display_name"] == "Terno"
        assert row["product_type"] == "RENTAL"
        assert row["active"] == "true"
        assert row["legacy_id"] == 4
        assert 4 in legacy_map
        assert legacy_map[4] == row["id"]

    def test_empty_docs_returns_empty(self):
        rows, legacy_map = transform_product_categories([], "default-uuid")
        assert rows == []
        assert legacy_map == {}


# ---------- transform_people_from_cliente ----------

class TestTransformPeopleFromCliente:
    def test_mock_cliente_produces_one_person_one_customer(self):
        docs = load_fixtures().get("cliente", [])
        people, customers, addresses, phones, pads, customer_map = transform_people_from_cliente(docs)

        assert len(people) == 1
        assert len(customers) == 1
        assert people[0]["name"] == "Joao Mock da Silva"
        assert people[0]["legacy_id"] == 7001
        assert 7001 in customer_map

    def test_mock_cliente_produces_address_and_phones(self):
        docs = load_fixtures().get("cliente", [])
        people, customers, addresses, phones, pads, customer_map = transform_people_from_cliente(docs)

        assert len(addresses) == 1
        assert addresses[0]["city"] == "MOCK CITY"
        assert len(phones) == 2
        assert phones[0]["phone"] in ("1600000000", "900000000")


# ---------- transform_employees ----------

class TestTransformEmployees:
    def test_mock_funcionario_produces_one_employee(self):
        docs = load_fixtures().get("funcionario", [])
        people, employees, user_accounts, employee_map = transform_people_from_funcionario(docs)

        assert len(people) == 1
        assert len(employees) == 1
        assert len(user_accounts) == 1
        assert people[0]["name"] == "Funcionario Mock"
        assert 8001 in employee_map


# ---------- transform_products_and_rental_items ----------

class TestTransformProductsAndRentalItems:
    def test_mock_roupa_maps_to_correct_category(self):
        docs = load_fixtures().get("roupa", [])
        cat_docs = load_fixtures().get("roupa_tipo", [])
        cat_rows, cat_map = transform_product_categories(cat_docs, "default-cat-id")

        products, rental_items = transform_products_and_rental_items(docs, "default-cat-id", cat_map)

        assert len(products) == 1
        assert len(rental_items) == 1
        # Produto deve ter category_id do roupa_tipo _id=4, nao default
        assert products[0]["category_id"] == cat_map[4]
        assert products[0]["name"] == "VESTIDO MOCK 42"
        assert rental_items[0]["legacy_id"] == 9001

    def test_unknown_tipo_uses_default_category(self):
        docs = [{"_id": 9999, "nome": "ROUPA SEM TIPO", "tipo": 999, "valor": 10.0}]
        products, rental_items = transform_products_and_rental_items(docs, "default-cat-id", {})

        assert products[0]["category_id"] == "default-cat-id"


# ---------- transform_contracts ----------

class TestTransformContracts:
    def test_mock_contrato_produces_contract_item_payments_meta(self):
        fixtures = load_fixtures()

        # Construir mapas necessarios
        cliente_docs = fixtures.get("cliente", [])
        _, _, _, _, _, customer_map = transform_people_from_cliente(cliente_docs)
        customer_name_map = {7001: "Joao Mock da Silva"}

        func_docs = fixtures.get("funcionario", [])
        _, _, _, employee_map = transform_people_from_funcionario(func_docs)

        roupa_docs = fixtures.get("roupa", [])
        cat_docs = fixtures.get("roupa_tipo", [])
        _, cat_map = transform_product_categories(cat_docs, "default-cat-id")
        _, rental_item_rows = transform_products_and_rental_items(roupa_docs, "default-cat-id", cat_map)
        rental_item_map = {}
        for r in rental_item_rows:
            legacy = r.get("legacy_id")
            if legacy != "":
                rental_item_map[str(legacy)] = r["id"]

        contrato_docs = fixtures.get("contrato", [])
        contracts, items, payments, meta = transform_contracts(
            contrato_docs, customer_map, customer_name_map, employee_map, rental_item_map
        )

        # 1 contrato
        assert len(contracts) == 1
        c = contracts[0]
        assert c["legacy_id"] == "5001"
        assert c["customer_name"] == "Joao Mock da Silva"
        assert c["status"] == "FINALIZED"  # baixa=True, situacao=0
        assert c["is_returned"] == "true"

        # Datas devem estar ordenadas: pickup <= event <= return
        assert c["pickup_date"] <= c["event_date"] <= c["return_date"]

        # 1 item de contrato
        assert len(items) == 1
        assert items[0]["description"].startswith("VESTIDO MOCK 42")
        assert items[0]["value"] == 350.0

        # 2 pagamentos
        assert len(payments) == 2
        assert payments[0]["value"] == 250.0
        assert payments[1]["value"] == 100.0
        # pagamentos de contrato FINALIZADO devem ser PAID
        assert payments[0]["status"] == "PAID"
        assert payments[1]["status"] == "PAID"

        # 3 meta rows (sub-itens: M44, SEM SAPATO, MANGA DIR 49CM)
        assert len(meta) == 3
        assert meta[0]["type"] == "OBSERVACAO"

    def test_payment_status_uses_situacao_not_baixa(self):
        """Regression: pagamentos de contrato FINALIZADO devem ser PAID mesmo sem baixa."""
        from datetime import datetime
        docs = [{
            "_id": 1000,
            "tipo": 1,
            "cliente": 7001,
            "retirada": datetime(2010, 10, 27, 2, 0, 0),
            "usa": datetime(2010, 10, 29, 2, 0, 0),
            "devolucao": datetime(2010, 10, 31, 2, 0, 0),
            "hoje": datetime(2010, 10, 27, 2, 0, 0),
            "devolveu": datetime(2010, 10, 31, 2, 0, 0),
            "criado_por": 8001,
            "baixa_por": 8001,
            "baixa": False,
            "situacao": 0,
            "itens": [],
            "pagamentos": [
                {"data": datetime(2010, 10, 27, 2, 0, 0), "forma": 0, "valor": 100.0, "vezes": 1, "funcionario": 8001},
            ],
        }]
        _, _, payments, _ = transform_contracts(docs, {7001: "uuid"}, {7001: "Mock"}, {8001: "emp"}, {})
        assert len(payments) == 1
        assert payments[0]["status"] == "PAID"

    def test_contract_dates_always_sorted(self):
        """Regression: datas legadas fora de ordem devem ser ordenadas."""
        docs = [{
            "_id": 999,
            "tipo": 1,
            "cliente": 7001,
            "retirada": datetime(2026, 11, 12, 2, 0, 0),
            "usa": datetime(2011, 1, 29, 2, 0, 0),
            "devolucao": datetime(2011, 1, 31, 2, 0, 0),
            "hoje": datetime(2011, 1, 18, 2, 0, 0),
            "devolveu": datetime(2011, 1, 31, 2, 0, 0),
            "criado_por": 8001,
            "baixa_por": 8001,
            "baixa": True,
            "situacao": 0,
            "itens": [],
            "pagamentos": [],
        }]
        contracts, _, _, _ = transform_contracts(docs, {7001: "uuid"}, {7001: "Mock"}, {8001: "emp"}, {})
        c = contracts[0]
        # 2026-11-12 vira 2011-12-26 (ano < 100 corrigido), mas aqui ano e 2026
        # sorted deve garantir pickup <= event <= return
        assert c["pickup_date"] <= c["event_date"] <= c["return_date"]

"""Funções de transformação de documentos legados em linhas de tabela."""

import re
from datetime import datetime
from uuid import uuid4

from config import CONTRACT_STATUS_MAP, CONTRACT_TYPE_MAP, PAYMENT_METHOD_MAP

EPOCH = "1970-01-01 00:00:00"
EPOCH_TZ = "1970-01-01 00:00:00+00"


def normalize_text(value) -> str:
    if value is None:
        return ""
    text = str(value).strip()
    # Remover bytes nulos e caracteres de controle inválidos para UTF-8
    text = text.replace("\x00", "").replace("\u0000", "")
    return text


def normalize_email(value) -> str:
    """Retorna email se válido, senão string vazia (NULL via COPY)."""
    if not value:
        return ""
    text = str(value).strip()
    if "@" not in text or " " in text:
        return ""
    return text


def normalize_document(value) -> str:
    """Remove pontuação de CPF/CNPJ; retorna vazio se None."""
    if not value:
        return ""
    return re.sub(r"[^0-9]", "", str(value))


def fmt_datetime(value) -> str:
    if value is None:
        return ""
    if isinstance(value, datetime):
        year = value.year
        if year < 100:
            year += 2000
        return f"{year:04d}-{value.month:02d}-{value.day:02d} {value.hour:02d}:{value.minute:02d}:{value.second:02d}"
    return str(value)


def fmt_date(value) -> str:
    if value is None:
        return ""
    if isinstance(value, datetime):
        year = value.year
        if year < 100:
            year += 2000
        return f"{year:04d}-{value.month:02d}-{value.day:02d}"
    # Tentar normalizar strings de data para YYYY-MM-DD
    s = str(value).strip()
    if not s:
        return ""
    # Se já está em YYYY-MM-DD
    if len(s) >= 10 and s[4] == "-" and s[7] == "-":
        return s[:10]
    # Tentar DD/MM/YYYY
    parts = s.split("/")
    if len(parts) == 3 and len(parts[2]) == 4:
        return f"{parts[2]}-{parts[1].zfill(2)}-{parts[0].zfill(2)}"
    # Tentar MM-DD-YY ou YY-MM-DD
    parts = s.split("-")
    if len(parts) == 3:
        if len(parts[2]) == 2:
            yy = int(parts[2])
            year = f"20{yy:02d}" if yy < 50 else f"19{yy:02d}"
            return f"{year}-{parts[0].zfill(2)}-{parts[1].zfill(2)}"
        if len(parts[0]) == 4:
            return f"{parts[0]}-{parts[1].zfill(2)}-{parts[2].zfill(2)}"
    return s


def new_uuid() -> str:
    return str(uuid4())


# ---------- categories ----------

def transform_categories(documents: list[dict]) -> list[dict]:
    rows = []
    for doc in documents:
        name = normalize_text(doc.get("nome"))
        rows.append({
            "id": new_uuid(),
            "name": name,
            "display_name": name,
            "description": "",
            "product_type": "RENTAL",
            "active": "true",
            "created_at": EPOCH,
            "updated_at": EPOCH,
            "legacy_id": doc.get("_id", ""),
        })
    return rows


def transform_product_categories(documents: list[dict], default_category_id: str) -> tuple[list[dict], dict]:
    """Retorna (category_rows, legacy_map) onde legacy_map mapeia roupa_tipo._id -> category uuid."""
    rows = []
    legacy_map = {}
    for doc in documents:
        cid = new_uuid()
        name = normalize_text(doc.get("nome")) or "SEM NOME"
        legacy_id = doc.get("_id", "")
        rows.append({
            "id": cid,
            "name": name,
            "display_name": name,
            "description": "",
            "product_type": "RENTAL",
            "active": "true",
            "created_at": EPOCH,
            "updated_at": EPOCH,
            "legacy_id": legacy_id,
        })
        if legacy_id is not None:
            legacy_map[legacy_id] = cid
    return rows, legacy_map


# ---------- people + customers + employees + user_accounts ----------

def transform_people_from_cliente(documents: list[dict]) -> tuple[list[dict], list[dict], list[dict], list[dict], list[dict], dict]:
    """Retorna (people_rows, customer_rows, address_rows, phone_rows, pad_rows, customer_legacy_map)."""
    people_rows = []
    customer_rows = []
    address_rows = []
    phone_rows = []
    pad_rows = []
    customer_legacy_map = {}  # legacy_id -> (uuid, name)

    for doc in documents:
        pid = new_uuid()
        legacy_id = doc.get("_id")
        name = normalize_text(doc.get("nome")) or "SEM NOME"
        document = normalize_document(doc.get("cpf") or doc.get("cnpj"))

        people_rows.append({
            "id": pid,
            "legacy_id": legacy_id if legacy_id is not None else "",
            "name": name,
            "document": document,
            "email": normalize_email(doc.get("email")),
            "created_at": EPOCH_TZ,
            "updated_at": EPOCH_TZ,
        })

        customer_rows.append({
            "id": pid,
            "is_authenticated": "true" if doc.get("autenticacao") else "false",
            "notes": normalize_text(doc.get("obs")),
            "created_by_id": "",
        })

        if legacy_id is not None:
            customer_legacy_map[legacy_id] = (pid, name)

        end = doc.get("endereco")
        if isinstance(end, dict) and (end.get("logradouro") or end.get("cidade")):
            addr_id = new_uuid()
            street = normalize_text(end.get("logradouro")) or "N/A"
            address_rows.append({
                "zip_code": normalize_text(end.get("cep")).replace("-", "")[:8],
                "street": street,
                "neighborhood": normalize_text(end.get("bairro")),
                "city": normalize_text(end.get("cidade")) or "N/A",
                "state": normalize_text(end.get("uf"))[:2] or "SP",
                "id": addr_id,
                "is_manual": "true",
            })
            pad_rows.append({
                "id": new_uuid(),
                "person_id": pid,
                "number": normalize_text(end.get("numero")),
                "complement": "",
                "start_date": EPOCH_TZ,
                "end_date": "",
                "address_id": addr_id,
            })

        for phone in (doc.get("fones") or []):
            phone_text = normalize_text(phone)
            if phone_text:
                phone_rows.append({
                    "customer_id": pid,
                    "phone": phone_text[:20],
                    "id": new_uuid(),
                })

    return people_rows, customer_rows, address_rows, phone_rows, pad_rows, customer_legacy_map


def transform_people_from_funcionario(documents: list[dict]) -> tuple[list[dict], list[dict], list[dict], dict]:
    """Retorna (people_rows, employee_rows, user_account_rows, employee_legacy_map)."""
    people_rows = []
    employee_rows = []
    user_account_rows = []
    employee_legacy_map = {}  # legacy_id -> uuid

    for doc in documents:
        pid = new_uuid()
        legacy_id = doc.get("_id")
        name = normalize_text(doc.get("nome"))
        sigla = normalize_text(doc.get("sigla"))

        people_rows.append({
            "id": pid,
            "legacy_id": legacy_id if legacy_id is not None else "",
            "name": name,
            "document": "",
            "email": "",
            "created_at": EPOCH_TZ,
            "updated_at": EPOCH_TZ,
        })

        employee_rows.append({
            "id": pid,
            "initials": (sigla or name[:3].upper())[:10],
            "role_level": doc.get("privilegio", 1),
        })

        user_account_rows.append({
            "id": pid,
            "username": (sigla or name)[:50],
            "password": normalize_text(doc.get("senha")) or "legacy",
            "pin": "",
            "is_active": "true" if doc.get("ativo") else "false",
            "password_changed_at": "",
            "issuer_cnpj": "",
        })

        if legacy_id is not None:
            employee_legacy_map[legacy_id] = pid

    return people_rows, employee_rows, user_account_rows, employee_legacy_map


# ---------- products + rental_items ----------

def transform_products_and_rental_items(documents: list[dict], default_category_id: str, category_legacy_map: dict = None) -> tuple[list[dict], list[dict]]:
    """Retorna (product_rows, rental_item_rows)."""
    product_rows = []
    rental_item_rows = []
    if category_legacy_map is None:
        category_legacy_map = {}

    for doc in documents:
        pid = new_uuid()
        legacy_id = doc.get("_id")
        name = normalize_text(doc.get("nome")) or "SEM NOME"
        valor = doc.get("valor", 0.0)
        if valor is None or valor <= 0:
            valor = 0.01

        tipo = doc.get("tipo")
        category_id = category_legacy_map.get(tipo, default_category_id)

        product_rows.append({
            "id": pid,
            "category_id": category_id,
            "name": name,
            "size": normalize_text(doc.get("tamanho")),
            "color": normalize_text(doc.get("cor")),
            "brand": "",
            "value": valor,
            "description": normalize_text(doc.get("obs")),
            "created_at": fmt_datetime(doc.get("data")) or EPOCH,
            "updated_at": EPOCH,
        })

        status = "RENTED" if doc.get("locado") else "AVAILABLE"
        if doc.get("status") == 0 and not doc.get("no_estoque"):
            status = "INACTIVE"

        rental_item_rows.append({
            "id": pid,
            "legacy_id": legacy_id if legacy_id is not None else "",
            "status": status,
            "notes": normalize_text(doc.get("obs")),
            "condition": "",
            "last_rental_date": "",
            "rental_count": doc.get("nloc", 0),
            "maintenance_due_date": "",
        })

    return product_rows, rental_item_rows


# ---------- rental_contracts + items + payments + meta ----------

def transform_contracts(documents: list[dict], customer_map: dict, customer_name_map: dict, employee_map: dict, rental_item_map: dict, default_customer_id: str = "") -> tuple[list[dict], list[dict], list[dict], list[dict]]:
    """Retorna (contract_rows, contract_item_rows, payment_rows, meta_rows)."""
    contract_rows = []
    contract_item_rows = []
    payment_rows = []
    meta_rows = []

    for doc in documents:
        cid = new_uuid()
        legacy_id = str(doc.get("_id"))
        cliente_legacy = doc.get("cliente")
        customer_uuid = customer_map.get(cliente_legacy, default_customer_id)
        customer_name = customer_name_map.get(cliente_legacy, "CLIENTE LEGADO") or "CLIENTE LEGADO"
        criado_por = doc.get("criado_por")
        employee_uuid = employee_map.get(criado_por, "")

        # Garantir pickup_date <= event_date <= return_date (check constraint)
        pickup = fmt_date(doc.get("retirada")) or "1970-01-01"
        event = fmt_date(doc.get("usa")) or pickup
        return_d = fmt_date(doc.get("devolucao")) or event
        # Ordenar as três datas para satisfazer check constraint
        sorted_dates = sorted([pickup, event, return_d])
        pickup, event, return_d = sorted_dates[0], sorted_dates[1], sorted_dates[2]

        contract_rows.append({
            "id": cid,
            "legacy_id": legacy_id,
            "contract_type": CONTRACT_TYPE_MAP.get(doc.get("tipo", 0), 0),
            "customer_name": customer_name,
            "customer_document": "",
            "customer_id": customer_uuid,
            "created_by_employee_id": employee_uuid,
            "returned_by_employee_id": employee_map.get(doc.get("baixa_por"), ""),
            "pickup_date": pickup,
            "event_date": event,
            "return_date": return_d,
            "actual_return_date": fmt_date(doc.get("devolveu")),
            "is_returned": "true" if doc.get("baixa") else "false",
            "status": CONTRACT_STATUS_MAP.get(doc.get("situacao", 0), "DRAFT"),
            "notes": normalize_text(doc.get("comunicado")),
            "created_at": fmt_datetime(doc.get("hoje")) or EPOCH_TZ,
            "parent_contract_id": "",
            "replaced_by_contract_id": "",
        })

        for item in (doc.get("itens") or []):
            item_id = new_uuid()
            codigo = normalize_text(item.get("codigo"))
            rental_item_uuid = rental_item_map.get(codigo, "")
            atendente = item.get("atendente")
            attendant_uuid = employee_map.get(atendente, "")
            item_value = item.get("valor", 0.0)
            if item_value is None or item_value <= 0:
                item_value = 0.01

            contract_item_rows.append({
                "id": item_id,
                "contract_id": cid,
                "rental_item_id": rental_item_uuid,
                "legacy_product_code": codigo,
                "description": normalize_text(item.get("descricao")) or "SEM DESCRICAO",
                "value": item_value,
                "is_delivered": "true" if item.get("entregue") else "false",
                "attendant_employee_id": attendant_uuid,
                "returned": "true" if doc.get("baixa") else "false",
                "returned_at": fmt_datetime(doc.get("devolveu")) if doc.get("baixa") else "",
                "returned_by_name": "",
            })

            for sub in (item.get("sub") or []):
                sub_text = normalize_text(sub)
                if sub_text:
                    meta_rows.append({
                        "id": new_uuid(),
                        "contract_item_id": item_id,
                        "type": "OBSERVACAO",
                        "description": sub_text,
                        "accessory_id": "",
                        "returned": "false",
                        "returned_at": "",
                    })

        for idx, pag in enumerate((doc.get("pagamentos") or []), start=1):
            func_legacy = pag.get("funcionario")
            pay_date = fmt_date(pag.get("data")) or fmt_date(doc.get("hoje")) or "1970-01-01"
            payment_rows.append({
                "id": new_uuid(),
                "contract_id": cid,
                "installment_number": idx,
                "payment_date": pay_date,
                "payment_method": PAYMENT_METHOD_MAP.get(pag.get("forma", 0), "CASH"),
                "value": pag.get("valor", 0.0) or 0.01,
                "installments": pag.get("vezes", 1) or 1,
                "processed_by_employee_id": employee_map.get(func_legacy, ""),
                "status": "PAID" if doc.get("baixa") else "PENDING",
            })

    return contract_rows, contract_item_rows, payment_rows, meta_rows


# ---------- conf ----------

def extract_issuer_cnpj(conf_docs: list[dict]) -> str:
    if not conf_docs:
        return ""
    return normalize_document(conf_docs[0].get("cnpj"))

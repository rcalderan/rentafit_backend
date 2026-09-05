"""Configurações e mapeamentos da migração noivabd -> Rentafit."""

# Ordem topológica de inserção (respeita FKs).
TABLES = [
    "categories",
    "people",
    "customers",
    "employees",
    "user_accounts",
    "addresses",
    "person_address_details",
    "customer_phones",
    "products",
    "rental_items",
    "rental_contracts",
    "rental_contract_items",
    "rental_payments",
    "rental_contract_item_meta",
]

# Colunas exatas de cada tabela no PostgreSQL (ordem importa para COPY).
CSV_HEADERS = {
    "categories": ["id", "name", "display_name", "description", "product_type", "active", "created_at", "updated_at", "legacy_id"],
    "people": ["id", "legacy_id", "name", "document", "email", "created_at", "updated_at"],
    "customers": ["id", "is_authenticated", "notes", "created_by_id"],
    "employees": ["id", "initials", "role_level"],
    "user_accounts": ["id", "username", "password", "pin", "is_active", "password_changed_at", "issuer_cnpj"],
    "addresses": ["zip_code", "street", "neighborhood", "city", "state", "id", "is_manual"],
    "person_address_details": ["id", "person_id", "number", "complement", "start_date", "end_date", "address_id"],
    "customer_phones": ["customer_id", "phone", "id"],
    "products": ["id", "category_id", "name", "size", "color", "brand", "value", "description", "created_at", "updated_at"],
    "rental_items": ["id", "legacy_id", "status", "notes", "condition", "last_rental_date", "rental_count", "maintenance_due_date"],
    "rental_contracts": ["id", "legacy_id", "contract_type", "customer_name", "customer_document", "customer_id", "created_by_employee_id", "returned_by_employee_id", "pickup_date", "event_date", "return_date", "actual_return_date", "is_returned", "status", "notes", "created_at", "parent_contract_id", "replaced_by_contract_id"],
    "rental_contract_items": ["id", "contract_id", "rental_item_id", "legacy_product_code", "description", "value", "is_delivered", "attendant_employee_id", "returned", "returned_at", "returned_by_name"],
    "rental_payments": ["id", "contract_id", "installment_number", "payment_date", "payment_method", "value", "installments", "processed_by_employee_id", "status"],
    "rental_contract_item_meta": ["id", "contract_item_id", "type", "description", "accessory_id", "returned", "returned_at"],
}

# Mapeamento de forma de pagamento legado -> enum do PostgreSQL.
PAYMENT_METHOD_MAP = {0: "CASH", 1: "CREDIT_CARD", 2: "DEBIT_CARD", 3: "BANK_TRANSFER", 4: "PIX"}

# Mapeamento de situacao do contrato -> status do PostgreSQL.
CONTRACT_STATUS_MAP = {0: "FINALIZED", 1: "SIGNED", 2: "DRAFT", 3: "REVISION", 4: "CLOSED"}

# Mapeamento de tipo de contrato legado -> inteiro.
CONTRACT_TYPE_MAP = {0: 0, 1: 1, 2: 2}

# Campos que devem ser NULL (não string vazia) quando ausentes.
# FKs opcionais, timestamps opcionais, e campos nullable.
NULL_WHEN_EMPTY = {
    "people": {"legacy_id", "document", "email"},
    "customers": {"created_by_id"},
    "user_accounts": {"pin", "password_changed_at", "issuer_cnpj"},
    "person_address_details": {"end_date", "number", "complement"},
    "products": {"size", "color", "brand", "description"},
    "rental_items": {"condition", "last_rental_date", "maintenance_due_date", "notes"},
    "rental_contracts": {"actual_return_date", "created_by_employee_id", "returned_by_employee_id", "parent_contract_id", "replaced_by_contract_id", "customer_document"},
    "rental_contract_items": {"rental_item_id", "attendant_employee_id", "returned_at", "returned_by_name"},
    "rental_payments": {"processed_by_employee_id"},
    "rental_contract_item_meta": {"accessory_id", "returned_at"},
    "addresses": {"zip_code", "neighborhood"},
    "categories": {"legacy_id"},
}

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

# De-para de funcionarios legados (sigla -> ccli/nome/email/cpf conhecidos).
# ccli = codigo do cliente no cadastro legado; None => alocar proximo id livre.
# A ordem do dict define a ordem de alocacao dos ids livres (CL primeiro, para
# reservar o menor id livre — hoje o 5).
FUNCIONARIO_DEPARA = {
    "RI": {"ccli": 1,     "name": "Richard Calderan",          "email": "richardcck@hotmail.com",      "cpf": "326972219840"},
    "RE": {"ccli": 2,     "name": "RENATA EMIKO",              "email": "emikacal@gmail.com",          "cpf": "29223700892"},
    "NE": {"ccli": 4,     "name": "NELSON VAZ",                "email": "nelsonvaz@noivamodas.com.br", "cpf": "19174306804"},
    "CL": {"ccli": None,  "name": "CLEYTON CARVALHO CALDERAN", "email": "cleytoncalderan@gmail.com",   "cpf": "21976776830"},
    "SI": {"ccli": 15193, "name": "SIMONE LEIKO KAIBARA CALDERAN", "email": "simone@noivamodas.com.br", "cpf": "25884738811"},
    "IA": {"ccli": 9965,  "name": "IAMA",                      "email": "iama@noivamodas.com.br",      "cpf": ""},
    "HI": {"ccli": None,  "name": "HIDEKO",                    "email": "hideko@noivamodas.com.br",    "cpf": ""},
    "AK": {"ccli": None,  "name": "AKEMI",                     "email": "akemi@noivamodas.com.br",     "cpf": ""},
    "CA": {"ccli": None,  "name": "CAMILA",                    "email": "camila@noivamodas.com.br",    "cpf": ""},
    "VA": {"ccli": None,  "name": "VALERIA",                   "email": "valeria@noivamodas.com.br",   "cpf": ""},
}

# Funcionarios que nao viram employee: ADM e o admin preservado via
# fetch_admin/restore_admin; N/A ("Desconhecido") e placeholder sem login.
FUNCIONARIO_SKIP_SIGLAS = {"ADM", "N/A"}

# Dominio dos emails auto-gerados para funcionarios fora do de-para.
FUNCIONARIO_AUTO_EMAIL_DOMAIN = "noivamodas.com.br"

# UUID do admin semeado por V2__insert-admin-user.sql (funcionario ADM do
# legado mapeia para ele, mantendo FKs de contratos).
ADMIN_UUID = "0194269a-0000-7000-8000-000000000001"

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

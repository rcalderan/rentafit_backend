# Rental Component — API Documentation

> Sistema de Locação — Componente responsável por orquestrar o ciclo de vida dos contratos de aluguel de trajes.

---

## Índice

1. [Fluxo de Trabalho](#fluxo-de-trabalho)
2. [Regras de Negócio](#regras-de-negócio)
3. [Diagrama ER (Banco de Dados)](#diagrama-er)
4. [Diagrama de Classes](#diagrama-de-classes)
5. [Endpoints da API](#endpoints-da-api)
6. [Enums de Referência](#enums-de-referência)

---

## Fluxo de Trabalho

### Ciclo de Vida do Contrato

```
[People] Cliente cadastrado
[Product] Roupa cadastrada
     │
     ▼
CREATE DRAFT ──────────────────── Proposta criada
     │  (sem reserva de estoque)
     │
SIGN ───────────────────────────── Assinado pelo cliente
     │  + checagem de conflitos     ← BLOCKING = 422 | WARNING = response.warnings
     │  (sem reserva de estoque)
     │
FINALIZE ───────────────────────── Contrato fechado
     │  + checagem de conflitos
     │  + RentalItem → RESERVED
     │  + Accessory.stock.reserve()
     │
DELIVER ITEM (por item) ─────────── Item retirado pelo cliente
     │  + RentalItem → RENTED
     │
PROCESS RETURN ─────────────────── Devolução registrada
     │  + RentalItem → MAINTENANCE
     │  + Accessory.stock.release()
     │
[Product] Item aprovado em manutenção
     └── RentalItem → AVAILABLE    ← Gerenciado pelo componente Product
```

### Ciclo de Vida dos Itens Físicos

| Estado       | Significado                                         |
|--------------|-----------------------------------------------------|
| `AVAILABLE`  | Disponível para locação                             |
| `RESERVED`   | Reservado — contrato FINALIZED, aguardando retirada |
| `RENTED`     | Retirado pelo cliente — em uso                      |
| `MAINTENANCE`| Devolvido — em revisão/lavanderia/ajuste de costura |

### Ciclo de Vida dos Acessórios (Estoque por Quantidade)

| Evento     | Ação                                   |
|------------|----------------------------------------|
| FINALIZE   | `Stock.reserve(1)` — qty_available--, qty_reserved++ |
| RETURN     | `Stock.release(1)` — qty_reserved--, qty_available++ |

---

## Regras de Negócio

### Transições de Status

| De       | Para      | Endpoint              | Validações adicionais                          |
|----------|-----------|-----------------------|------------------------------------------------|
| DRAFT    | SIGNED    | `PATCH /{id}/sign`    | Conflitos de reserva (BLOCKING/WARNING)        |
| SIGNED   | FINALIZED | `PATCH /{id}/finalize`| Ao menos 1 item; conflitos de reserva          |

- `update()` bloqueado se `status != DRAFT`
- `duplicate()` cria novo DRAFT com snapshot do cliente atualizado

### Regras de Datas (validadas no backend)

```
pickupDate ≤ eventDate ≤ returnDate
```
> Sugestão de datas e ajuste de feriados é responsabilidade do frontend.

### Regras de Conflito de Reserva

| Situação                         | Comportamento              |
|----------------------------------|----------------------------|
| Mesmo `eventDate` que outro contrato SIGNED/FINALIZED | **BLOCKING** — 422 |
| `eventDate` dentro de ±3 dias    | **WARNING** — `response.warnings[]` |
| `eventDate` a mais de 3 dias     | Sem alerta                 |
| Item com `rentalItemId = null`   | Ignorado                   |
| Próprio contrato na busca        | Excluído automaticamente   |

> Checagem ativada apenas em `sign()` e `finalize()`. Contratos DRAFT são ignorados na busca.

### Regras de Pagamento

| Regra                                          | Comportamento    |
|------------------------------------------------|------------------|
| `paymentDate > eventDate`                      | ValidationException |
| Soma PENDING+PAID > totalValue dos itens       | ValidationException |
| Soma PENDING+PAID < totalValue (no `update`)   | ✅ Parcela PENDING/PIX criada automaticamente |
| Soma PENDING+PAID ≠ totalValue (no `create`)   | ValidationException |
| Mais de 24 parcelas                            | ValidationException |
| `addPayment` após FINALIZED                    | ✅ Permitido      |
| `updatePayment`/`cancelPayment` após FINALIZED | ❌ Bloqueado      |

#### Auto-criação de parcela no `update()`

Quando o contrato é atualizado e a soma das parcelas informadas é **menor** que o valor total dos itens, o sistema cria automaticamente uma nova parcela:

| Campo               | Valor                                                  |
|---------------------|--------------------------------------------------------|
| `installmentNumber` | Próximo número sequencial (max existente + 1)          |
| `paymentDate`       | Última data + 30 dias, limitado ao `eventDate`         |
| `paymentMethod`     | `PIX`                                                  |
| `value`             | Diferença (totalItems − totalPayments)                  |
| `status`            | `PENDING`                                              |

> Essa regra **não se aplica ao `create()`**, onde a soma das parcelas deve ser exata.

### Snapshot do Cliente

Os campos `customerName` e `customerDocument` são gravados **uma única vez** na criação do contrato e nunca atualizados. Isso garante que o contrato histórico permaneça legível mesmo após alterações no cadastro do cliente. Para alterar os dados do cliente em um contrato FINALIZED, use `duplicate()`.

---

## Diagrama ER

```
┌─────────────────────────────────────────────────────────────┐
│                      rental_contracts                        │
├─────────────────────┬───────────────────────────────────────┤
│ id (UUID, PK)       │ status (DRAFT/SIGNED/FINALIZED)        │
│ legacy_id (INT)     │ is_returned (BOOL)                     │
│ contract_type (INT) │ notes (TEXT)                           │
│ customer_name *     │ created_at (TIMESTAMPTZ)               │
│ customer_document * │                                        │
│ customer_id (UUID)──┤──► customers(id) ON DELETE RESTRICT   │
│ created_by_emp_id ──┤──► employees(id) ON DELETE SET NULL   │
│ returned_by_emp_id──┤──► employees(id) ON DELETE SET NULL   │
│ pickup_date (DATE)  │                                        │
│ event_date (DATE)   │ ← INDEXED (conflict check)            │
│ return_date (DATE)  │                                        │
│ actual_return_date  │                                        │
└──────────┬──────────┴─────────────────────────────────────-─┘
           │ 1:N (CASCADE DELETE)
           ▼
┌──────────────────────────────────────────────────────────────┐
│                   rental_contract_items                       │
├──────────────────────────────────────────────────────────────┤
│ id (UUID, PK)                                                 │
│ contract_id (UUID) ──────────► rental_contracts(id)          │
│ rental_item_id (UUID, nullable) ─► rental_items(id) SET NULL │
│ legacy_product_code (VARCHAR) * snapshot                     │
│ description (VARCHAR)         * snapshot                     │
│ value (DECIMAL)               ← editável                     │
│ is_delivered (BOOL)                                          │
│ attendant_employee_id ─────────► employees(id) SET NULL      │
└──────────┬───────────────────────────────────────────────────┘
           │ 1:N (CASCADE DELETE)
           ▼
┌──────────────────────────────────────────────────────────────┐
│                  rental_contract_item_meta                    │
├──────────────────────────────────────────────────────────────┤
│ id (UUID, PK)                                                 │
│ contract_item_id (UUID) ────────► rental_contract_items(id)  │
│ type (ACESSORIO / OBSERVACAO)                                 │
│ description (TEXT)                                           │
│ accessory_id (UUID, nullable) ──► accessories(id) SET NULL   │
│                                   ^ quando preenchido:       │
│                                     → reserveStock()         │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                      rental_payments                          │
├──────────────────────────────────────────────────────────────┤
│ id (UUID, PK)                                                 │
│ contract_id (UUID) ──────────► rental_contracts(id)          │
│ installment_number (INT, 1-24)                               │
│ payment_date (DATE)                                          │
│ payment_method (CASH/PIX/CREDIT_CARD/DEBIT_CARD/TRANSFER)   │
│ value (DECIMAL > 0)                                          │
│ installments (INT, 1-24)      ← número de vezes no cartão    │
│ processed_by_employee_id ──────► employees(id) SET NULL      │
│ status (PENDING/PAID/CANCELLED)                              │
└──────────────────────────────────────────────────────────────┘

* = campo de snapshot (imutável após criação)
```

---

## Diagrama de Classes

```
                    ┌─────────────────────┐
                    │   <<interface>>      │
                    │   CustomerPort       │
                    │──────────────────────│
                    │ + findById(UUID)     │
                    │   → CustomerSnapshot │
                    └──────────┬───────────┘
                               │ implements
                    ┌──────────▼───────────┐
                    │   CustomerAdapter    │
                    │   (rental/adapter)   │
                    │  injects:            │
                    │  CustomerRepository  │
                    └──────────────────────┘

                    ┌─────────────────────┐
                    │   <<interface>>      │
                    │   RentalItemPort     │
                    │──────────────────────│
                    │ + findById / findByLegacyId │
                    │ + updateStatus()     │
                    │ + isAvailable()      │
                    └──────────┬───────────┘
                               │ implements
                    ┌──────────▼───────────┐
                    │  RentalItemAdapter   │
                    │  injects:            │
                    │  RentalItemRepository│
                    └──────────────────────┘

                    ┌─────────────────────┐
                    │   <<interface>>      │
                    │   AccessoryPort      │
                    │──────────────────────│
                    │ + isAvailableInStock │
                    │ + reserveStock()     │
                    │ + releaseStock()     │
                    └──────────┬───────────┘
                               │ implements
                    ┌──────────▼───────────┐
                    │  AccessoryAdapter    │
                    │  injects:            │
                    │  AccessoryRepository │
                    │  StockService        │
                    └──────────────────────┘

┌───────────────────────────────────────────────────────────────┐
│                    RentalContractController                    │
│  /api/v1/rental/contracts                                     │
└────────────────────────────┬──────────────────────────────────┘
                             │ uses
┌────────────────────────────▼──────────────────────────────────┐
│                   RentalContractService                        │
│  create / update / sign / finalize / processReturn / duplicate│
└────┬──────────────┬───────────────────┬────────────────────────┘
     │              │                   │
     ▼              ▼                   ▼
RentalContractValidator  RentalWorkflowService  RentalMapper
     │                        │
     ▼                        ├── RentalItemPort
ItemConflictChecker            └── AccessoryPort
     │
     ▼
RentalContractItemRepository
  (JPQL conflict query)

┌───────────────────────────────────────────────────────────────┐
│                    RentalPaymentController                     │
│  /api/v1/rental/contracts/{contractId}/payments               │
└────────────────────────────┬──────────────────────────────────┘
                             │ uses
┌────────────────────────────▼──────────────────────────────────┐
│                   RentalPaymentService                        │
│  addPayment / updatePayment / cancelPayment                   │
└───────────────────────────────────────────────────────────────┘
```

---

## Endpoints da API

### Rental Contracts — `/api/v1/rental/contracts`

| Método | Path                              | Descrição                              | Status de sucesso |
|--------|-----------------------------------|----------------------------------------|-------------------|
| GET    | `/`                               | Listar contratos (paginado)            | 200               |
| GET    | `/{id}`                           | Buscar por ID                          | 200               |
| GET    | `/byCustomer/{customerId}`        | Listar por cliente                     | 200               |
| POST   | `/`                               | Criar proposta (DRAFT)                 | 201               |
| PUT    | `/{id}`                           | Atualizar (apenas DRAFT)               | 200               |
| PATCH  | `/{id}/sign`                      | Assinar (DRAFT → SIGNED)               | 200               |
| PATCH  | `/{id}/finalize`                  | Finalizar (SIGNED → FINALIZED)         | 200               |
| PATCH  | `/{id}/return`                    | Processar devolução                    | 200               |
| PATCH  | `/{id}/items/{itemId}/deliver`    | Confirmar entrega de item              | 200               |
| POST   | `/{id}/duplicate`                 | Duplicar como novo DRAFT               | 201               |

### Rental Payments — `/api/v1/rental/contracts/{contractId}/payments`

| Método | Path             | Descrição                                    | Status de sucesso |
|--------|------------------|----------------------------------------------|-------------------|
| GET    | `/`              | Listar parcelas                              | 200               |
| POST   | `/`              | Adicionar parcela (sempre permitido)         | 201               |
| PUT    | `/{paymentId}`   | Atualizar parcela (bloqueado após FINALIZED) | 200               |
| DELETE | `/{paymentId}`   | Cancelar parcela (bloqueado após FINALIZED)  | 204               |

### Exemplo: Response com warnings

```json
{
  "id": "...",
  "status": "SIGNED",
  "statusDescription": "Assinado",
  "warnings": [
    "Item 'Vestido de Noiva': conflito de reserva com contrato abc-123 (evento em 2026-06-05) — ALERTA — dentro de 3 dias"
  ]
}
```

### Exemplo: Response sem warnings (campo omitido)

```json
{
  "id": "...",
  "status": "SIGNED",
  "statusDescription": "Assinado"
}
```

---

## Enums de Referência

### ContractStatus
| Valor      | Código Legado | Descrição        |
|------------|---------------|------------------|
| `DRAFT`    | 0             | Proposta         |
| `SIGNED`   | 1             | Assinado         |
| `FINALIZED`| 2             | Contrato fechado |

### PaymentMethod
| Valor           | Código Legado | Label              |
|-----------------|---------------|--------------------|
| `CASH`          | 0             | Dinheiro           |
| `PIX`           | 1             | PIX                |
| `CREDIT_CARD`   | 2             | Cartão de Crédito  |
| `DEBIT_CARD`    | 3             | Cartão de Débito   |
| `BANK_TRANSFER` | 4             | Transferência      |

### PaymentStatus
| Valor       | Código Legado | Descrição |
|-------------|---------------|-----------|
| `PENDING`   | 0             | Pendente  |
| `PAID`      | 1             | Pago      |
| `CANCELLED` | 2             | Cancelado |

### ItemMetaType
| Valor       | Descrição                                               |
|-------------|---------------------------------------------------------|
| `ACESSORIO` | Acessório catalogado (com accessoryId) ou textual (sem) |
| `OBSERVACAO`| Observação de texto livre                               |


# Módulo Product - Rentafit

## 📋 Visão Geral

O módulo Product gerencia todos os produtos da plataforma Rentafit, incluindo:

- **RentalItem**: Itens individuais para aluguel (vestidos, ternos, etc.)
- **RetailProduct**: Produtos para venda no varejo
- **Accessory**: Acessórios complementares (bolsas, sapatos, gravatas)

## 🏗️ Estrutura

```
br.com.rentafit.product/
├── controller/
│   ├── ProductController.java
│   ├── StockController.java
│   └── CategoryController.java
├── domain/
│   ├── Category.java
│   ├── Product.java (abstract)
│   ├── RentalItem.java
│   ├── RetailProduct.java
│   ├── Accessory.java
│   ├── Stock.java
│   ├── StockMovement.java
│   └── enums/
│       ├── ProductTypeCategory.java
│       ├── ProductStatus.java
│       ├── ProductCondition.java
│       └── StockMovementType.java
├── dto/
│   ├── ProductDTO.java
│   ├── ProductDetailsDTO.java
│   ├── StockDTO.java
│   ├── StockMovementDTO.java
│   └── CategoryDTO.java
├── repository/
│   ├── ProductRepository.java
│   ├── StockRepository.java
│   ├── StockMovementRepository.java
│   └── CategoryRepository.java
└── service/
    ├── ProductService.java
    ├── StockService.java
    └── CategoryService.java
```

## 🗂️ Entidades Principais

### Category
Orquestra todas as categorias de produtos (aluguel, venda, acessórios).

**Tipos**:
- `RENTAL`: Produtos para aluguel (itens únicos)
- `RETAIL`: Produtos para venda (controle por estoque)
- `ACCESSORY`: Acessórios (podem ser aluguel ou venda)

### Product (Abstract)
Classe base para todos os produtos com campos comuns:
- `id`, `name`, `category`, `size`, `color`, `brand`, `value`, `description`

### RentalItem
Itens individuais rastreáveis para aluguel. **SEM controle de estoque**.
- Campos: `legacyId`, `status`, `notes`, `minRentalDays`, `condition`, `lastRentalDate`, `rentalCount`, `maintenanceDueDate`

### RetailProduct
Produtos para venda no varejo. **COM controle de estoque obrigatório**.
- Campos: `sku`, `details`, relacionamento com `Stock`

### Accessory
Acessórios complementares. A categoria determina tudo:
- `BOLSAS_RENTAL` → item numerado, sem estoque
- `GRAVATAS_RETAIL` → controle por estoque, sem legacyId

### Stock
Controle de inventário para RetailProduct e Accessories RETAIL.
- Campos: `quantityAvailable`, `quantityReserved`, `quantityTotal`, `minStockLevel`, `location`
- Métodos: `reserve()`, `release()`, `addStock()`, `removeStock()`, `isLowStock()`

### StockMovement
Auditoria imutável de todas as movimentações de estoque.
- Tipos: `ENTRADA`, `SAIDA`, `RESERVA`, `LIBERACAO`, `AJUSTE`, `PERDA`

## 🔌 APIs Principais

### Products
- `POST /api/v1/products` - Criar produto
- `GET /api/v1/products/{id}` - Consultar por ID
- `GET /api/v1/products` - Listar com paginação
- `PUT /api/v1/products/{id}` - Atualizar
- `DELETE /api/v1/products/{id}` - Deletar

### Stock
- `GET /api/v1/stock/{productId}` - Consultar estoque
- `GET /api/v1/stock/low` - Produtos com estoque baixo
- `POST /api/v1/stock/reserve` - Reservar estoque
- `POST /api/v1/stock/release` - Liberar reserva
- `POST /api/v1/stock/add` - Adicionar estoque
- `POST /api/v1/stock/remove` - Remover estoque

### Categories
- `POST /api/v1/categories` - Criar categoria
- `GET /api/v1/categories/{id}` - Consultar por ID
- `GET /api/v1/categories` - Listar todas
- `GET /api/v1/categories/type/{type}` - Listar por tipo
- `GET /api/v1/categories/active` - Listar ativas
- `PUT /api/v1/categories/{id}` - Atualizar
- `DELETE /api/v1/categories/{id}` - Deletar

## 🗄️ Migrations

- `V6__create-table-categories.sql` - Tabela de categorias
- `V7__create-table-products.sql` - Tabela de produtos (herança single-table)
- `V8__create-table-stock.sql` - Tabela de estoque
- `V9__create-table-stock-movements.sql` - Tabela de movimentações
- `V10__insert-product-categories.sql` - Dados pré-configurados

## 📊 Regras de Negócio

### Estoque
- Apenas `RetailProduct` e `Accessory` RETAIL têm estoque
- `RentalItem` e `Accessory` RENTAL não têm estoque (gerenciado pelo módulo de aluguel)
- Estoque é criado automaticamente ao cadastrar RetailProduct

### Transações
- Reserva reduz `quantityAvailable`, aumenta `quantityReserved`
- Liberação reverte a reserva
- Cada operação gera um `StockMovement` para auditoria

### Validações
- `quantityTotal = quantityAvailable + quantityReserved` (sempre)
- `quantityAvailable >= 0`, `quantityReserved >= 0`, `quantityTotal >= 0`
- Produto sem estoque não pode ser vendido

## 🚀 Como Usar

### Criar um Produto de Aluguel
```bash
POST /api/v1/products
{
  "name": "Vestido Rosa",
  "categoryId": "uuid-festa-rental",
  "size": "M",
  "color": "Rosa",
  "brand": "Designer",
  "value": 150.00,
  "description": "Vestido de festa rosa",
  "minRentalDays": 1,
  "notes": "Novo, sem manchas"
}
```

### Criar um Produto de Venda
```bash
POST /api/v1/products
{
  "name": "Gravata Vermelha",
  "categoryId": "uuid-gravatas-retail",
  "size": "P",
  "color": "Vermelha",
  "brand": "Marca X",
  "value": 45.00,
  "sku": "GRAVATA-001",
  "details": "Gravata de seda pura"
}
```

### Reservar Estoque
```bash
POST /api/v1/stock/reserve?productId=uuid&quantity=5&userId=uuid
```

## 📝 Notas de Desenvolvimento

- Entidades seguem padrão do módulo `people`
- DTOs usam records do Java 16+
- Migrations Flyway automáticas
- Documentação Swagger/OpenAPI completa
- Preparado para futura migração do MongoDB

## 🔗 Relacionamentos com Outros Módulos

- **people**: Referência a `Employee` para rastreamento de movimentações
- **billing**: Futura integração para controle de preços dinâmicos
- **rental**: Futura integração para gerenciar disponibilidade de RentalItem

## ✅ Status de Implementação

- ✅ Entidades JPA
- ✅ Repositories
- ✅ Services
- ✅ Controllers (REST APIs)
- ✅ DTOs
- ✅ Migrations Flyway
- ✅ Enums
- ✅ Documentação Swagger

## 🔄 Próximas Etapas

1. Testes unitários e integração
2. Validações adicionais
3. Implementação do módulo de Aluguel
4. Scripts de migração de dados legados
5. Relatórios de inventário

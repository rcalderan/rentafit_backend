# Rentafit - Sistema de Gestão de Locação de Equipamentos

API RESTful para gerenciamento completo de uma locadora de trajes e equipamentos fitness: clientes, funcionários, catálogo de produtos, contratos de locação com ciclo de vida completo, controle de estoque e emissão de NFS-e (Portal Nacional, São Carlos/GINFES e Via/Serpro).

## 🚀 Tecnologias

- **Java 21 LTS**
- **Spring Boot 3.4.1**
- **Spring Security** (JWT + RSA + AES)
- **Spring Data JPA**
- **Spring WebFlux / Reactor** (integrações NFS-e assíncronas)
- **PostgreSQL 14**
- **Flyway** (migrations)
- **Lombok**
- **Swagger/OpenAPI 3**
- **Docker & Docker Compose**
- **JUnit 5 + Mockito** (testes)

---

## 📋 Pré-requisitos

- Java 21 LTS JDK
- Docker e Docker Compose
- Maven 3.8+
- IntelliJ IDEA (recomendado) ou outra IDE Java

---

## 🐳 Configuração do Banco de Dados

### 1. Iniciar o PostgreSQL via Docker

O projeto utiliza um container PostgreSQL já existente chamado `dev-postgresql`:

```bash
docker ps
```

Você deve ver o container `dev-postgresql` rodando na **porta 5433**.

Se o container não estiver rodando, inicie-o:

```bash
docker-compose up -d
```

### 2. Configuração do Docker Compose

O arquivo `docker-compose.yaml` está configurado para usar:

```yaml
Container: dev-postgresql
Imagem: postgres:14-alpine
Porta: 5433:5432
Database: rentafit
Usuário: postgres
Senha: 1234567
```

### 3. Acessar pgAdmin (Opcional)

Se você tiver o pgAdmin rodando no Docker:

1. Acesse: http://localhost:5050
2. Adicione servidor:
   - **Host:** localhost
   - **Port:** 5433
   - **Database:** rentafit
   - **Username:** postgres
   - **Password:** 1234567

---

## ⚙️ Configuração da Aplicação

### Perfis Spring

O projeto está configurado para usar o perfil **`local`** para desenvolvimento.

#### application.properties (Produção)
```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}/${DB_NAME:rentafit}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

#### application-local.properties (Desenvolvimento)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/rentafit
spring.datasource.username=postgres
spring.datasource.password=1234567
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

### Configurar IntelliJ IDEA

**Run → Edit Configurations → Spring Boot**

- **Active profiles:** `local`

Ou via:
- **VM options:** `-Dspring.profiles.active=local`
- **Program arguments:** `--spring.profiles.active=local`

---

## 🗃️ Estrutura do Banco de Dados

### Migrations Flyway

Localizadas em: `src/main/resources/db/migration/`

#### V1__create-table-customers.sql
Cria o schema completo:
- `addresses` - Endereços
- `people` - Tabela base para pessoas (herança JPA)
- `user_accounts` - Contas de usuário e autenticação
- `employees` - Funcionários
- `customers` - Clientes
- `customer_phones` - Telefones dos clientes

#### V2__insert-admin-user.sql
Insere usuário administrador padrão:
- **Username:** `admin`
- **Password:** `admin123` (BCrypt)
- **Role:** `ROLE_ADMIN`
- **Email:** admin@rentafit.com.br

### Diagrama de Tabelas

```
┌──────────────┐
│   people     │ ← Tabela base (herança)
│──────────────│
│ id (UUID)    │
│ name         │
│ document     │
│ email        │
└──────┬───────┘
       │
       ├─────────────────┬──────────────────┐
       │                 │                  │
┌──────▼─────┐  ┌────────▼───────┐  ┌──────▼──────────┐
│ employees  │  │  customers     │  │ user_accounts   │
│────────────│  │────────────────│  │─────────────────│
│ initials   │  │ is_authenticated│  │ username       │
│ role_level │  │ notes          │  │ password       │
│ pin        │  │ address_id     │  │ role           │
└────────────┘  │ number         │  │ is_active      │
                │ complement     │  └─────────────────┘
                └────────────────┘
                        │
                        │ 1:N
                        ▼
                ┌──────────────────┐
                │ customer_phones  │
                │──────────────────│
                │ phone            │
                └──────────────────┘

┌───────────────────┐       ┌──────────────────────┐
│ rental_contracts  │ 1──N  │ rental_contract_items│
│───────────────────│       │──────────────────────│
│ id (UUID)         │       │ rental_item_id        │
│ status            │       │ description (snapshot)│
│ customer_name *   │       │ value                 │
│ pickup_date       │       │ is_delivered          │
│ event_date        │       └──────────┬────────────┘
│ return_date       │                  │ 1:N
└───────────────────┘                  ▼
         │ 1:N             ┌────────────────────────┐
         ▼                 │ rental_contract_item_meta│
┌─────────────────┐        │────────────────────────│
│ rental_payments │        │ type (ACESSORIO/OBS)   │
│─────────────────│        │ accessory_id (nullable) │
│ installment_num │        └────────────────────────┘
│ payment_method  │
│ value           │
│ status          │
└─────────────────┘
```

---

## 🏗️ Arquitetura do Projeto

### Estrutura de Pacotes

```
br.com.rentafit/
│
├── auth/                        ← Autenticação e Autorização
│   ├── domain/                  → UserAccount, RefreshToken, UserRole
│   ├── repository/              → UserAccountRepository, RefreshTokenRepository
│   ├── service/                 → UserAccountService, RefreshTokenService, TokenService
│   ├── dto/                     → LoginRequestDTO, LoginResponseDTO, UserProfileResponseDTO
│   └── controller/              → AuthController
│
├── people/                      ← Gestão de Pessoas
│   ├── domain/                  → Person, Customer, Employee, Address
│   ├── repository/              → CustomerRepository, EmployeeRepository
│   ├── service/                 → CustomerService, EmployeeService, AddressService
│   ├── dto/                     → CustomerDTO, EmployeeDTO, AddressDTO, ...
│   ├── mapper/                  → PeopleMapper (toDTO, updateFromDTO)
│   └── controller/              → CustomerController, EmployeeController, AddressController
│
├── product/                     ← Gestão de Produtos
│   ├── domain/                  → RentalItem, RetailProduct, Category, Stock, StockMovement
│   ├── repository/              → RentalItemRepository, RetailProductRepository, ...
│   ├── service/                 → RentalItemService, RetailProductService, CategoryService, StockService
│   ├── dto/                     → RentalItemDTO, ProductRetailDTO, CategoryDTO, StockDTO, ...
│   └── controller/              → RentalItemController, RetailProductController, CategoryController, StockController
│
├── rental/                      ← Módulo de Locação
│   ├── adapter/                 → CustomerAdapter, RentalItemAdapter, AccessoryAdapter
│   ├── controller/              → RentalContractController, RentalPaymentController
│   ├── domain/                  → RentalContract, RentalContractItem, RentalContractItemMeta, RentalPayment
│   ├── dto/                     → CreateRentalContractDTO, RentalContractDetailsDTO, RentalPaymentInputDTO, ...
│   ├── mapper/                  → RentalMapper
│   ├── port/                    → CustomerPort, RentalItemPort, AccessoryPort
│   ├── repository/              → RentalContractRepository, RentalPaymentRepository
│   ├── service/                 → RentalContractService, RentalPaymentService, RentalWorkflowService
│   └── validation/              → RentalContractValidator, ItemConflictChecker
│
├── billing/                     ← Faturamento / NFS-e
│   ├── config/                  → CertificateConfig
│   ├── controller/              → BillingController, SaoCarlosNfseController, NfseViaController
│   ├── domain/                  → Invoice
│   ├── dto/                     → InvoiceEmissionRequestDTO, SaoCarlosEmitirNfseRequestDTO, via/...
│   ├── repository/              → InvoiceRepository
│   ├── service/                 → BillingService, InvoiceService, NfsePortalService, SaoCarlosNfseService, NfseViaService
│   └── util/                    → Utilitários de billing
│
├── migration/                   ← Utilitários de Migração
│   └── util/                    → BsonFileReader
│
├── common/                      ← Componentes Compartilhados
│   ├── dto/                     → ErrorResponseDTO
│   ├── exception/               → ResourceNotFoundException
│   └── security/                → SecurityConfig, TokenService, CryptoService, AesCryptoService
│
├── config/                      ← Configurações Globais
│   └── OpenAPIConfig            → Swagger/OpenAPI
│
└── RentafitApplication.java     ← Classe Principal
```

### Padrão de Camadas

```
┌─────────────────────────────────────┐
│      Web Layer (Controllers)        │  ← REST APIs
├─────────────────────────────────────┤
│      Service Layer                  │  ← Lógica de Negócio
├─────────────────────────────────────┤
│      Mapper Layer                   │  ← Conversões Entity↔DTO
├─────────────────────────────────────┤
│      Repository Layer               │  ← Acesso a Dados (JPA)
├─────────────────────────────────────┤
│      Domain Layer (Entities)        │  ← Modelo de Domínio
└─────────────────────────────────────┘
```

---

## 🚀 Executar a Aplicação

### Via IntelliJ IDEA

1. Configure o perfil `local` (veja seção "Configurar IntelliJ IDEA")
2. Execute `RentafitApplication.java`
3. A aplicação iniciará em: http://localhost:8080

### Via Maven

```bash
# Windows PowerShell
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Linux/Mac
mvn spring-boot:run -Dspring.profiles.active=local
```

### Primeira Execução

Na primeira execução:
1. ✅ Flyway criará todas as tabelas
2. ✅ Inserirá o usuário admin automaticamente
3. ✅ Aplicação estará pronta para uso

---

## 📚 API Documentation (Swagger)

Após iniciar a aplicação, acesse:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

### Endpoints Disponíveis

#### **Auth** (`/api/auth`)
- `GET  /api/auth/public-key` - Chave pública RSA para criptografia
- `POST /api/auth/login` - Login (retorna JWT + refresh token)
- `POST /api/auth/refresh` - Renovar access token
- `GET  /api/auth/me` - Perfil do usuário autenticado

#### **Customers** (`/api/v1/customers`)
- `GET    /api/v1/customers` - Listar todos (paginado)
- `GET    /api/v1/customers?name={name}` - Buscar por nome (contains/LIKE, case-insensitive, paginado)
- `GET    /api/v1/customers/byName/{name}` - Buscar por nome (contains/LIKE, case-insensitive, paginado)
- `GET    /api/v1/customers/byNamePrefix/{namePrefix}` - Buscar por prefixo de nome (otimizada para autocomplete)
- `GET    /api/v1/customers/byId/{id}` - Buscar por ID
- `GET    /api/v1/customers/byDocument/{document}` - Buscar por CPF/CNPJ
- `GET    /api/v1/customers/byLegacyId/{legacyId}` - Buscar por ID legado
- `GET    /api/v1/customers/{id}/address-history` - Histórico de endereços
- `POST   /api/v1/customers` - Criar novo cliente
- `PUT    /api/v1/customers` - Atualizar cliente
- `DELETE /api/v1/customers/{id}` - Deletar cliente

**Exemplos de busca por nome:**
- `GET /api/v1/customers?name=joao&page=0&size=10&sort=name,asc`
- `GET /api/v1/customers/byName/joao?page=0&size=10&sort=name,asc`
- `GET /api/v1/customers/byNamePrefix/jo?page=0&size=10&sort=name,asc`

#### **Employees** (`/api/v1/employees`)
- `GET    /api/v1/employees` - Listar todos (paginado)
- `GET    /api/v1/employees/{id}` - Buscar por ID
- `GET    /api/v1/employees/initials/{initials}` - Buscar por iniciais
- `POST   /api/v1/employees` - Criar novo funcionário
- `POST   /api/v1/employees/check` - Validar credenciais (iniciais + PIN, requer Bearer token válido)
- `PUT    /api/v1/employees/{id}` - Atualizar funcionário
- `DELETE /api/v1/employees/{id}` - Deletar funcionário

#### **Addresses** (`/api/v1/addresses`)
- `GET /api/v1/addresses/find/{zipCode}` - Buscar endereço por CEP (ViaCEP)

#### **Categories** (`/api/v1/categories`)
- `GET    /api/v1/categories` - Listar todas
- `GET    /api/v1/categories/{id}` - Buscar por ID
- `GET    /api/v1/categories/type/{type}` - Filtrar por tipo (RENTAL/RETAIL)
- `GET    /api/v1/categories/active` - Listar categorias ativas
- `POST   /api/v1/categories` - Criar categoria
- `PUT    /api/v1/categories/{id}` - Atualizar categoria
- `DELETE /api/v1/categories/{id}` - Deletar categoria

#### **Rental Items** (`/api/v1/products/rental`)
- `GET    /api/v1/products/rental` - Listar todos (paginado)
- `GET    /api/v1/products/rental/{id}` - Buscar por ID
- `GET    /api/v1/products/rental/byLegacy/{id}` - Buscar por código legado
- `POST   /api/v1/products/rental` - Criar item de locação
- `PUT    /api/v1/products/rental/{id}` - Atualizar item de locação
- `DELETE /api/v1/products/rental/{id}` - Deletar item de locação

#### **Retail Products** (`/api/v1/products/retail`)
- `GET    /api/v1/products/retail` - Listar todos (paginado)
- `GET    /api/v1/products/retail/{id}` - Buscar por ID
- `GET    /api/v1/products/retail/bysku/{sku}` - Buscar por SKU
- `POST   /api/v1/products/retail` - Criar produto para venda
- `PUT    /api/v1/products/retail/{id}` - Atualizar produto
- `DELETE /api/v1/products/retail/{id}` - Deletar produto

#### **Stock** (`/api/v1/stock`)
- `GET  /api/v1/stock/{productId}` - Consultar estoque de um produto
- `GET  /api/v1/stock/low` - Produtos com estoque baixo
- `GET  /api/v1/stock/{productId}/movements` - Movimentações de estoque
- `POST /api/v1/stock/add` - Adicionar estoque
- `POST /api/v1/stock/remove` - Remover do estoque
- `POST /api/v1/stock/reserve` - Reservar estoque
- `POST /api/v1/stock/release` - Liberar reserva

#### **Rental Contracts** (`/api/v1/rental/contracts`)
- `GET   /api/v1/rental/contracts` - Listar contratos (paginado)
- `GET   /api/v1/rental/contracts/{id}` - Buscar por ID
- `GET   /api/v1/rental/contracts/byCustomer/{customerId}` - Contratos do cliente
- `POST  /api/v1/rental/contracts` - Criar proposta (DRAFT)
- `PUT   /api/v1/rental/contracts/{id}` - Atualizar (apenas DRAFT)
- `PATCH /api/v1/rental/contracts/{id}/sign` - Assinar (DRAFT → SIGNED)
- `PATCH /api/v1/rental/contracts/{id}/finalize` - Finalizar (SIGNED → FINALIZED)
- `PATCH /api/v1/rental/contracts/{id}/return` - Processar devolução
- `PATCH /api/v1/rental/contracts/{id}/items/{itemId}/deliver` - Confirmar entrega de item
- `POST  /api/v1/rental/contracts/{id}/duplicate` - Duplicar como novo DRAFT

#### **Rental Payments** (`/api/v1/rental/contracts/{contractId}/payments`)
- `GET    /api/v1/rental/contracts/{contractId}/payments` - Listar parcelas
- `POST   /api/v1/rental/contracts/{contractId}/payments` - Adicionar parcela
- `PUT    /api/v1/rental/contracts/{contractId}/payments/{paymentId}` - Atualizar parcela
- `DELETE /api/v1/rental/contracts/{contractId}/payments/{paymentId}` - Cancelar parcela

#### **NFS-e Portal Nacional** (`/api/billing/invoices`)
- `POST /api/billing/invoices/emit` - Emitir NFS-e
- `GET  /api/billing/invoices/{id}` - Consultar por ID interno
- `GET  /api/billing/invoices/chave/{chaveAcesso}` - Consultar no Portal Nacional
- `GET  /api/billing/invoices/chave/{chaveAcesso}/pdf` - Download PDF (DANFSe)
- `GET  /api/billing/invoices/chave/{chaveAcesso}/xml` - Download XML legal
- `GET  /api/billing/invoices/numero/{numeroNota}` - Consultar por número

#### **NFS-e São Carlos / GINFES** (`/api/billing/saocarlos`)
- `POST /api/billing/saocarlos/emit` - Emitir NFS-e em São Carlos
- `GET  /api/billing/saocarlos/situacao/{protocolo}` - Situação do lote
- `GET  /api/billing/saocarlos/lote/{protocolo}` - Consultar lote processado

#### **NFS-e Via / Serpro** (`/api/billing/via`)
- `GET  /api/billing/via/testeNfse` - Teste de integração
- `POST /api/billing/via/nfsev` - Receber e validar NFS-e Via
- `POST /api/billing/via/cancelamento` - Cancelar NFS-e Via
- `POST /api/billing/via/substituicao` - Substituir NFS-e Via
- `GET  /api/billing/via/aliquota-efetiva/cnpj/{cnpj}/trecho/{codigoTrecho}/data/{dataReferencia}` - Alíquota efetiva
- `GET  /api/billing/via/consulta/protocolo` - Consultar por protocolo
- `GET  /api/billing/via/consulta/chaveacesso` - Consultar por chave de acesso

---

## 🔐 Autenticação

### Usuário Padrão (Admin)

```
Username: admin
Password: admin123
Role: ROLE_ADMIN
```

### Estrutura de Roles

- `ROLE_ADMIN` - Acesso total ao sistema
- `ROLE_EMPLOYEE` - Acesso de funcionário
- `ROLE_CUSTOMER` - Acesso de cliente

---

## 🛠️ Troubleshooting

### ❌ Erro: "password authentication failed for user postgres"

**Solução:**
1. Verifique o `application-local.properties`
2. Confirme senha no `docker-compose.yaml`
3. Reinicie o container: `docker restart dev-postgresql`

### ❌ Erro: "Connection refused"

**Solução:**
1. Verifique se o container está rodando: `docker ps`
2. Use a porta **5433** (não 5432)
3. Confirme: `localhost:5433` no `application-local.properties`

### ❌ Tabelas sem dados após iniciar

**Solução:**
Isso ocorria quando `ddl-auto=create` recriava as tabelas após o Flyway.

✅ **Corrigido:** Agora usa `ddl-auto=validate`

Para recriar o banco:
```bash
docker exec dev-postgresql psql -U postgres -d rentafit -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```
Depois reinicie a aplicação.

### ❌ Erro de compilação após refatoração

**Solução:**
```bash
mvn clean compile
```

---

## 🧪 Testes

### Testes Unitários e de Integração

```bash
# Executar todos os testes
mvn test

# Executar com cobertura
mvn verify
```

### Testes da API com Postman

O projeto inclui uma coleção unificada do Postman com todos os endpoints da API.

**Localização:** `src/test/postman/`

#### 📦 Coleção Unificada (RECOMENDADO)

**Arquivo:** `Rentafit_Unified_API.postman_collection.json`

Coleção completa e unificada com todos os módulos:
- ✅ **Auth** - Login, Refresh Token, Profile, Chave Pública RSA
- ✅ **People - Customers** - CRUD completo + busca por documento/legacyId/histórico de endereços
- ✅ **People - Employees** - CRUD + busca por iniciais + validação de credenciais (PIN)
- ✅ **People - Addresses** - Busca de CEP via ViaCEP
- ✅ **Product - Categories** - Gerenciamento de categorias
- ✅ **Product - Rental Items** - Produtos de aluguel (com suporte a legacyId)
- ✅ **Product - Retail Items** - Produtos para venda (com busca por SKU)
- ✅ **Product - Stock** - Gestão de estoque e movimentações
- ✅ **Rental Contracts** - Ciclo de vida completo (DRAFT → SIGNED → FINALIZED → devolvido)
- ✅ **Rental Payments** - Gerenciamento de parcelas por contrato
- ✅ **Billing - NFS-e Portal Nacional** - Emissão e consulta de NFS-e
- ✅ **Billing - NFS-e São Carlos (GINFES)** - Emissão e consulta para São Carlos
- ✅ **Billing - NFS-e Via (Serpro)** - Integração com API Serpro

**Características:**
- 🔐 Autenticação automática via Bearer Token
- 📝 Scripts automáticos para salvar IDs e tokens
- 🌍 Suporte para múltiplos environments (Local/Production)
- 📚 Documentação completa de cada endpoint

#### 🌍 Environments

- **Local:** `Rentafit_Local.postman_environment.json`
  - Base URL: `http://localhost:8080`
  
- **Production:** `Rentafit_Production.postman_environment.json`
  - Base URL: `https://api.rentafit.com.br`

#### 📖 Documentação

Consulte o guia completo de uso da coleção Postman:
- **Arquivo:** `src/test/postman/README_POSTMAN.md`
- Instruções de importação e configuração
- Fluxo de testes recomendado
- Exemplos de requests
- Troubleshooting

#### 🚀 Quick Start

1. Importe a coleção: `Rentafit_Unified_API.postman_collection.json`
2. Importe o environment: `Rentafit_Local.postman_environment.json`
3. Execute: `Auth → Login` (credenciais: admin/admin123)
4. Crie clientes, funcionários e produtos de aluguel
5. Crie um contrato DRAFT → Sign → Finalize
6. Teste os endpoints de pagamento, devolução e NFS-e

---

## 📦 Build e Deploy

### Gerar JAR

```bash
mvn clean package -DskipTests
```

O JAR será gerado em: `target/Rentafit-0.0.1-SNAPSHOT.jar`

### Executar JAR

```bash
java -Dspring.profiles.active=local -jar target/Rentafit-0.0.1-SNAPSHOT.jar
```

---

## ✅ Status do Projeto

### Implementado

- ✅ Estrutura modular (auth, people, product, rental, billing, common, config)
- ✅ Entidades JPA com herança (Person → Customer/Employee)
- ✅ Migrations Flyway
- ✅ Repositories (JPA)
- ✅ Services com lógica de negócio
- ✅ DTOs e Mappers (Entity↔DTO)
- ✅ Controllers REST completos
- ✅ Documentação Swagger/OpenAPI
- ✅ Configuração Spring Security
- ✅ JWT Authentication (login, access token, refresh token)
- ✅ AuthController (login, refresh, me, public-key)
- ✅ Criptografia RSA (CryptoService) e AES (AesCryptoService)
- ✅ Perfis de ambiente (local, hk, production)
- ✅ Docker Compose para PostgreSQL
- ✅ Módulo de Locação (RentalContract, ciclo de vida completo)
- ✅ Módulo de Faturamento NFS-e (Portal Nacional, São Carlos/GINFES, Via/Serpro)
- ✅ Módulo de Estoque (Stock, StockMovement)
- ✅ Testes unitários e de integração (JUnit + Mockito)
- ✅ Validação de credenciais de funcionário por iniciais + PIN

### Em Desenvolvimento

- ⏳ Relatórios e dashboards gerenciais
- ⏳ Integração com meios de pagamento (PagSeguro/Stripe)
- ⏳ Notificações (e-mail / WhatsApp) para clientes
- ⏳ CI/CD pipeline completo com deploy automatizado

---

## 📝 Convenções do Projeto

### Nomenclatura

- **Entities:** Singular, PascalCase (`Customer`, `Employee`)
- **DTOs:** Sufixo `DTO` (`CustomerDTO`, `EmployeeDTO`)
- **Repositories:** Sufixo `Repository` (`CustomerRepository`)
- **Services:** Sufixo `Service` (`CustomerService`)
- **Controllers:** Sufixo `Controller` (`CustomerController`)
- **Mappers:** Sufixo `Mapper` (`PeopleMapper`)

### Packages

- `domain/` - Entidades e enums
- `repository/` - Interfaces JPA
- `service/` - Lógica de negócio
- `dto/` - Data Transfer Objects
- `mapper/` - Conversões Entity↔DTO
- `controller/` - REST Controllers

---

## 👥 Contribuindo

1. Siga os padrões de nomenclatura
2. Mantenha a separação de camadas
3. Documente APIs com Swagger annotations
4. Escreva testes para novas funcionalidades
5. Use Lombok para reduzir boilerplate

---

## 🔄 Git Flow & Branching Strategy

Este projeto segue o padrão **Git Flow** para gerenciamento de versões e controle de código.

### Estrutura de Branches

```
master (produção - releases estáveis)
  ↑
  └─ release/* (preparação de release v1.0, v1.1, etc.)
       ↓
develop (desenvolvimento - integração contínua)
  ↑
  ├─ feature/* (novas funcionalidades)
  ├─ bugfix/* (correções em desenvolvimento)
  └─ hotfix/* (correções críticas em produção)
```

### Tipos de Branches

#### **master**
- ✅ Apenas releases estáveis e testadas
- ✅ Protegida: requer pull request e revisão
- ✅ Requer verificações de CI/CD passando
- ✅ Cada commit recebe tag de versão (v1.0.0, v1.0.1, etc.)

#### **develop**
- ✅ Branch principal de desenvolvimento
- ✅ Protegida: requer pull request e aprovação
- ✅ Requer testes passando
- ✅ Recebe merges de feature/bugfix branches

#### **feature/\***
- 🔀 Criada a partir de: `develop`
- 🔀 Merge de volta para: `develop`
- 🔀 Convenção: `feature/nome-da-feature`
- 🔀 Exemplos:
  ```bash
  feature/add-jwt-authentication
  feature/refactor-mapper-layer
  feature/add-customer-validation
  ```

#### **bugfix/\***
- 🔀 Criada a partir de: `develop`
- 🔀 Merge de volta para: `develop`
- 🔀 Convenção: `bugfix/nome-do-bug`
- 🔀 Exemplos:
  ```bash
  bugfix/fix-login-error
  bugfix/fix-null-pointer-exception
  bugfix/fix-database-connection
  ```

#### **hotfix/\***
- 🚨 Criada a partir de: `master`
- 🚨 Merge de volta para: `master` E `develop`
- 🚨 Convenção: `hotfix/nome-critico`
- 🚨 Exemplos:
  ```bash
  hotfix/security-patch
  hotfix/fix-critical-bug
  hotfix/fix-production-error
  ```

#### **release/\***
- 📦 Criada a partir de: `develop`
- 📦 Merge de volta para: `master` (com tag) E `develop`
- 📦 Convenção: `release/x.y.z`
- 📦 Exemplos:
  ```bash
  release/1.0.0
  release/1.1.0
  release/2.0.0
  ```

### Workflow Prático

#### 1️⃣ **Criar uma nova feature**

```bash
# Atualizar develop
git checkout develop
git pull origin develop

# Criar branch de feature
git checkout -b feature/add-jwt-authentication

# Fazer commits
git add .
git commit -m "feat: implement JWT authentication"

# Push para remote
git push -u origin feature/add-jwt-authentication
```

#### 2️⃣ **Enviar Pull Request**

- Abra PR no GitHub/GitLab
- Title: `feat: add JWT authentication`
- Description: descreva a mudança
- Aguarde aprovação e verificações de CI/CD

#### 3️⃣ **Após aprovação: Merge para develop**

```bash
# No GitHub/GitLab, clique em "Merge pull request"
# Ou via CLI:
git checkout develop
git pull origin develop
git merge feature/add-jwt-authentication
git push origin develop
```

#### 4️⃣ **Deletar branch de feature**

```bash
git branch -d feature/add-jwt-authentication
git push origin --delete feature/add-jwt-authentication
```

#### 5️⃣ **Preparar release**

```bash
# Criar branch de release a partir de develop
git checkout -b release/1.0.0 develop
git push -u origin release/1.0.0

# Fazer ajustes finais, testes, bumpar versão
# Commit final:
git commit -m "chore: bump version to 1.0.0"
git push origin release/1.0.0

# Enviar PR para master
```

#### 6️⃣ **Fazer merge em master e tagear**

```bash
# Via GitHub: merge release/1.0.0 para master
# Depois, localmente:
git checkout master
git pull origin master

# Criar tag de versão
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# Fazer merge também em develop
git checkout develop
git merge master
git push origin develop
```

#### 7️⃣ **Hotfix (correção crítica)**

```bash
# Criar hotfix a partir de master
git checkout -b hotfix/security-patch master
git push -u origin hotfix/security-patch

# Fazer fix e commit
git commit -m "fix: security vulnerability in auth"

# Merge em master com tag
git checkout master
git merge hotfix/security-patch
git tag -a v1.0.1 -m "Hotfix version 1.0.1"
git push origin master v1.0.1

# Merge também em develop
git checkout develop
git merge hotfix/security-patch
git push origin develop

# Deletar hotfix
git branch -d hotfix/security-patch
git push origin --delete hotfix/security-patch
```

### Convenção de Commits

Seguimos **Conventional Commits**:

```bash
# Feature
git commit -m "feat: add JWT authentication"

# Bug fix
git commit -m "fix: prevent null pointer exception"

# Documentation
git commit -m "docs: update README with git flow"

# Refactor
git commit -m "refactor: reorganize auth module"

# Test
git commit -m "test: add unit tests for mapper"

# Chore
git commit -m "chore: update dependencies"

# Breaking change
git commit -m "feat!: rename Customer API endpoints"
```

### Proteção de Branches

Configure no GitHub: **Settings → Branches → Branch protection rules**

#### Para `master`:
- ✅ Exigir pull requests antes de merge
- ✅ Exigir mínimo 2 aprovações
- ✅ Descartar aprovações obsoletas
- ✅ Exigir verificações de status (CI/CD)
- ✅ Exigir branches atualizadas antes de merge
- ✅ Restringir push direto (apenas admins)
- ✅ Exigir commits assinados

#### Para `develop`:
- ✅ Exigir pull requests antes de merge
- ✅ Exigir mínimo 1 aprovação
- ✅ Exigir verificações de status (CI/CD)
- ✅ Permitir push direto apenas para admins

---

## 🔄 CI/CD Pipeline

### GitHub Actions

O projeto utiliza **GitHub Actions** para automatizar testes e deployment.

#### Arquivo: `.github/workflows/ci-cd.yml`

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ master, develop ]
  pull_request:
    branches: [ master, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    strategy:
      matrix:
        java-version: ['21']
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Setup Java ${{ matrix.java-version }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ matrix.java-version }}
        distribution: 'temurin'
        cache: maven
    
    - name: Build with Maven
      run: mvn clean compile -DskipTests
    
    - name: Run Tests
      run: mvn test
    
    - name: Generate Test Report
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: test-reports
        path: target/surefire-reports/
    
    - name: SonarQube Analysis (optional)
      if: github.ref == 'refs/heads/develop'
      run: mvn sonar:sonar -Dsonar.projectKey=rentafit
      env:
        SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
        SONAR_LOGIN: ${{ secrets.SONAR_LOGIN }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/master' && github.event_name == 'push'
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Setup Java
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven
    
    - name: Build JAR
      run: mvn clean package -DskipTests
    
    - name: Deploy to Production
      run: |
        echo "Deploying to production..."
        # Seu comando de deployment aqui
```

### Executar CI/CD Localmente

```bash
# Simular build local
mvn clean package

# Simular testes
mvn test

# Simular análise de código
mvn sonar:sonar
```

---

## 📄 Licença

Este projeto é privado e proprietário.

---

## 📞 Contato

Para dúvidas ou suporte, entre em contato com a equipe de desenvolvimento.

---

**Rentafit Backend** - Sistema de Gestão de Locação de Trajes e Equipamentos

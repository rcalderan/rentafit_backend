# Rentafit - Sistema de Gestão de Locação de Equipamentos

API RESTful para gerenciamento de clientes, funcionários e sistema de autenticação para locadora de equipamentos fitness.

## 🚀 Tecnologias

- **Java 21 LTS**
- **Spring Boot 3.4.1**
- **Spring Security**
- **Spring Data JPA**
- **PostgreSQL 14**
- **Flyway** (migrations)
- **Lombok**
- **Swagger/OpenAPI 3**
- **Docker & Docker Compose**

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
└────────────┘  │ address_id     │  │ role           │
                │ number         │  │ is_active      │
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
```

---

## 🏗️ Arquitetura do Projeto

### Estrutura de Pacotes

```
br.com.rentafit/
│
├── auth/                        ← Autenticação e Autorização
│   ├── domain/                  → UserAccount, UserRole
│   ├── repository/              → UserAccountRepository
│   ├── service/                 → UserAccountService (UserDetailsService)
│   ├── dto/                     → (preparado para LoginDTO, etc.)
│   ├── mapper/                  → (preparado para conversões)
│   └── controller/              → (preparado para AuthController)
│
├── people/                      ← Gestão de Pessoas
│   ├── domain/                  → Person, Customer, Employee, Address
│   ├── repository/              → CustomerRepository, EmployeeRepository
│   ├── service/                 → CustomerService, EmployeeService
│   ├── dto/                     → CustomerDTO, EmployeeDTO, AddressDTO
│   ├── mapper/                  → PeopleMapper (toDTO, updateFromDTO)
│   └── controller/              → CustomerController, EmployeeController
│
├── common/                      ← Componentes Compartilhados
│   └── security/                → SecurityConfig
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

#### **Customers** (`/api/customers`)
- `GET /api/customers` - Listar todos (paginado)
- `GET /api/customers/{id}` - Buscar por ID
- `POST /api/customers` - Criar novo cliente
- `PUT /api/customers/{id}` - Atualizar cliente
- `DELETE /api/customers/{id}` - Deletar cliente

#### **Employees** (`/api/employees`)
- `GET /api/employees` - Listar todos (paginado)
- `GET /api/employees/{id}` - Buscar por ID
- `POST /api/employees` - Criar novo funcionário
- `PUT /api/employees/{id}` - Atualizar funcionário
- `DELETE /api/employees/{id}` - Deletar funcionário

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
- ✅ **Auth** - Login, Refresh Token, Profile
- ✅ **People - Customers** - CRUD completo de clientes
- ✅ **People - Employees** - CRUD completo de funcionários
- ✅ **Product - Categories** - Gerenciamento de categorias
- ✅ **Product - Rental Items** - Produtos de aluguel
- ✅ **Product - Retail Items** - Produtos para venda
- ✅ **Product - Stock** - Gestão de estoque e movimentações

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
4. Teste os demais endpoints (token salvo automaticamente)

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

- ✅ Estrutura modular (auth, people, common, config)
- ✅ Entidades JPA com herança (Person → Customer/Employee)
- ✅ Migrations Flyway (V1, V2)
- ✅ Repositories (JPA)
- ✅ Services com lógica de negócio
- ✅ DTOs e Mappers (Entity↔DTO)
- ✅ Controllers REST completos
- ✅ Documentação Swagger/OpenAPI
- ✅ Configuração Spring Security
- ✅ UserDetailsService para autenticação
- ✅ Perfis de ambiente (local, production)
- ✅ Docker Compose para PostgreSQL

### Em Desenvolvimento

- ⏳ JWT Authentication (login, tokens)
- ⏳ AuthController (endpoints de login/logout)
- ⏳ Validações customizadas
- ⏳ Tratamento global de exceções
- ⏳ Testes unitários e de integração
- ⏳ CI/CD pipeline

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

**Rentafit Backend** - Sistema de Gestão de Locação v0.0.1

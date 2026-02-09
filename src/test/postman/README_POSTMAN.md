# Rentafit - Postman API Collection

Coleção unificada completa da API Rentafit para testes e documentação de todos os módulos do sistema.

## 📋 Conteúdo

Esta coleção contém todos os endpoints organizados por módulos:

- **Auth** - Autenticação e gerenciamento de tokens JWT
- **People - Customers** - Gerenciamento de clientes
- **People - Employees** - Gerenciamento de funcionários
- **Product - Categories** - Gerenciamento de categorias de produtos
- **Product - Rental Items** - Gerenciamento de produtos de aluguel
- **Product - Retail Items** - Gerenciamento de produtos para venda
- **Product - Stock** - Gerenciamento de estoque e movimentações

## 🚀 Como Usar

### 1. Importar a Coleção

1. Abra o Postman
2. Clique em **Import**
3. Selecione o arquivo: `Rentafit_Unified_API.postman_collection.json`
4. A coleção será importada com todos os endpoints organizados

### 2. Importar o Environment

Importe o ambiente correspondente ao seu contexto de teste:

#### Local (Desenvolvimento)
- Arquivo: `Rentafit_Local.postman_environment.json`
- Base URL: `http://localhost:8080`

#### Production
- Arquivo: `Rentafit_Production.postman_environment.json`
- Base URL: `https://api.rentafit.com.br`

**Como importar:**
1. No Postman, clique no ícone de **Environments** (canto superior direito)
2. Clique em **Import**
3. Selecione o arquivo de environment desejado
4. Ative o environment clicando no dropdown e selecionando-o

### 3. Realizar o Login

**IMPORTANTE:** Antes de testar qualquer endpoint (exceto os públicos), você deve fazer login:

1. Navegue até: `Auth → Login`
2. Execute a request (Send)
3. O script automático salvará os tokens nas variáveis:
   - `access_token` - Usado automaticamente em todas as requests autenticadas
   - `refresh_token` - Para renovar o access token quando expirar
   - `user_id` - ID do usuário logado (obtido no endpoint `/auth/me`)

**Credenciais padrão:**
```json
{
    "username": "admin",
    "password": "admin123"
}
```

### 4. Testar os Endpoints

Após o login, todos os endpoints estarão autenticados automaticamente usando o Bearer Token armazenado em `{{access_token}}`.

## 🔐 Autenticação Automática

A coleção está configurada com **Bearer Token Authentication** em nível de coleção, o que significa:

- ✅ Todos os endpoints (exceto Login e Public Key) usam automaticamente o `{{access_token}}`
- ✅ Você não precisa configurar autenticação manualmente em cada request
- ✅ Os tokens são salvos automaticamente após o login via scripts

### Scripts de Teste Automáticos

A coleção inclui scripts que automatizam o salvamento de IDs importantes:

#### Login
```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.collectionVariables.set("access_token", jsonData.accessToken);
    pm.collectionVariables.set("refresh_token", jsonData.refreshToken);
}
```

#### Create Customer
```javascript
if (pm.response.code === 201) {
    var jsonData = pm.response.json();
    pm.collectionVariables.set("customer_id", jsonData.id);
}
```

Este padrão se repete para:
- `employee_id` (Create Employee)
- `category_id` (Create Category)
- `rental_product_id` (Create Rental Product)
- `retail_product_id` (Create Retail Product)
- `user_id` (Get Current User Profile)

## 📁 Estrutura de Variáveis

### Collection Variables

Estas variáveis são definidas em nível de coleção e podem ser substituídas pelo environment:

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `base_url` | URL base da API | `http://localhost:8080` |
| `access_token` | JWT access token | (salvo automaticamente no login) |
| `refresh_token` | Refresh token | (salvo automaticamente no login) |
| `user_id` | UUID do usuário logado | `550e8400-e29b-41d4-a716-446655440000` |
| `customer_id` | UUID do cliente para testes | (salvo ao criar customer) |
| `employee_id` | UUID do funcionário para testes | (salvo ao criar employee) |
| `category_id` | UUID da categoria para testes | (salvo ao criar categoria) |
| `rental_product_id` | UUID do produto de aluguel | (salvo ao criar rental product) |
| `retail_product_id` | UUID do produto de venda | (salvo ao criar retail product) |

## 🔄 Fluxo de Teste Recomendado

### 1. Setup Inicial

```
1. Auth → Login
2. Auth → Get Current User Profile (salva user_id)
```

### 2. Testar Módulo People

```
# Customers
1. Create Customer → salva customer_id
2. Get Customer by ID
3. List All Customers
4. Update Customer
5. Get Customer Address History
6. Delete Customer (opcional)

# Employees
1. Create Employee → salva employee_id
2. Get Employee by ID
3. List All Employees
4. Update Employee
5. Delete Employee (opcional)
```

### 3. Testar Módulo Product

```
# Categories
1. Create Category → salva category_id
2. Get All Categories
3. Get Active Categories
4. Update Category

# Rental Products
1. Create Rental Product → salva rental_product_id
2. Get All Rental Products
3. Get Rental Product by ID
4. Update Rental Product

# Retail Products
1. Create Retail Product → salva retail_product_id
2. Get All Retail Products
3. Get Retail Product by ID
4. Update Retail Product

# Stock
1. Add Stock (usar rental_product_id e user_id)
2. Get Stock by Product ID
3. Get Stock Movements
4. Reserve Stock
5. Release Reservation
6. Remove Stock
```

### 4. Renovar Token (se expirar)

```
Auth → Refresh Token
```

## 📝 Exemplos de Requests

### Criar um Cliente

**Endpoint:** `POST /api/v1/customers`

```json
{
    "name": "João da Silva",
    "document": "12345678900",
    "email": "joao.silva@email.com",
    "isAuthenticated": false,
    "notes": "Cliente VIP",
    "legacyId": 1001,
    "address": {
        "street": "Rua das Flores",
        "neighborhood": "Centro",
        "city": "São Paulo",
        "state": "SP",
        "zipCode": "01234567"
    },
    "number": "100",
    "complement": "Apto 12",
    "phones": [
        "11987654321",
        "1133334444"
    ]
}
```

### Criar um Produto de Aluguel

**Endpoint:** `POST /api/v1/products/rental`

```json
{
    "name": "Esteira Profissional",
    "categoryId": "{{category_id}}",
    "size": "Grande",
    "color": "Preto",
    "brand": "Movement",
    "value": 150.00,
    "description": "Esteira profissional para treinos intensos",
    "legacyId": "EST-001",
    "status": "AVAILABLE",
    "notes": "Produto em excelente estado",
    "minRentalDays": 7,
    "condition": "EXCELLENT"
}
```

### Adicionar Estoque

**Endpoint:** `POST /api/v1/stock/add`

**Query Parameters:**
- `productId`: `{{rental_product_id}}`
- `quantity`: `10`
- `userId`: `{{user_id}}`
- `notes`: `Entrada de estoque inicial` (opcional)

## 🛠️ Troubleshooting

### Erro 401 (Unauthorized)

**Causa:** Token expirado ou inválido

**Solução:**
1. Execute novamente: `Auth → Login`
2. Ou use: `Auth → Refresh Token`

### Erro 404 (Not Found)

**Causa:** ID da variável não foi salvo corretamente

**Solução:**
1. Verifique se você executou a request de criação do recurso
2. Verifique se o script de teste foi executado com sucesso
3. Confira as variáveis da coleção (clique com botão direito na coleção → Edit → Variables)

### Variáveis não estão sendo salvas

**Solução:**
1. Verifique se os scripts de teste estão habilitados no Postman
2. Olhe o console do Postman (View → Show Postman Console) para ver mensagens de log
3. Confirme que o response code foi o esperado (200, 201, etc.)

### Base URL incorreta

**Solução:**
1. Verifique qual environment está ativo
2. Ou edite a variável `base_url` na coleção diretamente

## 📊 Paginação

Endpoints de listagem suportam paginação via query parameters:

- `page` - Número da página (começando em 0)
- `size` - Quantidade de itens por página
- `sort` - Campo e direção de ordenação (ex: `name,asc` ou `createdAt,desc`)

**Exemplo:**
```
GET /api/v1/customers?page=0&size=10&sort=name,asc
```

## 🔍 Filtros Específicos

### Customers
- `GET /api/v1/customers/byDocument/{document}` - Busca por CPF/CNPJ
- `GET /api/v1/customers/byLegacyId/{legacyId}` - Busca por ID legado

### Categories
- `GET /api/v1/categories/type/{type}` - Filtra por tipo (RENTAL, RETAIL, ACCESSORY)
- `GET /api/v1/categories/active` - Lista apenas categorias ativas

### Stock
- `GET /api/v1/stock/low` - Lista produtos com estoque baixo

## 📄 Documentação da API

Para mais detalhes sobre os endpoints, acesse a documentação Swagger:

- **Local:** http://localhost:8080/swagger-ui.html
- **Production:** https://api.rentafit.com.br/swagger-ui.html

## 🆘 Suporte

Para dúvidas ou problemas:

1. Consulte o README principal do projeto: `README.md`
2. Verifique a documentação Swagger
3. Entre em contato com a equipe de desenvolvimento

---

**Rentafit API Collection** - v1.0.0

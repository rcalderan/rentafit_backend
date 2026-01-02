# CONTRIBUTING.md - Guia de Contribuição

## 🎯 Bem-vindo!

Obrigado por estar interessado em contribuir para o projeto **Rentafit**! Este documento fornece diretrizes e instruções para contribuições.

---

## 📝 Processo de Contribuição

### 1. **Antes de Começar**

- Verifique [issues abertas](../../issues) para não duplicar esforços
- Leia o [README.md](README.md) para entender o projeto
- Familiarize-se com a [arquitetura do projeto](README.md#🏗️-arquitetura-do-projeto)

### 2. **Criar uma Branch**

Siga o padrão [Git Flow](README.md#🔄-git-flow--branching-strategy):

```bash
# Feature nova
git checkout -b feature/nome-descritivo

# Correção de bug
git checkout -b bugfix/nome-descritivo

# Exemplos:
git checkout -b feature/add-jwt-authentication
git checkout -b bugfix/fix-null-pointer-exception
```

### 3. **Desenvolvendo**

#### Código

```java
// ✅ BOM: siga as convenções
@Service
public class CustomerService {
    private final CustomerRepository repository;
    private final PeopleMapper mapper;
    
    public CustomerDTO createCustomer(CustomerDTO dto) {
        Customer customer = new Customer();
        mapper.updateFromDTO(customer, dto);
        return mapper.toDTO(repository.save(customer));
    }
}

// ❌ RUIM: não siga padrões
public class customerService {
    public void create(CustomerDTO customerDTO) {
        // lógica sem separação de camadas
    }
}
```

#### Convenções

**Nomenclatura:**
- Classes: `PascalCase` (ex: `CustomerService`)
- Métodos/variáveis: `camelCase` (ex: `createCustomer`)
- Constantes: `SCREAMING_SNAKE_CASE` (ex: `MAX_RETRY_ATTEMPTS`)
- Packages: `lowercase.with.dots` (ex: `br.com.rentafit.people.service`)

**Estrutura de Pacotes:**
```
module/
├── domain/         → Entidades JPA e Enums
├── repository/     → Interfaces de acesso a dados
├── service/        → Lógica de negócio
├── dto/            → Data Transfer Objects
├── mapper/         → Conversões Entity↔DTO
└── web/            → REST Controllers
```

#### Testes

Escreva testes para suas mudanças:

```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class CustomerServiceTest {
    
    @InjectMocks
    private CustomerService service;
    
    @Mock
    private CustomerRepository repository;
    
    @Test
    public void testCreateCustomer() {
        // Arrange
        CustomerDTO dto = new CustomerDTO(...);
        
        // Act
        CustomerDTO result = service.create(dto);
        
        // Assert
        assertNotNull(result.getId());
    }
}
```

Execução de testes:
```bash
mvn test                    # Testes unitários
mvn verify                  # Testes de integração
mvn jacoco:report          # Cobertura de código
```

#### Documentação de API

Use Swagger annotations:

```java
@RestController
@RequestMapping("/api/customers")
@Slf4j
public class CustomerController {
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Buscar cliente por ID",
        description = "Retorna um cliente específico pelo seu UUID"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Cliente encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerDTO.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Cliente não encontrado"
    )
    public ResponseEntity<CustomerDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
```

### 4. **Commits**

Siga [Conventional Commits](https://www.conventionalcommits.org/):

```bash
# Feature
git commit -m "feat: add JWT authentication"

# Bug fix
git commit -m "fix: prevent null pointer in mapper"

# Documentation
git commit -m "docs: update README with git flow"

# Refactor
git commit -m "refactor: reorganize auth module"

# Test
git commit -m "test: add unit tests for CustomerService"

# Chore
git commit -m "chore: update Spring Boot to 3.4.2"

# Breaking change
git commit -m "feat!: rename /api/customers to /api/people/customers"
```

### 5. **Push e Pull Request**

```bash
# Push para remote
git push origin feature/nome-da-feature

# Abrir PR no GitHub
# Title: "feat: add JWT authentication"
# Description: descrever a mudança, motivação, etc.
```

**Template de PR:**
```markdown
## Descrição
Descreva o que esta mudança faz.

## Tipo de Mudança
- [ ] Bug fix
- [ ] Feature nova
- [ ] Breaking change
- [ ] Documentação

## Como foi testado
Descreva como testou a mudança.

## Checklist
- [ ] Código segue os padrões do projeto
- [ ] Testes foram adicionados
- [ ] Documentação foi atualizada
- [ ] Sem warnings no build
```

### 6. **Revisão de Código**

- Aguarde aprovação de pelo menos 1 revisor
- Responda aos comentários dos revisores
- Faça ajustes conforme solicitado
- Após aprovação, o mantenedor fará o merge

---

## 🏗️ Estrutura de Código

### Exemplo: Novo Recurso (Feature)

```bash
# 1. Criar branch
git checkout -b feature/add-payment-service

# 2. Criar estrutura
src/main/java/br/com/rentafit/payment/
├── domain/
│   ├── Payment.java
│   └── PaymentStatus.java
├── repository/
│   └── PaymentRepository.java
├── service/
│   └── PaymentService.java
├── dto/
│   ├── PaymentDTO.java
│   └── PaymentResponseDTO.java
├── mapper/
│   └── PaymentMapper.java
└── web/
    └── PaymentController.java

# 3. Adicionar migration
src/main/resources/db/migration/
└── V3__create-table-payments.sql

# 4. Implementar testes
src/test/java/br/com/rentafit/payment/
├── service/
│   └── PaymentServiceTest.java
└── web/
    └── PaymentControllerTest.java

# 5. Fazer commits incrementais
git commit -m "feat: add Payment entity and repository"
git commit -m "feat: implement PaymentService with business logic"
git commit -m "feat: create PaymentController REST endpoints"
git commit -m "test: add unit tests for PaymentService"
git commit -m "docs: update API documentation for payments"

# 6. Push e PR
git push origin feature/add-payment-service
```

---

## 🧪 Checklist Antes de Submeter PR

- [ ] **Código**
  - [ ] Segue convenções de nomenclatura
  - [ ] Mantém separação de camadas
  - [ ] Sem código duplicado
  - [ ] Sem warnings

- [ ] **Testes**
  - [ ] Testes unitários adicionados
  - [ ] Todos os testes passam (`mvn test`)
  - [ ] Cobertura adequada (80%+)

- [ ] **Documentação**
  - [ ] Swagger annotations adicionadas
  - [ ] README atualizado (se necessário)
  - [ ] Javadocs para métodos públicos

- [ ] **Qualidade**
  - [ ] Sem erros de segurança
  - [ ] Build passa (`mvn clean package`)
  - [ ] SonarQube passou (se integrado)

- [ ] **Git**
  - [ ] Commits com mensagens descritivas
  - [ ] Baseado na branch correta (`develop` para feature)
  - [ ] Sem merge conflicts

---

## 📚 Padrões de Design

### Mapper Pattern
```java
// ✅ BOM: usar mapper para conversões
@Service
public class CustomerService {
    private final PeopleMapper mapper;
    
    public CustomerDTO create(CustomerDTO dto) {
        Customer customer = new Customer();
        mapper.updateFromDTO(customer, dto);
        return mapper.toDTO(repository.save(customer));
    }
}
```

### Service Pattern
```java
// ✅ BOM: lógica de negócio no service
@Service
public class CustomerService {
    public CustomerDTO create(CustomerDTO dto) {
        // Validação
        // Transformação de dados
        // Persistência
        // Transformação para DTO
    }
}
```

### Repository Pattern
```java
// ✅ BOM: acesso a dados via repository
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByDocument(String document);
}
```

---

## 🔐 Segurança

### Senhas e Secrets
**NUNCA** commite:
- Senhas
- API keys
- Tokens
- Arquivos `.env`
- Credenciais de banco de dados

Use variáveis de ambiente ou GitHub Secrets.

### Dependências
```bash
# Verifique vulnerabilidades
mvn dependency-check:check

# Mantenha dependências atualizadas
mvn versions:display-dependency-updates
```

---

## 🚀 Boas Práticas

1. **Commits Pequenos**: Um commit = uma mudança lógica
2. **Mensagens Claras**: Descreva o "O quê" e o "Por quê"
3. **Testes Primeiro**: Write tests before code (TDD)
4. **Code Review**: Sempre faça revisão antes de merge
5. **Documentação**: Código sem documentação é código quebrado
6. **Performance**: Considere impacto em performance
7. **Segurança**: Valide inputs, use senhas com hash, etc.

---

## 📞 Contato

Dúvidas? Entre em contato com:
- [Issues](../../issues)
- [Discussions](../../discussions)
- Email: desenvolvimento@rentafit.com.br

---

## 📄 Licença

Ao contribuir, você concorda que suas contribuições serão licenciadas sob a mesma licença do projeto (Privada).

---

**Obrigado por contribuir para o Rentafit! 🎉**


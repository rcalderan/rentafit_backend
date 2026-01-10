# Migration Module - MongoDB → PostgreSQL

## 🎯 Objetivo

Migrar dados do backup MongoDB legado (noivabd) para PostgreSQL do projeto Rentafit usando Spring Batch, com foco inicial no módulo **People** (clientes e funcionários).

## 📋 Componentes

### Configuração
- **MigrationBatchConfig**: Orquestra job e steps do Spring Batch
- **MigrationProperties**: Propriedades de configuração carregadas de application-local.properties

### Extração (Readers)
- **ClienteItemReader**: Lê cliente.bson usando BsonFileReader
- **FuncionarioItemReader**: Lê funcionario.bson usando BsonFileReader

### Transformação (Processors)
- **ClienteItemProcessor**: MongoDB cliente → PostgreSQL customer
  - Extrai legacy_id do ObjectId
  - Criptografa documento (CPF/CNPJ)
  - Resolve endereço (Address)
  - Resolve created_by (Employee)
  
- **FuncionarioItemProcessor**: MongoDB funcionario → PostgreSQL employee + userAccount
  - Extrai legacy_id do ObjectId
  - Criptografa documento (CPF/CNPJ)
  - Hash de senha com BCrypt
  - Mapeia role_level → UserRole (ADMIN, MANAGER, EMPLOYEE)

### Carga (Writers)
- **CustomerItemWriter**: Persiste Customer no PostgreSQL
- **EmployeeItemWriter**: Persiste Employee no PostgreSQL
- **UserAccountItemWriter**: Persiste UserAccount no PostgreSQL

### Validação & Auditoria
- **MigrationValidator**: Validações pós-migração (contagem, integridade, amostragem)
- **MigrationJobListener**: Listener de eventos do Job (beforeJob, afterJob)

### Utilitários
- **BsonFileReader**: Leitura de arquivos .bson usando MongoDB BSON library
- **LegacyIdMapper**: Mapeamento ObjectId → legacy_id

### DTOs
- **ClienteDocument**: Representação intermediária de cliente do MongoDB
- **FuncionarioDocument**: Representação intermediária de funcionário do MongoDB

## 🚀 Como Usar

### 1. Configurar Properties

Edite `src/main/resources/application-local.properties`:

```properties
# Habilitar migração
migration.enabled=true

# Quais entidades migrar
migration.entities=cliente,funcionario

# Caminho dos arquivos BSON
migration.bson.base-path=.legado/noivabd

# Configurações de batch
migration.batch.chunk-size=100
migration.batch.skip-limit=10
migration.batch.max-retries=3
migration.batch.retry-backoff-ms=1000
```

### 2. Executar Migração

#### Opção A: Via Spring Boot (Automático)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

#### Opção B: Via REST API

Após iniciar a aplicação, execute:

```bash
curl -X POST http://localhost:8080/api/migration/start?entities=cliente,funcionario
```

### 3. Monitorar Logs

```bash
tail -f logs/rentafit.log
```

Procure por:
- `STARTING MIGRATION JOB`
- `Step: clienteStep - Read: X, Write: Y, Skip: Z`
- `Step: funcionarioStep - Read: X, Write: Y, Skip: Z`
- `MIGRATION JOB COMPLETED`

### 4. Validar Resultados

```sql
-- Contar registros migrados
SELECT 
    (SELECT COUNT(*) FROM people WHERE legacy_id IS NOT NULL) as total_people,
    (SELECT COUNT(*) FROM people WHERE type = 'Customer') as total_customers,
    (SELECT COUNT(*) FROM people WHERE type = 'Employee') as total_employees,
    (SELECT COUNT(*) FROM user_accounts) as total_user_accounts;

-- Verificar dados criptografados
SELECT id, name, document, email
FROM people
LIMIT 5;

-- Verificar integridade referencial
SELECT 
    c.name AS customer_name,
    e.name AS created_by_employee
FROM people c
LEFT JOIN people e ON c.created_by = e.id
WHERE c.type = 'Customer'
LIMIT 10;
```

## 📊 Fluxo de Execução

```
┌─────────────────────────────────────┐
│  1. Job Launch (JobLauncher)        │
│     - MigrationJobListener.beforeJob│
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  2. Step: clienteStep                │
│     ┌─────────────────────────┐     │
│     │ ClienteItemReader       │     │
│     │ - BsonFileReader        │     │
│     │ - cliente.bson          │     │
│     └──────────┬──────────────┘     │
│                ▼                     │
│     ┌─────────────────────────┐     │
│     │ ClienteItemProcessor    │     │
│     │ - Extract legacy_id     │     │
│     │ - Encrypt document      │     │
│     │ - Resolve address       │     │
│     └──────────┬──────────────┘     │
│                ▼                     │
│     ┌─────────────────────────┐     │
│     │ CustomerItemWriter      │     │
│     │ - saveAll(customers)    │     │
│     └─────────────────────────┘     │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  3. Step: funcionarioStep            │
│     (mesma estrutura)                │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  4. Step: validationStep             │
│     - MigrationValidator.validate() │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  5. Job Complete                     │
│     - MigrationJobListener.afterJob │
│     - Log statistics                │
└─────────────────────────────────────┘
```

## 🔧 Recursos do Spring Batch

### Chunk Processing
- Processa registros em lotes de 100 (configurável)
- Commit a cada chunk completado
- Rollback automático em caso de erro no chunk

### Fault Tolerance
- **Skip**: Pula registros com erro (max: 10)
- **Retry**: Tenta novamente em caso de erro transiente (max: 3x)
- **BackOff**: Aguarda 1000ms entre retries

### Job Repository
- Metadata de execução armazenada em tabelas Spring Batch
- Rastreamento de JobExecution, StepExecution
- Permite restart de jobs falhados

## 🔐 Segurança

### Criptografia
- ✅ **Document (CPF)**: Criptografado via `DatabaseEncryptionConverter`
- ✅ **Passwords**: Hash BCrypt com salt

### Auditoria
- ✅ **legacy_id**: Campo rastreado em people.legacy_id
- ✅ **Job History**: Spring Batch Job Repository

## ⚠️ Troubleshooting

### Erro: "BSON file not found"
- Verifique `migration.bson.base-path` em application-local.properties
- Certifique-se que `.legado/noivabd/cliente.bson` existe

### Erro: "No documents loaded"
- Arquivo BSON pode estar corrompido
- Verifique logs: `logging.level.br.com.rentafit.migration=DEBUG`

### Erro: "Validation failed"
- Verifique logs do ValidationStep
- Valide manualmente com queries SQL
- Configure `migration.skip-validation=true` para pular validação (não recomendado)

### Password não sendo hasheado
- Verifique que `PasswordEncoder` está configurado no Spring Context
- Logs devem mostrar: `"Password encoded with BCrypt"`

## 📈 Métricas

| Métrica | Localização |
|---------|-------------|
| **Registros lidos** | StepExecution.readCount |
| **Registros escritos** | StepExecution.writeCount |
| **Registros pulados** | StepExecution.skipCount |
| **Tempo de execução** | JobExecution.duration |

Acesse via logs ou query:

```sql
SELECT * FROM batch_job_execution ORDER BY start_time DESC LIMIT 1;
SELECT * FROM batch_step_execution WHERE job_execution_id = ?;
```

## 🔄 Próximas Fases

### Fase 2: Outras Entidades
- Contratos (contrato.bson)
- Roupas (roupa.bson)
- Finanças (financa.bson)

### Fase 3: Testes Completos
- Testes unitários com @SpringBatchTest
- Testes de integração com H2

### Fase 4: Performance
- Análise de performance com grandes volumes
- Otimização de chunk size
- Parallel steps

## 📚 Referências

- [Spring Batch 5.x Documentation](https://docs.spring.io/spring-batch/docs/current/reference/html/)
- [MongoDB BSON Specification](https://bsonspec.org/)
- [.legado/REVISED_STRATEGY.md](../../../.legado/REVISED_STRATEGY.md)

---

**Status:** ✅ Implementado - Pronto para uso  
**Última atualização:** 10/01/2026
  - Gera UUID novo
  - Extrai legacy_id
  - **Criptografa document (CPF)**
  - Resolve endereço
  - Resolve funcionário criador

- **FuncionarioItemProcessor**: MongoDB funcionario → PostgreSQL employee + userAccount
  - Gera UUID novo
  - Extrai legacy_id
  - **Criptografa document (CPF)**
  - **Hash de senha com BCrypt**
  - Mapeia nivel_acesso → UserRole

### Carga (Writers)
- **CustomerItemWriter**: Persiste Customer no PostgreSQL
- **EmployeeItemWriter**: Persiste Employee no PostgreSQL
- **UserAccountItemWriter**: Persiste UserAccount no PostgreSQL

### Validação & Auditoria
- **MigrationValidator**: Validações pós-migração
- **MigrationJobListener**: Listener de eventos do job

### Utilitários
- **BsonFileReader**: Leitura de arquivos .bson (stub - TODO: parsing real)
- **LegacyIdMapper**: Mapeamento ObjectId → legacy_id

### DTOs
- **ClienteDocument**: Representação intermediária de cliente
- **FuncionarioDocument**: Representação intermediária de funcionário

---

## 🚀 Como Usar

### 1. Ativar Migração

Editar `application-local.properties`:

```properties
migration.enabled=true
migration.entities=cliente,funcionario
```

### 2. Configurar Propriedades (Optional)

```properties
# Tamanho do lote
migration.batch.chunk-size=100

# Retry configuration
migration.batch.max-retries=3
migration.batch.retry-backoff-ms=1000

# Pular validações
migration.skip-validation=false

# Caminho do BSON
migration.bson.base-path=.legado/noivabd
```

### 3. Iniciar Aplicação

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

O job será executado automaticamente na inicialização se `migration.enabled=true`.

### 4. Monitorar Execução

Ver logs no console (DEBUG habilitado):
```
[INFO] Starting migration job...
[INFO] Processing cliente...
[INFO] Processing funcionario...
[INFO] Validation step...
[INFO] ✓ Job completed successfully
```

---

## 🔒 Segurança

### Criptografia
- ✅ **Document (CPF)**: Criptografado via `DatabaseEncryptionConverter`
- ✅ **Password**: Hash BCrypt para funcionários

### Auditoria
- ✅ **legacy_id**: Campo rastreado em people.legacy_id
- ✅ **Logging**: Cada transformação é logada

---

## 🧪 Estrutura de Testes

Criar em `src/test/java/br/com/rentafit/migration/`:

```
migration-test/
├── processor/
│   ├── ClienteItemProcessorTest.java
│   └── FuncionarioItemProcessorTest.java
├── reader/
│   ├── ClienteItemReaderTest.java
│   └── FuncionarioItemReaderTest.java
├── validator/
│   └── MigrationValidatorTest.java
└── config/
    └── MigrationBatchConfigTest.java
```

### Exemplo de Teste

```java
@SpringBatchTest
@DataJpaTest
@Import({MigrationBatchConfig.class, ClienteItemProcessor.class})
class MigrationBatchTest {
    
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    void testClienteMigrationJobComplete() throws Exception {
        // Given
        ClienteDocument cliente = createSampleCliente();
        
        // When
        JobExecution execution = jobLauncherTestUtils.launchJob();
        
        // Then
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertThat(customerRepository.count()).isGreaterThan(0);
    }
}
```

---

## 📊 Fluxo de Dados

```
MongoDB (cliente.bson)
   │
   ├─→ ClienteItemReader
   ├─→ ClienteItemProcessor
   │   ├─ UUID generation
   │   ├─ legacy_id mapping
   │   ├─ Document encryption
   │   └─ Address resolution
   ├─→ CustomerItemWriter
   │
   └─→ PostgreSQL (customers)

MongoDB (funcionario.bson)
   │
   ├─→ FuncionarioItemReader
   ├─→ FuncionarioItemProcessor
   │   ├─ UUID generation
   │   ├─ legacy_id mapping
   │   ├─ Document encryption
   │   ├─ Password hashing
   │   └─ Role mapping
   ├─→ EmployeeItemWriter
   ├─→ UserAccountItemWriter
   │
   └─→ PostgreSQL (employees, user_accounts)

Both
   │
   └─→ MigrationValidator
       ├─ Count records
       ├─ Verify uniqueness
       ├─ Check referential integrity
       └─ Sample data validation
```

---

## ⚙️ Configuração Detalhada

### MigrationProperties

```yaml
migration:
  enabled: false                          # Ativar/desativar job
  entities: cliente,funcionario           # Quais entidades migrar
  skip-validation: false                  # Pular validações
  skip-cleanup: false                     # Pular limpeza
  audit-enabled: true                     # Habilitar auditoria
  batch:
    chunk-size: 100                       # Tamanho do lote
    max-retries: 3                        # Tentativas de retry
    retry-backoff-ms: 1000                # Espera antes de retry (ms)
    skip-limit: 10                        # Máximo de erros a ignorar
  bson:
    base-path: .legado/noivabd            # Caminho dos arquivos
    cliente-file-name: cliente.bson
    funcionario-file-name: funcionario.bson
```

---

## 📝 Documentação Relacionada

- **MONGODB_SCHEMA.md**: Análise da estrutura do MongoDB legado
- **MIGRATION_MAPPING.md**: Mapeamento detalhado MongoDB → PostgreSQL
- **ETL_STRATEGY.md**: Estratégia e arquitetura Spring Batch
- **FUTURE_ENTITIES_MAPPING.md**: Referência para futuras migrações
- **IMPLEMENTATION_PLAN.md**: Plano de implementação completo

---

## 🚨 Troubleshooting

### Erro: "BSON file not found"
**Solução**: Verificar se `migration.bson.base-path` aponta para `.legado/noivabd`

### Erro: "No documents loaded"
**Solução**: É esperado em POC. O parsing real de BSON precisa ser implementado (TODO)

### Erro: "Validation failed"
**Solução**: Verificar `migration.skip-validation=true` ou corrigir dados antes de migrar

### Password não sendo hasheado
**Solução**: Verificar se `PasswordEncoder` está injetado no processor

---

## 🔄 Status de Implementação

| Componente | Status | Notas |
|-----------|--------|-------|
| MigrationBatchConfig | ✅ | Completo |
| ClienteItemReader | ✅ | Stub - parsear real TODO |
| FuncionarioItemReader | ✅ | Stub - parsear real TODO |
| ClienteItemProcessor | ✅ | Completo com criptografia |
| FuncionarioItemProcessor | ✅ | Completo com hash |
| Writers | ✅ | Completo |
| Validator | ⚠️ | Stub - validações TODO |
| Listener | ✅ | Completo |
| Tests | 🔄 | A implementar |
| BSON Parsing Real | 🔄 | Próxima fase |

---

## 📚 Próximas Fases

### Fase 2: Parsing BSON Real
- Integrar bson4jackson ou mongo-java-driver
- Implementar desserialização real de BSON
- Testar com arquivos reais

### Fase 3: Testes Completos
- Testes unitários com @SpringBatchTest
- Testes de integração E2E
- Cobertura Jacoco
- Performance testing

### Fase 4: Outras Entidades
- Contratos (contrato.bson)
- Equipamentos (roupa.bson)
- Categorias (categoria.bson)
- Dados financeiros (financa.bson)

---

## 📞 Suporte

Para problemas ou perguntas:
1. Verificar logs: `logging.level.br.com.rentafit=DEBUG`
2. Consultar MIGRATION_MAPPING.md
3. Consultar ETL_STRATEGY.md
4. Verificar com a equipe de desenvolvimento

---

**Migration Module v0.1.0**
Rentafit Backend - Sistema de Gestão de Locação


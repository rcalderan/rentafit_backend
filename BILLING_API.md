# Endpoints de Emissão de NFS-e

Este documento detalha os endpoints implementados para emissão e gestão de Notas Fiscais de Serviço Eletrônicas (NFS-e) através do Portal Nacional.

## Autenticação

Todos os endpoints requerem autenticação via JWT Bearer Token no header:
```
Authorization: Bearer <seu-token-jwt>
```

## Endpoints Disponíveis

### 1. Emitir NFS-e

**POST** `/api/billing/invoices/emit`

Emite uma nova NFS-e para um cliente através do Portal Nacional.

**Permissões:** `ADMIN`, `EMPLOYEE`

**Request Body:**
```json
{
  "customerId": "uuid-do-cliente",
  "serviceValue": 1000.00,
  "nbsCode": "1.0101",
  "serviceDescription": "Serviços de consultoria empresarial",
  "cityCode": "3550308",
  "ibsRate": 0.025,
  "cbsRate": 0.015,
  "isqnRate": 0.0
}
```

**Response (201 Created):**
```json
{
  "id": "uuid-interno",
  "accessKey": "12345678901234567890123456789012345678901234567890",
  "invoiceNumber": 123456,
  "protocol": "PR123456789",
  "status": "AUTHORIZED",
  "issueDate": "2026-01-18T17:30:00-03:00",
  "processingDate": "2026-01-18T17:30:05-03:00",
  "serviceValue": 1000.00,
  "taxes": {
    "ibsRate": 0.025,
    "ibsValue": 25.00,
    "cbsRate": 0.015,
    "cbsValue": 15.00,
    "isqnRate": 0.0,
    "isqnValue": 0.0,
    "totalTaxValue": 40.00
  }
}
```

**Possíveis Erros:**
- `400 Bad Request`: Dados inválidos na requisição
- `401 Unauthorized`: Token inválido ou expirado
- `422 Unprocessable Entity`: Erro de validação no Portal Nacional

---

### 2. Consultar NFS-e por ID Interno

**GET** `/api/billing/invoices/{id}`

Busca uma NFS-e no banco de dados local pelo ID interno.

**Permissões:** `ADMIN`, `EMPLOYEE`, `CUSTOMER`

**Response (200 OK):**
```json
{
  "id": "uuid",
  "accessKey": "12345...",
  "invoiceNumber": 123456,
  "customer": { ... },
  "issueDate": "2026-01-18T17:30:00-03:00",
  "serviceValue": 1000.00,
  "taxes": { ... },
  "status": "AUTHORIZED",
  "createdAt": "2026-01-18T17:30:00-03:00"
}
```

---

### 3. Consultar NFS-e no Portal Nacional

**GET** `/api/billing/invoices/chave/{chaveAcesso}`

Consulta os detalhes de uma NFS-e autorizada diretamente no Portal Nacional usando a chave de acesso.

**Permissões:** `ADMIN`, `EMPLOYEE`, `CUSTOMER`

**Exemplo:** `/api/billing/invoices/chave/12345678901234567890123456789012345678901234567890`

**Response (200 OK):**
```json
{
  "chaveAcesso": "12345...",
  "numero": 123456,
  "status": "AUTORIZADA",
  "dhAutorizacao": "2026-01-18T17:30:05-03:00",
  "prestador": {
    "cnpj": "12345678000100",
    "inscricaoMunicipal": "12345",
    "razaoSocial": "Empresa Prestadora LTDA"
  },
  "tomador": {
    "cpfCnpj": "12345678900",
    "nome": "Cliente Exemplo"
  },
  "servico": {
    "codigoNbs": "1.0101",
    "descricao": "Serviços de consultoria"
  },
  "valores": {
    "valorServico": 1000.00,
    "valorIbs": 25.00,
    "valorCbs": 15.00,
    "valorIsqn": 0.0,
    "valorTotal": 1000.00
  }
}
```

---

### 4. Download do PDF (DANFSe)

**GET** `/api/billing/invoices/chave/{chaveAcesso}/pdf`

Baixa o Documento Auxiliar da NFS-e (DANFSe) em formato PDF diretamente do Portal Nacional.

**Permissões:** `ADMIN`, `EMPLOYEE`, `CUSTOMER`

**Response (200 OK):**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="NFSe_<chave>.pdf"`
- Body: Bytes do arquivo PDF

---

### 5. Download do XML Legal

**GET** `/api/billing/invoices/chave/{chaveAcesso}/xml`

Baixa o arquivo XML legal da NFS-e diretamente do Portal Nacional.

**Permissões:** `ADMIN`, `EMPLOYEE`, `CUSTOMER`

**Response (200 OK):**
- Content-Type: `application/xml`
- Content-Disposition: `attachment; filename="NFSe_<chave>.xml"`
- Body: Conteúdo XML

---

### 6. Consultar por Número da Nota

**GET** `/api/billing/invoices/numero/{numeroNota}`

Busca uma NFS-e no banco de dados local pelo número da nota fiscal.

**Permissões:** `ADMIN`, `EMPLOYEE`

**Exemplo:** `/api/billing/invoices/numero/123456`

---

## Configurações Necessárias

### Variáveis de Ambiente

```properties
# Portal NFS-e
NFSE_API_URL=https://hom.nfse.gov.br/via/
NFSE_STS_URL=https://hom.nfse.gov.br/api/token
NFSE_STS_ENABLED=true

# Certificado Digital ICP-Brasil
NFSE_CERT_PATH=.cert/certificado.p12
NFSE_CERT_PASSWORD=senha-do-certificado

# Dados do Prestador
NFSE_PRESTADOR_CNPJ=12345678000100
NFSE_PRESTADOR_IM=12345

# Ambiente (1=Produção, 2=Homologação)
NFSE_AMBIENTE=2

# Tributos (alíquotas padrão)
NFSE_IBS_ALIQUOTA=0.025
NFSE_CBS_ALIQUOTA=0.015
```

### Certificado Digital

1. Obtenha um certificado digital ICP-Brasil tipo A1 (arquivo .pfx ou .p12)
2. Coloque o certificado na pasta `.cert/` na raiz do projeto
3. Configure a senha do certificado na variável `NFSE_CERT_PASSWORD`

## Fluxo de Emissão

1. **Autenticação OAuth2 (Automática)**
   - O sistema obtém automaticamente um Bearer Token via STS usando o certificado digital
   - O token é renovado automaticamente quando expira

2. **Envio do DPS**
   - O sistema constrói a Declaração de Prestação de Serviços (DPS) conforme NT 004
   - Calcula automaticamente os tributos IBS e CBS
   - Envia para o Portal Nacional via mTLS

3. **Processamento**
   - Síncrono (201): Nota autorizada imediatamente
   - Assíncrono (202): Necessário consultar posteriormente pelo protocolo

4. **Armazenamento**
   - A nota autorizada é salva no banco de dados local
   - Consultas podem ser feitas tanto localmente quanto no portal

## Códigos NBS Comuns

| Código | Descrição |
|--------|-----------|
| 1.0101 | Análise e desenvolvimento de sistemas |
| 1.0102 | Programação |
| 1.0103 | Processamento de dados |
| 1.0104 | Elaboração de programas de computadores |
| 1.0105 | Licenciamento ou cessão de software |
| 1.0106 | Assessoria e consultoria em informática |

Para a lista completa, consulte a tabela NBS oficial da Receita Federal.

## Swagger UI

Acesse a documentação interativa em: `http://localhost:8080/swagger-ui.html`

## Observações Importantes

1. **Ambiente de Homologação**: Por padrão, o sistema está configurado para o ambiente de testes. Para produção, altere `NFSE_AMBIENTE=1`.

2. **Certificado Válido**: O certificado deve estar válido e dentro do prazo de validade.

3. **Dados do Prestador**: Configure corretamente o CNPJ e Inscrição Municipal antes de emitir notas.

4. **Código do Município**: Use o código IBGE do município (7 dígitos). Exemplo: São Paulo = 3550308.

5. **Alíquotas**: Se não informadas na requisição, serão usadas as alíquotas padrão configuradas.

## Tratamento de Erros

O sistema trata automaticamente:
- Renovação de token expirado
- Retry em caso de falha temporária de rede
- Validação de dados antes do envio
- Log detalhado de todas as operações

Erros retornados pelo Portal Nacional são repassados com as mensagens originais para facilitar a correção.

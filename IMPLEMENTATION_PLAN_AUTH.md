# Plano de Implementação: Sistema de Autenticação JWT e OAuth2

Este documento descreve as etapas para implementar um sistema de autenticação robusto e seguro no projeto Rentafit, utilizando JWT (JSON Web Tokens) e preparando o terreno para integração com provedores OAuth2.

## 1. Arquitetura Proposta

*   **Access Token**: JWT de curta duração (ex: 15 min), armazenado no cliente ou em Cookie `httpOnly`.
*   **Refresh Token**: Token de longa duração (ex: 7 dias), persistido no banco de dados para controle de revogação e segurança.
*   **Biblioteca**: `com.auth0:java-jwt` para manipulação de tokens.
*   **Segurança**: Spring Security com filtros customizados.

## 2. Comparativo: httpOnly Cookies vs. Persistência em Banco

### httpOnly Cookies
*   **O que é**: Um cookie que não pode ser acessado via JavaScript (`document.cookie`), mitigando ataques XSS.
*   **Prós**:
    *   Proteção automática contra roubo de tokens via scripts maliciosos.
    *   Enviado automaticamente pelo navegador em cada requisição.
*   **Contras**:
    *   Requer proteção adicional contra CSRF (Cross-Site Request Forgery).
    *   Configuração de CORS e SameSite mais rigorosa.

### Persistência de Refresh Tokens (Banco de Dados)
*   **Por que**: Tokens JWT por natureza são apátridas (stateless). Para invalidar um token antes do vencimento (logout forçado, troca de senha), precisamos de uma referência no servidor.
*   **Prós**:
    *   Controle total: Possibilidade de revogar sessões específicas.
    *   Segurança: Pode-se detectar uso indevido de tokens de renovação.
*   **Contras**:
    *   Adiciona uma consulta ao banco em cada renovação de token (custo de performance).

---

## 3. Etapas de Execução

### Fase 1: Preparação e Dependências
1.  Adicionar `com.auth0:java-jwt` ao `pom.xml`.
2.  Configurar variáveis de ambiente no `application.properties` (Secret Key, Expiration).

### Fase 2: Domínio e Persistência de Tokens
1.  Criar entidade `RefreshToken` para armazenar o token, data de expiração e o usuário associado.
2.  Implementar `RefreshTokenRepository` (H2 inicialmente, preparado para Postgres/DynamoDB).

### Fase 3: Lógica de JWT (TokenService)
1.  Serviço para gerar Access Tokens.
2.  Serviço para gerar/validar Refresh Tokens.
3.  Implementar rotação de Refresh Tokens (gerar um novo a cada uso para maior segurança).

### Fase 4: Segurança e Filtros
1.  Implementar `JwtAuthenticationFilter` para validar o token em cada requisição.
2.  Configurar `SecurityConfig` para proteger as rotas `/api/**` e permitir `/auth/**`.
3.  Configurar o `AuthenticationManager`.

### Fase 5: Endpoints de Autenticação (AuthController)
1.  `POST /api/auth/login`: Autentica, gera tokens e (opcionalmente) define Cookies.
2.  `POST /api/auth/refresh`: Valida o Refresh Token e gera um novo Access Token.
3.  `POST /api/auth/logout`: Invalida o Refresh Token no banco.

### Fase 6: Preparação OAuth2
1.  Configurar as bases do `spring-boot-starter-oauth2-client`.
2.  Preparar o `UserAccount` para aceitar `provider_id` (Google, GitHub, etc).

---

## 4. Configurações de Ambiente (Variáveis)

As seguintes variáveis devem ser definidas (ou usar valores default em `application-local.properties`):

*   `RENTFIT_JWT_SECRET`: Chave mestra para assinatura do token.
*   `RENTFIT_ACCESS_TOKEN_EXPIRATION`: Tempo em milissegundos (ex: 900000 para 15min).
*   `RENTFIT_REFRESH_TOKEN_EXPIRATION`: Tempo em milissegundos (ex: 604800000 para 7 dias).

## 5. Próximos Passos
1. Adicionar dependência no `pom.xml`.
2. Criar DTOs de Request/Response de Login.
3. Implementar o `TokenService`.


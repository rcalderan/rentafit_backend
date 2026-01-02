# VERSIONING.md - Estratégia de Versionamento

## 📦 Versionamento Semântico (Semantic Versioning)

O projeto segue **[SemVer 2.0.0](https://semver.org/)** para versionamento.

### Formato: MAJOR.MINOR.PATCH

```
v1.2.3
│ │ │
│ │ └─ PATCH: correções de bugs, sem mudanças de API
│ └──── MINOR: novas funcionalidades, retrocompatível
└─────  MAJOR: mudanças incompatíveis com versão anterior
```

### Exemplos

| Versão | Mudança | Tipo |
|--------|---------|------|
| v1.0.0 | Primeira release | MAJOR |
| v1.1.0 | Nova feature de login | MINOR |
| v1.1.1 | Fix em erro de validação | PATCH |
| v2.0.0 | Reorganização de endpoints (breaking) | MAJOR |
| v2.0.1 | Security patch crítico | PATCH |

---

## 🔄 Processo de Release

### 1. **Desenvolver no `develop`**

```bash
# Múltiplas features/bugfixes em develop
git checkout develop
git pull origin develop

# Criar feature
git checkout -b feature/nova-funcionalidade
# ... desenvolvimento ...
git push origin feature/nova-funcionalidade
# ... merge via PR ...
```

### 2. **Preparar Release**

Quando `develop` está pronto para produção:

```bash
# Determinar versão (ex: 1.1.0)
# Criar release branch
git checkout -b release/1.1.0 develop
git push -u origin release/1.1.0
```

**Na release branch:**
```bash
# Atualizar version em pom.xml
# <version>1.1.0</version>

# Atualizar CHANGELOG.md
# Atualizar README.md se necessário

git commit -m "chore: bump version to 1.1.0"
git push origin release/1.1.0
```

### 3. **Testes e Ajustes Finais**

```bash
# Na release branch, fazer últimos ajustes
git commit -m "chore: update dependencies for 1.1.0"
git push origin release/1.1.0

# Executar testes finais
mvn clean verify

# Build final
mvn clean package
```

### 4. **Merge em `master`**

```bash
# Via GitHub: criar PR release/1.1.0 → master
# Aguardar aprovação e CI/CD passar
# Fazer merge no GitHub
```

### 5. **Tagear e Deploy**

```bash
# Atualizar local
git checkout master
git pull origin master

# Criar tag
git tag -a v1.1.0 -m "Release version 1.1.0"

# Push tag
git push origin v1.1.0

# Criar GitHub Release (automático ou manual)
```

### 6. **Sincronizar `develop`**

```bash
# Fazer merge de master em develop
git checkout develop
git pull origin develop
git merge master
git push origin develop

# Deletar release branch
git push origin --delete release/1.1.0
git branch -d release/1.1.0
```

---

## 🚨 Hotfix Process

Para correções críticas em produção:

### 1. **Criar Hotfix a partir de `master`**

```bash
# Bug encontrado em produção (v1.1.0)
# Próxima versão será v1.1.1

git checkout -b hotfix/security-vulnerability master
git push -u origin hotfix/security-vulnerability
```

### 2. **Implementar Correção**

```bash
# Fazer fix
git commit -m "fix: security vulnerability in authentication"

# Atualizar version
# <version>1.1.1</version>

git commit -m "chore: bump version to 1.1.1"
git push origin hotfix/security-vulnerability
```

### 3. **Merge em `master` com Tag**

```bash
# Via GitHub: criar PR hotfix → master
# Aguardar aprovação

# Localmente após merge:
git checkout master
git pull origin master

# Tag
git tag -a v1.1.1 -m "Hotfix version 1.1.1"
git push origin v1.1.1
```

### 4. **Merge também em `develop`**

```bash
git checkout develop
git pull origin develop
git merge hotfix/security-vulnerability
git push origin develop

# Deletar hotfix
git push origin --delete hotfix/security-vulnerability
git branch -d hotfix/security-vulnerability
```

---

## 📋 CHANGELOG

Manter arquivo `CHANGELOG.md` atualizado:

```markdown
# Changelog

## [Unreleased]

### Added
- Nova feature X
- Nova feature Y

### Changed
- Refactored mapper layer

### Fixed
- Bug na validação de email

---

## [1.1.0] - 2026-01-15

### Added
- JWT authentication endpoint
- User registration endpoint

### Changed
- Refactored PeopleMapper

### Fixed
- Null pointer exception in CustomerService

### Security
- Updated Spring Security to 6.2.0

---

## [1.0.0] - 2026-01-02

### Added
- Initial release
- Customer CRUD endpoints
- Employee CRUD endpoints
- Swagger/OpenAPI documentation
- Docker Compose configuration
```

---

## 🔖 Tags e Releases

### Criar Tag Localmente

```bash
# Lightweight tag
git tag v1.1.0

# Annotated tag (recomendado)
git tag -a v1.1.0 -m "Release version 1.1.0"

# Com assinatura GPG
git tag -s v1.1.0 -m "Release version 1.1.0"

# Listar tags
git tag -l

# Mostrar detalhes
git show v1.1.0
```

### Push Tags para Remote

```bash
# Push uma tag
git push origin v1.1.0

# Push todas as tags
git push origin --tags

# Deletar tag local
git tag -d v1.1.0

# Deletar tag remota
git push origin :refs/tags/v1.1.0
git push origin --delete v1.1.0
```

### Criar Release no GitHub

Via GitHub UI:
1. Acesse: **Releases → Draft a new release**
2. **Tag version:** v1.1.0
3. **Release title:** Version 1.1.0
4. **Description:** 
   ```markdown
   ## Features
   - JWT authentication
   - User registration
   
   ## Bug Fixes
   - Fixed null pointer in mapper
   
   ## Breaking Changes
   None
   ```
5. Anexe o JAR compilado
6. Publique release

---

## 🔢 Numbering Strategy

### Quando aumentar MAJOR (v2.0.0)?
- Mudanças breaking na API
- Reorganização de endpoints
- Mudança de arquitetura significativa

### Quando aumentar MINOR (v1.1.0)?
- Nova feature
- Nova endpoint
- Melhoria sem breaking changes

### Quando aumentar PATCH (v1.0.1)?
- Bug fix
- Performance improvement
- Security patch
- Atualização de dependência

---

## 🚀 Exemplo Completo

### Cenário: Implementar Nova Feature

```bash
# 1. Criar feature branch de develop
git checkout develop
git pull origin develop
git checkout -b feature/payment-integration

# 2. Desenvolver
# ... múltiplos commits ...
git commit -m "feat: add Payment entity and repository"
git commit -m "feat: implement PaymentService"
git commit -m "feat: create PaymentController"
git commit -m "test: add PaymentService tests"

# 3. Push e PR para develop
git push -u origin feature/payment-integration
# ... PR criado, aprovado, mergeado ...

# 4. Após múltiplas features em develop, preparar release 1.2.0
git checkout develop
git pull origin develop
git checkout -b release/1.2.0

# 5. Ajustes finais
# Atualizar pom.xml: 1.2.0
# Atualizar CHANGELOG.md
git commit -m "chore: bump version to 1.2.0"
git push origin release/1.2.0

# 6. Testes finais
mvn clean verify

# 7. PR e merge em master
# ... via GitHub UI ...

# 8. Tag e release
git checkout master
git pull origin master
git tag -a v1.2.0 -m "Release version 1.2.0"
git push origin v1.2.0

# 9. Sincronizar develop
git checkout develop
git pull origin develop
git merge master
git push origin develop

# 10. Deletar release branch
git push origin --delete release/1.2.0
git branch -d release/1.2.0
```

---

## 📊 Histórico de Versões

| Versão | Data | Features | Status |
|--------|------|----------|--------|
| v0.0.1 | 2026-01-02 | Initial setup | Development |
| v1.0.0 | TBD | CRUD completo | Release |
| v1.1.0 | TBD | JWT Auth | Release |
| v2.0.0 | TBD | Pagamentos | Planejado |

---

## 🔗 Referências

- [Semantic Versioning](https://semver.org/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Git Tagging](https://git-scm.com/book/en/v2/Git-Basics-Tagging)
- [GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases)

---

**Última atualização:** 2026-01-02


# QUICK START - Git Flow & CI/CD

Guia rápido de comandos essenciais para trabalhar com Git Flow.

---

## 🚀 Começar uma Feature

```bash
# 1. Atualizar develop
git checkout develop
git pull origin develop

# 2. Criar feature branch
git checkout -b feature/minha-funcionalidade

# 3. Fazer commits
git add .
git commit -m "feat: implementar login"
git commit -m "feat: adicionar validações"
git commit -m "test: adicionar testes para login"

# 4. Push para remote
git push -u origin feature/minha-funcionalidade

# 5. Criar PR no GitHub
# GitHub → Compare & Pull Request → Descrever mudança → Create Pull Request

# 6. Após aprovação, fazer merge
# GitHub → Merge pull request → Confirm merge

# 7. Deletar branch
git push origin --delete feature/minha-funcionalidade
git branch -d feature/minha-funcionalidade
```

---

## 🐛 Corrigir um Bug

```bash
# 1. Criar bugfix branch de develop
git checkout -b bugfix/corrigir-erro develop

# 2. Fazer fix
git commit -m "fix: resolver erro de null pointer"

# 3. Push e PR
git push -u origin bugfix/corrigir-erro
# ... merge via GitHub ...

# 4. Deletar branch
git branch -d bugfix/corrigir-erro
git push origin --delete bugfix/corrigir-erro
```

---

## 🚨 Hotfix (Correção Crítica em Produção)

```bash
# 1. Criar hotfix de master
git checkout -b hotfix/security-patch master

# 2. Fazer correção
git commit -m "fix: vulnerabilidade de segurança"

# 3. Atualizar version (v1.0.0 → v1.0.1)
# Editar pom.xml: <version>1.0.1</version>
git commit -m "chore: bump version to 1.0.1"

# 4. Push
git push -u origin hotfix/security-patch

# 5. Merge em master com tag
git checkout master
git merge hotfix/security-patch
git tag -a v1.0.1 -m "Hotfix version 1.0.1"
git push origin master v1.0.1

# 6. Merge também em develop
git checkout develop
git merge hotfix/security-patch
git push origin develop

# 7. Deletar hotfix
git branch -d hotfix/security-patch
git push origin --delete hotfix/security-patch
```

---

## 📦 Preparar Release

```bash
# 1. Criar release branch (ex: 1.1.0)
git checkout -b release/1.1.0 develop

# 2. Atualizar version em pom.xml
# <version>1.1.0</version>

# 3. Atualizar CHANGELOG.md com novidades

# 4. Commits finais
git commit -m "chore: update version to 1.1.0"
git commit -m "chore: update CHANGELOG"

# 5. Push
git push -u origin release/1.1.0

# 6. Testes finais
mvn clean verify

# 7. PR para master
git push origin release/1.1.0
# GitHub → Create PR: release/1.1.0 → master

# 8. Após merge em master
git checkout master
git pull origin master
git tag -a v1.1.0 -m "Release version 1.1.0"
git push origin v1.1.0

# 9. Sincronizar com develop
git checkout develop
git merge master
git push origin develop

# 10. Deletar release
git branch -d release/1.1.0
git push origin --delete release/1.1.0
```

---

## 📝 Convenção de Commits

```bash
# ✅ Exemplos corretos:

# Feature
git commit -m "feat: add JWT authentication"
git commit -m "feat: implement PaymentService"

# Bug fix
git commit -m "fix: prevent null pointer exception"
git commit -m "fix: resolve login error"

# Documentation
git commit -m "docs: update README"
git commit -m "docs: add API documentation"

# Refactor
git commit -m "refactor: simplify mapper code"
git commit -m "refactor: reorganize auth module"

# Test
git commit -m "test: add unit tests for CustomerService"
git commit -m "test: add integration tests"

# Chore (manutenção)
git commit -m "chore: update dependencies"
git commit -m "chore: bump Spring Boot to 3.4.2"

# ❌ Exemplos INCORRETOS:
git commit -m "fix stuff"          # Muito vago
git commit -m "updated"            # Sem contexto
git commit -m "WIP"                # Não descreve mudança
git commit -m "final version"      # Impreciso
```

---

## 🔍 Comandos Úteis

```bash
# Ver branches locais
git branch -a

# Ver histórico de commits
git log --oneline -n 10

# Ver diferenças
git diff develop..feature/minha-funcionalidade

# Verificar status
git status

# Atualizar local com remote
git fetch origin

# Deletar branch local
git branch -d feature/minha-funcionalidade

# Deletar branch remoto
git push origin --delete feature/minha-funcionalidade

# Renomear branch
git branch -m feature/nome-antigo feature/nome-novo

# Voltar para commit anterior
git revert <commit-hash>

# Ver tags
git tag -l

# Deletar tag
git tag -d v1.0.0
git push origin --delete v1.0.0
```

---

## ✅ Checklist antes de fazer Push

- [ ] Código compila sem erros: `mvn clean compile`
- [ ] Testes passam: `mvn test`
- [ ] Sem warnings de compilação
- [ ] Commits com mensagens descritivas
- [ ] Baseado na branch correta (develop para feature)
- [ ] Sem conflitos com develop/master
- [ ] Swagger annotations adicionadas
- [ ] Documentação atualizada

---

## 🔗 Ver PR Status no Terminal

```bash
# Listar PRs (usando GitHub CLI)
gh pr list

# Ver detalhes de um PR
gh pr view <numero-pr>

# Revisar PR localmente
gh pr checkout <numero-pr>

# Aprovar PR
gh pr review <numero-pr> --approve

# Solicitar mudanças
gh pr review <numero-pr> --request-changes

# Comentar em PR
gh pr comment <numero-pr> -b "Achei legal essa mudança"
```

---

## 📊 Ver Histórico com Gráfico

```bash
# Ver branches com gráfico
git log --graph --decorate --oneline --all

# Alias (adicionar ao .gitconfig)
git config --global alias.tree \
  'log --graph --decorate --oneline --all'

# Depois usar:
git tree
```

---

## 🆘 Resolver Conflitos

```bash
# Se tiver conflito no merge
git status  # Ver arquivos com conflito

# Editar arquivo:
# Procurar por: <<<<<<< HEAD
# Escolher qual versão manter
# Remover marcadores de conflito

# Depois:
git add .
git commit -m "chore: resolve merge conflicts"
git push origin
```

---

## 📈 Ver Estatísticas

```bash
# Commits por autor
git shortlog -s -n

# Linhas de código
git diff --stat <branch1> <branch2>

# Contributions
git log --pretty=format:"%an" | sort | uniq -c | sort -rn
```

---

## 🎓 Aprender Mais

```bash
# Ver ajuda de qualquer comando
git help <comando>

# Exemplos:
git help commit
git help merge
git help rebase
```

---

## 🔐 Boas Práticas Essenciais

1. ✅ **Sempre pull antes de push**
   ```bash
   git pull origin develop
   git push origin feature/minha-funcionalidade
   ```

2. ✅ **Commits pequenos e lógicos**
   - Um commit = uma mudança
   - Não misturar features diferentes

3. ✅ **Mensagens descritivas**
   - `feat: adicionar...` (não `fix: melhorias`)
   - Explicar **o quê** e **por quê**

4. ✅ **Fazer PR mesmo para mudanças pequenas**
   - Code review é importante
   - Documenta histórico

5. ✅ **Sincronizar com master antes de release**
   ```bash
   git checkout master
   git pull origin master
   ```

---

## 📞 Precisa de Ajuda?

- Dúvidas sobre Git Flow? → Veja `README.md#🔄-git-flow--branching-strategy`
- Guia de contribuição? → Veja `CONTRIBUTING.md`
- Versionamento? → Veja `VERSIONING.md`

---

**Última atualização:** 2026-01-02  
**Versão:** 1.0.0


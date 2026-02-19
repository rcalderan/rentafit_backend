# Script PowerShell para criar usuário IAM e política para deploy HK
# Uso: .\create-hk-user.ps1

$ErrorActionPreference = "Stop"

Write-Host "🔐 Criando usuário e política IAM para Deploy HK..." -ForegroundColor Cyan

# Variáveis
$POLICY_NAME = "RentafitHKDeployPolicy"
$USER_NAME = "rentafit-cicd-hk"
$POLICY_FILE = "infrastructure/iam/hk-deploy-policy.json"

# Obter Account ID
Write-Host "📋 Obtendo Account ID..." -ForegroundColor Yellow
$ACCOUNT_ID = (aws sts get-caller-identity --query Account --output text)
Write-Host "📋 Account ID: $ACCOUNT_ID" -ForegroundColor Green

# Criar a política
Write-Host "📝 Criando política $POLICY_NAME..." -ForegroundColor Yellow
try {
    $POLICY_ARN = (aws iam create-policy `
        --policy-name $POLICY_NAME `
        --policy-document file://$POLICY_FILE `
        --description "Permissões mínimas para deploy em Homologação (HK)" `
        --query 'Policy.Arn' `
        --output text)
    Write-Host "✅ Política criada: $POLICY_ARN" -ForegroundColor Green
} catch {
    $POLICY_ARN = "arn:aws:iam::${ACCOUNT_ID}:policy/${POLICY_NAME}"
    Write-Host "⚠️  Política já existe: $POLICY_ARN" -ForegroundColor Yellow
}

# Criar o usuário
Write-Host "👤 Criando usuário $USER_NAME..." -ForegroundColor Yellow
try {
    aws iam create-user `
        --user-name $USER_NAME `
        --tags Key=Project,Value=Rentafit Key=Environment,Value=HK
    Write-Host "✅ Usuário criado" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Usuário já existe" -ForegroundColor Yellow
}

# Anexar a política ao usuário
Write-Host "🔗 Anexando política ao usuário..." -ForegroundColor Yellow
aws iam attach-user-policy `
    --user-name $USER_NAME `
    --policy-arn $POLICY_ARN
Write-Host "✅ Política anexada" -ForegroundColor Green

# Criar Access Key
Write-Host "🔑 Criando Access Key..." -ForegroundColor Yellow
$ACCESS_KEY_OUTPUT = (aws iam create-access-key --user-name $USER_NAME | ConvertFrom-Json)

$ACCESS_KEY_ID = $ACCESS_KEY_OUTPUT.AccessKey.AccessKeyId
$SECRET_ACCESS_KEY = $ACCESS_KEY_OUTPUT.AccessKey.SecretAccessKey

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "✅ CONFIGURAÇÃO CONCLUÍDA!" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "📋 Adicione os seguintes secrets no GitHub:" -ForegroundColor Yellow
Write-Host ""
Write-Host "AWS_ACCESS_KEY_ID_HK:" -ForegroundColor White
Write-Host $ACCESS_KEY_ID -ForegroundColor Cyan
Write-Host ""
Write-Host "AWS_SECRET_ACCESS_KEY_HK:" -ForegroundColor White
Write-Host $SECRET_ACCESS_KEY -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  IMPORTANTE: Guarde essas credenciais em local seguro!" -ForegroundColor Red
Write-Host "⚠️  Elas não serão exibidas novamente." -ForegroundColor Red
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "📖 Próximos passos:" -ForegroundColor Yellow
Write-Host "1. Acesse: https://github.com/SEU_USUARIO/Rentafit/settings/secrets/actions" -ForegroundColor White
Write-Host "2. Adicione os secrets acima (clique em 'New repository secret')" -ForegroundColor White
Write-Host "3. Configure também: AWS_REGION_HK (ex: us-east-1)" -ForegroundColor White
Write-Host "4. Revise: infrastructure/iam/README.md para secrets adicionais" -ForegroundColor White
Write-Host ""

# Salvar em arquivo temporário
$outputFile = "hk-credentials-$(Get-Date -Format 'yyyy-MM-dd-HHmmss').txt"
@"
AWS_ACCESS_KEY_ID_HK=$ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY_HK=$SECRET_ACCESS_KEY
AWS_REGION_HK=us-east-1

Created: $(Get-Date)
User: $USER_NAME
Policy: $POLICY_NAME
"@ | Out-File -FilePath $outputFile -Encoding UTF8

Write-Host "💾 Credenciais salvas em: $outputFile" -ForegroundColor Green
Write-Host "⚠️  Deletar este arquivo após adicionar no GitHub!" -ForegroundColor Red


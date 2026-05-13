# Rentafit HK - Infraestrutura EC2 Spot (Custo Mínimo)

> Arquitetura de baixo custo para ambiente de homologação (HK).
> Economia: ~92% comparado ao ECS Fargate (de ~$33/mês para ~$3/mês).

---

## Componentes

| Arquivo | Descrição |
|---------|-----------|
| `ec2-hk.yaml` | EC2 t3.small Spot + Docker + S3 Bucket |
| `lambda-scheduler.yaml` | Lambdas start/stop + EventBridge (agendamento 8h-18h) |

---

## Pré-requisitos

1. **VPC e Subnet** (ou usar default):
   ```bash
   # Se nao tiver VPC criada, usar a default da conta:
   aws ec2 describe-vpcs --filters "Name=isDefault,Values=true"
   aws ec2 describe-subnets --filters "Name=default-for-az,Values=true"
   ```

2. **Secrets GitHub** configuradas:
   - `AWS_ACCESS_KEY_ID_HK`
   - `AWS_SECRET_ACCESS_KEY_HK`
   - `AWS_REGION` (ex: us-east-1)
   - `AWS_ACCOUNT_ID` (ID da conta AWS)
   - `HK_INSTANCE_ID` (preencher apos criar EC2)
   - `HK_DB_PASSWORD` (senha do PostgreSQL)

---

## Deploy da Infraestrutura

### 1. Criar EC2 Instance

```bash
# Usando VPC default (substituir vpc-xxx e subnet-xxx pelos valores reais)
aws cloudformation deploy \
  --stack-name rentafit-hk-ec2 \
  --template-file ec2-hk.yaml \
  --capabilities CAPABILITY_IAM \
  --parameter-overrides \
    VpcId=vpc-xxx \
    SubnetId=subnet-xxx \
    AllowedIp=SEU_IP/32 \
    DbPassword=senha_segura_aqui

# Capturar Instance ID para o proximo passo
aws cloudformation describe-stacks \
  --stack-name rentafit-hk-ec2 \
  --query 'Stacks[0].Outputs[?OutputKey==`InstanceId`].OutputValue' \
  --output text
```

**Notas:**
- `AllowedIp`: seu IP publico em formato CIDR (ex: `201.42.1.1/32`)
- A instancia inicia automaticamente via UserData (pode levar 5-10 min)
- A instancia eh Spot - pode ser interrompida, mas configura para `stop` (nao terminate)

### 2. Criar Agendamento Start/Stop

```bash
# Substituir i-xxxxxxxx pelo Instance ID do passo anterior
aws cloudformation deploy \
  --stack-name rentafit-hk-scheduler \
  --template-file lambda-scheduler.yaml \
  --capabilities CAPABILITY_IAM \
  --parameter-overrides \
    InstanceId=i-xxxxxxxx

# Opcional: cron personalizado (ex: 07:00-19:00)
aws cloudformation deploy \
  --stack-name rentafit-hk-scheduler \
  --template-file lambda-scheduler.yaml \
  --parameter-overrides \
    InstanceId=i-xxxxxxxx \
    StartCron="0 10 ? * MON-FRI *" \
    StopCron="0 22 ? * MON-FRI *"
```

### 3. Configurar Secrets GitHub

Adicionar ao repositorio GitHub (Settings > Secrets and variables > Actions):

| Secret | Valor |
|--------|-------|
| `HK_INSTANCE_ID` | `i-xxxxxxxx` (do passo 1) |
| `HK_DB_PASSWORD` | Mesma senha usada no DbPassword |
| `AWS_ACCOUNT_ID` | `123456789` (ID da conta, sem hifen) |

---

## Operações Diárias

### Verificar Status da Instancia

```bash
aws ec2 describe-instances \
  --instance-ids i-xxxxxxxx \
  --query 'Reservations[0].Instances[0].[State.Name,PublicIpAddress,InstanceType]' \
  --output table
```

### Start Manual (fora do horario)

```bash
aws ec2 start-instances --instance-ids i-xxxxxxxx
```

### Stop Manual (economia)

```bash
aws ec2 stop-instances --instance-ids i-xxxxxxxx
```

### Acesso via Session Manager (sem SSH)

```bash
# Conectar a instancia sem precisar de key pair
aws ssm start-session --target i-xxxxxxxx

# Dentro da instancia:
sudo su -
cd /opt/rentafit
docker-compose logs -f app
docker exec -it rentafit-postgres psql -U postgres -d rentafit
```

### Deploy Manual (emergencia)

Se o CI/CD falhar, deploy manual:

```bash
# Via SSM
aws ssm send-command \
  --instance-ids i-xxxxxxxx \
  --document-name "AWS-RunShellScript" \
  --parameters commands=["/opt/rentafit/deploy.sh"]

# Ou conectando na instancia
aws ssm start-session --target i-xxxxxxxx
sudo /opt/rentafit/deploy.sh
```

---

## Troubleshooting

### Instancia nao inicia

```bash
# Verificar logs UserData
aws ec2 get-console-output --instance-id i-xxxxxxxx

# Verificar status checks
aws ec2 describe-instance-status --instance-id i-xxxxxxxx
```

### Aplicacao nao responde

```bash
# Conectar via SSM e verificar
aws ssm start-session --target i-xxxxxxxx
docker ps
docker-compose logs app

# Restart manual
cd /opt/rentafit && docker-compose restart
```

### Spot Instance interrompida

Se a Spot Instance for interrompida:
1. Ela entra em estado `stopped` (nao terminada)
2. O Lambda de start (ou manual) reinicia ela normalmente
3. Dados do PostgreSQL persistem no EBS

Para evitar interrupcoes, considerar:
- EC2 On-Demand (mais caro, ~$8/mês)
- Spot Capacity Rebalancing (avancado)

---

## Custos Estimados (us-east-1)

| Componente | Custo |
|------------|-------|
| EC2 t3.small Spot (160h/mês) | $0.51 |
| EBS gp3 20GB | $1.60 |
| ECR | $0.10 |
| S3 | $0.02 |
| Lambda | $0.00 (free tier) |
| Data Transfer | $0.50 |
| **Total** | **~$2.75/mês** |

---

## Rollback para ECS

Se necessario voltar ao ECS:

```bash
# 1. Parar/deletar stacks EC2
aws cloudformation delete-stack --stack-name rentafit-hk-scheduler
aws cloudformation delete-stack --stack-name rentafit-hk-ec2

# 2. Reverter workflow GitHub
# Editar .github/workflows/ci-cd-pre.yml, restaurar secao deploy-hk anterior

# 3. ECS ja esta pronto (se a stack ecs-hk.yaml ainda existir)
aws ecs update-service \
  --cluster Rentafit-HK-Cluster \
  --service rentafit-hk-service \
  --desired-count 1
```

---

## Referencias

- [AWS EC2 Spot Instances](https://aws.amazon.com/ec2/spot/)
- [AWS Systems Manager Session Manager](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager.html)
- [AWS EventBridge Scheduler](https://docs.aws.amazon.com/eventbridge/latest/userguide/scheduler.html)

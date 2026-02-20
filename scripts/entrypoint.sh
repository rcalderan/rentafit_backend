#!/bin/bash
set -e

# Se ambiente for HK, inicializa e sobe o Postgres interno
if [ "$SPRING_PROFILES_ACTIVE" = "hk" ]; then
  echo "--- Configurando Ambiente de Homologação (HK) ---"

  # Path padrão do postgresql-14 instalado via apt no Ubuntu 22.04
  DATA_DIR="/var/lib/postgresql/14/main"

  # Garante permissões corretas no diretório de dados
  chown -R postgres:postgres /var/lib/postgresql
  chmod 700 "$DATA_DIR"

  # Inicia o cluster usando pg_ctlcluster (respeita configuração padrão do apt)
  echo "Iniciando PostgreSQL..."
  pg_ctlcluster 14 main start

  # Aguarda o Postgres aceitar conexões
  until sudo -u postgres pg_isready -q; do
    echo "Aguardando PostgreSQL ficar pronto..."
    sleep 1
  done

  # Garante que o banco 'rentafit' existe e configura senha
  sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname = 'rentafit'" | grep -q 1 || \
    sudo -u postgres psql -c "CREATE DATABASE rentafit;"

  sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD '$DB_PASSWORD';"

  echo "PostgreSQL pronto para uso."
fi

# Inicia a aplicação Spring Boot
echo "Iniciando aplicação Spring Boot: $SPRING_APPLICATION_NAME..."
exec java -jar app.jar


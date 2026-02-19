#!/bin/bash
set -e

# Se ambiente for HK, inicializa e sobe o Postgres interno
if [ "$SPRING_PROFILES_ACTIVE" = "hk" ]; then
  echo "--- Configurando Ambiente de Homologao (HK) ---"

  # O diretrio /var/lib/postgresql/data  onde o EFS estar montado
  DATA_DIR="/var/lib/postgresql/data"

  # Ajusta permisses (necessrio pois o EFS pode vir com root/root)
  chown -R postgres:postgres $DATA_DIR
  chmod 700 $DATA_DIR

  # Inicializa o banco se estiver vazio
  if [ -z "$(ls -A $DATA_DIR)" ]; then
    echo "Inicializando repositrio de dados do Postgres no EFS..."
    sudo -u postgres /usr/lib/postgresql/14/bin/initdb -D $DATA_DIR
  fi

  # Inicia o servio
  echo "Iniciando PostgreSQL..."
  service postgresql start

  # Garante que o banco 'rentafit' existe e configura senha
  sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname = 'rentafit'" | grep -q 1 || \
    sudo -u postgres psql -c "CREATE DATABASE rentafit;"

  sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD '$DB_PASSWORD';"

  echo "PostgreSQL pronto para uso."
fi

# Inicia a aplicao Java
echo "Iniciando aplicao Spring Boot: $SPRING_APPLICATION_NAME..."
exec java -jar app.jar


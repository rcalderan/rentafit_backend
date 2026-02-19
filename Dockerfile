# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies for better caching
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM ubuntu:22.04

# Evita perguntas durante instalao
ENV DEBIAN_FRONTEND=noninteractive

# Instala Java 21, Postgres 14 e utilitrios
RUN apt-get update && \
    apt-get install -y openjdk-21-jre postgresql-14 curl sudo && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copia o artefato do build
COPY --from=build /app/target/Rentafit-*.jar app.jar
# Copia o script de entrypoint
COPY scripts/entrypoint.sh .
RUN chmod +x entrypoint.sh

# Expe portas: 8080 (App) e 5432 (Postgres se HK)
EXPOSE 8080 5432

# Variavel padrao
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["./entrypoint.sh"]


package br.com.rentafit.migration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propriedades de configuração para migração do MongoDB → PostgreSQL
 * Carregadas de application.properties com prefixo "migration"
 */
@Data
@Component
@ConfigurationProperties(prefix = "migration")
public class MigrationProperties {

    /**
     * Habilita/desabilita todo o job de migração
     * Default: false (deve ser ativado explicitamente)
     */
    private Boolean enabled = false;

    /**
     * Configurações de batch
     */
    private BatchConfig batch = new BatchConfig();

    /**
     * Configuração de leitura de arquivos BSON
     */
    private BsonConfig bson = new BsonConfig();

    /**
     * Quais entidades migrar: "cliente", "funcionario", "cliente,funcionario"
     * Default: "cliente,funcionario"
     */
    private String entities = "cliente,funcionario";

    /**
     * Pular validações pré-migração
     * Default: false
     */
    private Boolean skipValidation = false;

    /**
     * Pular limpeza pós-migração
     * Default: false
     */
    private Boolean skipCleanup = false;

    /**
     * Habilitar auditoria de migração em tabela
     * Default: true
     */
    private Boolean auditEnabled = true;

    @Data
    public static class BatchConfig {
        /**
         * Tamanho do chunk (lote) processado por vez
         * Default: 100
         */
        private Integer chunkSize = 100;

        /**
         * Número máximo de tentativas para cada registro
         * Default: 3
         */
        private Integer maxRetries = 3;

        /**
         * Tempo de espera antes de retry em ms
         * Default: 1000
         */
        private Integer retryBackoffMs = 1000;

        /**
         * Número máximo de erros a permitir antes de parar
         * Default: 10
         */
        private Integer skipLimit = 10;
    }

    @Data
    public static class BsonConfig {
        /**
         * Caminho base para os arquivos BSON
         * Default: ".legado/noivabd"
         */
        private String basePath = ".legado/noivabd";

        /**
         * Nome do arquivo BSON para clientes
         * Default: "cliente.bson"
         */
        private String clienteFileName = "cliente.bson";

        /**
         * Nome do arquivo BSON para funcionários
         * Default: "funcionario.bson"
         */
        private String funcionarioFileName = "funcionario.bson";
    }

    /**
     * Helper para verificar se uma entidade deve ser migrada
     */
    public boolean shouldMigrateCliente() {
        return entities != null && entities.contains("cliente");
    }

    /**
     * Helper para verificar se uma entidade deve ser migrada
     */
    public boolean shouldMigrateFuncionario() {
        return entities != null && entities.contains("funcionario");
    }
}


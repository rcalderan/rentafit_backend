package br.com.rentafit.migration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Propriedades de configuração para migração do MongoDB → PostgreSQL via Python.
 */
@Data
@Component
@ConfigurationProperties(prefix = "migration")
public class MigrationProperties {

    /**
     * Habilita/desabilita os endpoints de migração.
     * Default: false (deve ser ativado explicitamente).
     */
    private Boolean enabled = false;

    /**
     * Caminho base para os arquivos BSON de legado e sessões.
     * Default: .legado/noivabd
     */
    private String bsonBasePath = ".legado/noivabd";

    /**
     * Caminho onde o Python gerará os CSVs e report.json por sessão.
     * Default: migration/output
     */
    private String outputPath = "migration/output";

    /**
     * Executável Python a ser usado.
     * Default: python
     */
    private String pythonExecutable = "python3";

    /**
     * Caminho para o script Python principal de migração.
     * Default: migration/scripts/migrate.py
     */
    private String scriptPath = "/app/migration/scripts/migrate.py";

    /**
     * Pular validações pré-migração.
     * Default: false
     */
    private Boolean skipValidation = false;

    /**
     * Pular limpeza pós-migração.
     * Default: false
     */
    private Boolean skipCleanup = false;

    /**
     * Habilitar auditoria de migração em tabela.
     * Default: true
     */
    private Boolean auditEnabled = true;

    public Path resolveBsonBasePath() {
        return Path.of(bsonBasePath).toAbsolutePath().normalize();
    }

    public Path resolveOutputPath() {
        return Path.of(outputPath).toAbsolutePath().normalize();
    }

    public Path resolveScriptPath() {
        return Path.of(scriptPath).toAbsolutePath().normalize();
    }
}

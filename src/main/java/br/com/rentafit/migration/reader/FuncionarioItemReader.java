package br.com.rentafit.migration.reader;

import br.com.rentafit.migration.dto.FuncionarioDocument;
import br.com.rentafit.migration.config.MigrationProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.stereotype.Component;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemReader que lê dados de funcionários do arquivo funcionario.bson
 *
 * Para produção, seria necessário implementar parsing real de BSON.
 * Esta versão é um stub que demonstra a arquitetura.
 */
@Component
public class FuncionarioItemReader implements ItemReader<FuncionarioDocument> {

    private static final Logger log = LoggerFactory.getLogger(FuncionarioItemReader.class);

    private final MigrationProperties migrationProperties;
    private List<FuncionarioDocument> documentos;
    private int currentIndex = 0;

    public FuncionarioItemReader(MigrationProperties migrationProperties) {
        this.migrationProperties = migrationProperties;
    }

    @Override
    public FuncionarioDocument read()
        throws UnexpectedInputException, ParseException, NonTransientResourceException {

        // Inicializar documentos na primeira leitura
        if (documentos == null) {
            documentos = loadFuncionariosFromBson();
        }

        // Retornar próximo documento ou null se fim da lista
        if (currentIndex < documentos.size()) {
            FuncionarioDocument documento = documentos.get(currentIndex);
            currentIndex++;
            return documento;
        }

        return null; // Sinaliza fim da leitura
    }

    /**
     * Carrega funcionários do arquivo BSON
     *
     * NOTA: Esta é uma implementação stub.
     * Para produção, seria necessário:
     * 1. Usar bson4jackson ou similar
     * 2. Implementar parsing real de BSON binary
     * 3. Mapear campos BSON → FuncionarioDocument
     */
    private List<FuncionarioDocument> loadFuncionariosFromBson() {
        List<FuncionarioDocument> funcionarios = new ArrayList<>();

        String bsonFilePath = migrationProperties.getBson().getBasePath()
            + File.separator
            + migrationProperties.getBson().getFuncionarioFileName();

        File file = new File(bsonFilePath);
        if (!file.exists()) {
            log.warn("BSON file not found: {}", bsonFilePath);
            return funcionarios;
        }

        try {
            log.info("Loading funcionarios from BSON file: {}", bsonFilePath);

            // TODO: Implementar parsing real de BSON
            // Por enquanto, retornar lista vazia para demonstrar arquitetura
            // Em produção: usar biblioteca BSON parser

            log.info("Loaded {} funcionario records from BSON", funcionarios.size());
        } catch (Exception e) {
            log.error("Error reading funcionario BSON file: {}", bsonFilePath, e);
            throw new ParseException("Error reading BSON file: " + bsonFilePath, e);
        }

        return funcionarios;
    }

    /**
     * Retorna exemplo de documento para testes
     * Remove este método em produção
     */
    protected FuncionarioDocument createSampleFuncionario() {
        FuncionarioDocument func = new FuncionarioDocument();
        func.setId("507f1f77bcf86cd799439022");
        func.setNome("Funcionário Exemplo");
        func.setDocumento("987.654.321-00");
        func.setEmail("funcionario@rentafit.com");
        func.setUsuario("func.exemplo");
        func.setSenha("senha123"); // Será hasheada durante migração
        func.setIniciais("FE");
        func.setNivelAcesso(2); // Manager
        func.setAtivo(true);
        func.setDataCriacao(OffsetDateTime.now());
        func.setDataAtualizacao(OffsetDateTime.now());

        return func;
    }
}


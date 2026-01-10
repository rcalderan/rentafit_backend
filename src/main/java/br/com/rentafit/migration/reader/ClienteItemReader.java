package br.com.rentafit.migration.reader;

import br.com.rentafit.migration.dto.ClienteDocument;
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
 * ItemReader que lê dados de clientes do arquivo cliente.bson
 *
 * Para produção, seria necessário implementar parsing real de BSON.
 * Esta versão é um stub que demonstra a arquitetura.
 */
@Component
public class ClienteItemReader implements ItemReader<ClienteDocument> {

    private static final Logger log = LoggerFactory.getLogger(ClienteItemReader.class);

    private final MigrationProperties migrationProperties;
    private List<ClienteDocument> documentos;
    private int currentIndex = 0;

    public ClienteItemReader(MigrationProperties migrationProperties) {
        this.migrationProperties = migrationProperties;
    }

    @Override
    public ClienteDocument read()
        throws UnexpectedInputException, ParseException, NonTransientResourceException {

        // Inicializar documentos na primeira leitura
        if (documentos == null) {
            documentos = loadClientesFromBson();
        }

        // Retornar próximo documento ou null se fim da lista
        if (currentIndex < documentos.size()) {
            ClienteDocument documento = documentos.get(currentIndex);
            currentIndex++;
            return documento;
        }

        return null; // Sinaliza fim da leitura
    }

    /**
     * Carrega clientes do arquivo BSON
     *
     * NOTA: Esta é uma implementação stub.
     * Para produção, seria necessário:
     * 1. Usar bson4jackson ou similar
     * 2. Implementar parsing real de BSON binary
     * 3. Mapear campos BSON → ClienteDocument
     */
    private List<ClienteDocument> loadClientesFromBson() {
        List<ClienteDocument> clientes = new ArrayList<>();

        String bsonFilePath = migrationProperties.getBson().getBasePath()
            + File.separator
            + migrationProperties.getBson().getClienteFileName();

        File file = new File(bsonFilePath);
        if (!file.exists()) {
            log.warn("BSON file not found: {}", bsonFilePath);
            return clientes;
        }

        try {
            log.info("Loading clientes from BSON file: {}", bsonFilePath);

            // TODO: Implementar parsing real de BSON
            // Por enquanto, retornar lista vazia para demonstrar arquitetura
            // Em produção: usar biblioteca BSON parser

            log.info("Loaded {} cliente records from BSON", clientes.size());
        } catch (Exception e) {
            log.error("Error reading cliente BSON file: {}", bsonFilePath, e);
            throw new ParseException("Error reading BSON file: " + bsonFilePath, e);
        }

        return clientes;
    }

    /**
     * Retorna exemplo de documento para testes
     * Remove este método em produção
     */
    protected ClienteDocument createSampleCliente() {
        ClienteDocument cliente = new ClienteDocument();
        cliente.setId("507f1f77bcf86cd799439011");
        cliente.setNome("Cliente Exemplo");
        cliente.setDocumento("123.456.789-00");
        cliente.setEmail("cliente@example.com");
        cliente.setTelefones(List.of("(11) 99999-8888"));
        cliente.setAutenticado(false);
        cliente.setDataCriacao(OffsetDateTime.now());
        cliente.setDataAtualizacao(OffsetDateTime.now());

        ClienteDocument.EnderecoData endereco = new ClienteDocument.EnderecoData();
        endereco.setRua("Rua das Flores");
        endereco.setNumero("123");
        endereco.setComplemento("Apto 4B");
        endereco.setCidade("São Paulo");
        endereco.setEstado("SP");
        endereco.setCep("01311-100");
        cliente.setEndereco(endereco);

        return cliente;
    }
}


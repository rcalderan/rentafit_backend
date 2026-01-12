package br.com.rentafit.migration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Representa um documento de Cliente do MongoDB legado (cliente.bson)
 * Esta é uma entidade intermediária usada apenas durante a migração
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteDocument {

    private String id;              // ObjectId do MongoDB (String)
    private String nome;
    private String documento;       // CPF/CNPJ
    private String email;
    private List<String> telefones;

    private EnderecoData endereco;

    private String notas;
    private String criadoPor;       // ObjectId do funcionário que criou
    private Boolean autenticado;

    private OffsetDateTime dataCriacao;
    private OffsetDateTime dataAtualizacao;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnderecoData {
        private String rua;
        private String numero;
        private String complemento;
        private String bairro;
        private String cidade;
        private String estado;
        private String cep;
    }
}


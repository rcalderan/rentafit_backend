package br.com.rentafit.migration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

/**
 * Representa um documento de Funcionário do MongoDB legado (funcionario.bson)
 * Esta é uma entidade intermediária usada apenas durante a migração
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuncionarioDocument {

    private String id;              // ObjectId do MongoDB (String)
    private String nome;
    private String documento;       // CPF/CNPJ
    private String email;
    private String usuario;         // username
    private String senha;           // será processada com BCrypt
    private String iniciais;
    private Integer nivelAcesso;    // 1=Admin, 2=Manager, 3=Employee
    private Boolean ativo;

    private OffsetDateTime dataCriacao;
    private OffsetDateTime dataAtualizacao;
}


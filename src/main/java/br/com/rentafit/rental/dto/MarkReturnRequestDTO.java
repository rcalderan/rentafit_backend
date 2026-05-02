package br.com.rentafit.rental.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request para marcar itens/acessórios como devolvidos.
 *
 * <p>Exemplo: {"returnerName": "João (amigo da noiva)", "entries": [...]}</p>
 */
public record MarkReturnRequestDTO(

        @NotBlank(message = "returnerName é obrigatório")
        String returnerName,

        @NotEmpty(message = "entries não pode ser vazio")
        @Valid
        List<ReturnEntryDTO> entries
) {}

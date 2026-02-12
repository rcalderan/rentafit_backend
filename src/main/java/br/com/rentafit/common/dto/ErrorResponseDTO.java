package br.com.rentafit.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response structure")
public class ErrorResponseDTO {

    @Schema(description = "Timestamp when the error occurred", example = "2026-02-10T08:29:37")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "404")
    private Integer status;

    @Schema(description = "Error type", example = "Not Found")
    private String error;

    @Schema(description = "Error message", example = "Category not found with id: bc01f290-fdaf-4171-9e41-a742fa3c129c")
    private String message;

    @Schema(description = "Request path", example = "/api/v1/categories/bc01f290-fdaf-4171-9e41-a742fa3c129c")
    private String path;
}


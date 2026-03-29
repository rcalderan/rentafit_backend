package br.com.rentafit.rental.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de saída detalhado de um contrato de locação.
 *
 * <p>O campo {@code warnings} é omitido do JSON quando não há alertas,
 * seguindo o padrão {@code @JsonInclude(NON_NULL)} do ErrorResponse.</p>
 */
@Builder
public record RentalContractDetailsDTO(
        UUID id,
        String legacyId,
        Integer contractType,
        Integer status,
        String statusDescription,

        // Snapshot do cliente (imutável após criação)
        UUID customerId,
        String customerName,
        String customerDocument,

        // Referências de funcionários
        UUID createdByEmployeeId,
        UUID returnedByEmployeeId,

        // Datas
        LocalDate pickupDate,
        LocalDate eventDate,
        LocalDate returnDate,
        LocalDate actualReturnDate,

        // Estado
        Boolean returned,
        String notes,
        OffsetDateTime createdAt,

        // Valores calculados
        BigDecimal totalValue,
        BigDecimal paidValue,
        BigDecimal remainingValue,

        // Itens e pagamentos
        List<RentalContractItemDetailsDTO> items,
        List<RentalPaymentDetailsDTO> payments,

        // Alertas não-bloqueantes (omitido quando null/vazio)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<String> warnings
) {}


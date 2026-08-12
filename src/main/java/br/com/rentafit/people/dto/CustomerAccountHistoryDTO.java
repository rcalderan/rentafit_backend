package br.com.rentafit.people.dto;

import br.com.rentafit.rental.dto.RentalContractSummaryDTO;
import br.com.rentafit.sales.dto.SalesOrderSummaryDTO;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Aggregated view of a customer's own rental and sales history.
 * Used by the self-service /account endpoint.
 */
public record CustomerAccountHistoryDTO(
        Page<RentalContractSummaryDTO> rentals,
        List<SalesOrderSummaryDTO> orders
) {}

package br.com.rentafit.billing.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Representa os tributos incidentes sobre o serviço conforme a NT 004 (RTC).
 * Inclui os novos tributos da Reforma Tributária: IBS e CBS.
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxInfo {

    private BigDecimal ibsRate;
    private BigDecimal ibsValue;

    private BigDecimal cbsRate;
    private BigDecimal cbsValue;

    private BigDecimal isqnRate;
    private BigDecimal isqnValue;

    private BigDecimal totalTaxValue;
}

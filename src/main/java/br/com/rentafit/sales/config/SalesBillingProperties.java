package br.com.rentafit.sales.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propriedades de configuração para emissão de NFS-e em vendas.
 *
 * <pre>
 * rentafit:
 *   billing:
 *     auto-emit-on-payment: false
 *     nbs-code: "931230000"
 *     service-description: "Locação de trajes e acessórios de vestuário"
 *     city-code: "3548906"
 * </pre>
 *
 * <p>Quando {@code autoEmitOnPayment = true}, a NFS-e é emitida automaticamente
 * ao transitar para PAID. Quando false, fica como PENDING_EMISSION para emissão manual.</p>
 */
@Component
@ConfigurationProperties(prefix = "rentafit.billing")
@Getter
@Setter
public class SalesBillingProperties {

    /** Se true, emite NFS-e automaticamente ao atingir status PAID. */
    private boolean autoEmitOnPayment = false;

    /** Código NBS para serviço de locação de trajes (padrão: 931230000). */
    private String nbsCode = "931230000";

    /** Descrição do serviço prestado (padrão: locação de trajes). */
    private String serviceDescription = "Locação de trajes e acessórios de vestuário";

    /** Código IBGE do município de prestação (padrão: São Carlos/SP = 3548906). */
    private String cityCode = "3548906";
}

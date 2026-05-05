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
}

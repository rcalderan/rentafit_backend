package br.com.rentafit.billing.domain.saocarlos;

import br.com.rentafit.people.domain.Customer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "saocarlos_nfse")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaoCarlosNfse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private SaoCarlosLoteRps lote;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rps_id")
    private SaoCarlosRps rps;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "numero_nfse", nullable = false)
    private Long numeroNfse;

    @Column(name = "codigo_verificacao", nullable = false, length = 9)
    private String codigoVerificacao;

    @Column(name = "data_emissao", nullable = false)
    private LocalDateTime dataEmissao;

    @Column(name = "numero_rps_substituido")
    private Long numeroRpsSubstituido;

    @Column(name = "serie_rps_substituido", length = 5)
    private String serieRpsSubstituido;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusNfse status;

    @Column(name = "valor_servicos", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorServicos;

    @Column(name = "valor_liquido", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorLiquido;

    @Column(name = "base_calculo", precision = 15, scale = 2)
    private BigDecimal baseCalculo;

    @Column(name = "aliquota", precision = 5, scale = 4)
    private BigDecimal aliquota;

    @Column(name = "valor_iss", precision = 15, scale = 2)
    private BigDecimal valorIss;

    @Column(name = "xml_nfse", columnDefinition = "TEXT")
    private String xmlNfse;

    @Column(name = "link_visualizacao")
    private String linkVisualizacao;

    @Column(name = "data_cancelamento")
    private LocalDateTime dataCancelamento;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public enum StatusNfse {
        AUTORIZADA,
        CANCELADA,
        SUBSTITUIDA
    }
}

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
@Table(name = "saocarlos_rps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaoCarlosRps {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private SaoCarlosLoteRps lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "numero_rps", nullable = false)
    private Long numeroRps;

    @Column(name = "serie_rps", nullable = false, length = 5)
    private String serieRps;

    @Column(name = "tipo_rps", nullable = false)
    private Integer tipoRps;

    @Column(name = "data_emissao", nullable = false)
    private LocalDateTime dataEmissao;

    @Column(name = "natureza_operacao", nullable = false)
    private Integer naturezaOperacao;

    @Column(name = "regime_especial_tributacao")
    private Integer regimeEspecialTributacao;

    @Column(name = "simples_nacional", nullable = false)
    private Integer simplesNacional;

    @Column(name = "incentivador_cultural", nullable = false)
    private Integer incentivadorCultural;

    @Column(name = "status_rps", nullable = false)
    private Integer statusRps;

    @Column(name = "valor_servicos", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorServicos;

    @Column(name = "valor_deducoes", precision = 15, scale = 2)
    private BigDecimal valorDeducoes;

    @Column(name = "valor_pis", precision = 15, scale = 2)
    private BigDecimal valorPis;

    @Column(name = "valor_cofins", precision = 15, scale = 2)
    private BigDecimal valorCofins;

    @Column(name = "valor_inss", precision = 15, scale = 2)
    private BigDecimal valorInss;

    @Column(name = "valor_ir", precision = 15, scale = 2)
    private BigDecimal valorIr;

    @Column(name = "valor_csll", precision = 15, scale = 2)
    private BigDecimal valorCsll;

    @Column(name = "valor_iss", precision = 15, scale = 2)
    private BigDecimal valorIss;

    @Column(name = "valor_iss_retido", precision = 15, scale = 2)
    private BigDecimal valorIssRetido;

    @Column(name = "valor_outras_retencoes", precision = 15, scale = 2)
    private BigDecimal valorOutrasRetencoes;

    @Column(name = "base_calculo", precision = 15, scale = 2)
    private BigDecimal baseCalculo;

    @Column(name = "aliquota", precision = 5, scale = 4)
    private BigDecimal aliquota;

    @Column(name = "valor_liquido_nfse", precision = 15, scale = 2)
    private BigDecimal valorLiquidoNfse;

    @Column(name = "desconto_incondicionado", precision = 15, scale = 2)
    private BigDecimal descontoIncondicionado;

    @Column(name = "desconto_condicionado", precision = 15, scale = 2)
    private BigDecimal descontoCondicionado;

    @Column(name = "item_lista_servico", nullable = false, length = 5)
    private String itemListaServico;

    @Column(name = "codigo_cnae", length = 7)
    private String codigoCnae;

    @Column(name = "codigo_tributacao_municipio", length = 20)
    private String codigoTributacaoMunicipio;

    @Column(name = "discriminacao", columnDefinition = "TEXT", nullable = false)
    private String discriminacao;

    @Column(name = "codigo_municipio", nullable = false)
    private Integer codigoMunicipio;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}

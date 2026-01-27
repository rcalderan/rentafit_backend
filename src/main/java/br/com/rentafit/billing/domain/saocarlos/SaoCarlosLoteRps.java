package br.com.rentafit.billing.domain.saocarlos;

import br.com.rentafit.people.domain.Customer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "saocarlos_lote_rps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaoCarlosLoteRps {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "numero_lote", unique = true, nullable = false)
    private String numeroLote;

    @Column(name = "protocolo")
    private String protocolo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "cnpj_prestador", nullable = false, length = 14)
    private String cnpjPrestador;

    @Column(name = "inscricao_municipal_prestador", length = 15)
    private String inscricaoMunicipalPrestador;

    @Column(name = "quantidade_rps", nullable = false)
    private Integer quantidadeRps;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false)
    private SituacaoLote situacao;

    @Column(name = "data_envio")
    private OffsetDateTime dataEnvio;

    @Column(name = "data_recebimento")
    private OffsetDateTime dataRecebimento;

    @Column(name = "xml_enviado", columnDefinition = "TEXT")
    private String xmlEnviado;

    @Column(name = "xml_resposta", columnDefinition = "TEXT")
    private String xmlResposta;

    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public enum SituacaoLote {
        PENDENTE,           // Aguardando envio
        ENVIADO,            // Enviado, aguardando processamento
        PROCESSADO,         // Processado com sucesso
        PROCESSADO_ERRO,    // Processado com erro
        ERRO_ENVIO,         // Erro no envio
        CANCELADO           // Cancelado
    }
}

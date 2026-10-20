package br.com.rentafit.sector.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Setor físico de atendimento/caixa/retirada pertencente a um estabelecimento
 * (Matriz ou Filial, identificado pelo CNPJ de 14 dígitos).
 *
 * <p>O vínculo com o CNPJ é mantido por valor ({@code issuerCnpj}) e não por FK,
 * pois o cadastro fiscal de estabelecimentos vive no microsserviço
 * costume-rental-nfe — mesmo padrão de desacoplamento de {@code issuer_cnpj}
 * em {@code user_accounts}.</p>
 */
@Entity
@Table(name = "sectors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /** CNPJ (14 dígitos) da Matriz/Filial à qual o setor pertence. */
    @Column(name = "issuer_cnpj", nullable = false, length = 14)
    private String issuerCnpj;

    /** Nome de exibição do estabelecimento (nome fantasia/razão social no cadastro do setor). */
    @Column(name = "branch_name")
    private String branchName;

    @Column(nullable = false, length = 100)
    private String name;

    /** Sigla curta do setor (ex.: BALCAO, DEPOSITO) — única por estabelecimento. */
    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Setor padrão sugerido para terminais que ainda não foram configurados. */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}

package br.com.rentafit.auth.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "installation_state")
@Getter
@Setter
public class InstallationState {
    @Id
    private Integer id;
    private String status;
    private UUID bootstrapAccountId;
    private String bootstrapUsername;
    private UUID completedByAccountId;
    private OffsetDateTime completedAt;
}

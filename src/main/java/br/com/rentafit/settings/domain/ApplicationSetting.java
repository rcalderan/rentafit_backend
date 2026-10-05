package br.com.rentafit.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "application_settings")
@Getter
@Setter
@NoArgsConstructor
public class ApplicationSetting {
    @Id
    @Column(name = "setting_key")
    private String key;
    @Column(name = "setting_value", nullable = false)
    private String value;
    private UUID updatedBy;
    private OffsetDateTime updatedAt;
}

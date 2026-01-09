package br.com.rentafit.people.domain;

import br.com.rentafit.common.security.DatabaseEncryptionConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "people")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Base entity for all people in the system")
public abstract class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Unique identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Column(name = "legacy_id", unique = true)
    @Schema(description = "ID from the legacy MongoDB system", example = "101")
    private Integer legacyId;

    @NotBlank
    @Column(nullable = false)
    @Schema(description = "Full name", example = "John Doe")
    private String name;

    @Column(unique = true)
    @Convert(converter = DatabaseEncryptionConverter.class)
    @Schema(description = "Document (CPF/RG/CNPJ)", example = "123.456.789-00")
    private String document;

    @Email
    @Column(unique = true)
    @Schema(description = "Email address", example = "john.doe@example.com")
    private String email;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Creation timestamp with timezone")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Last update timestamp with timezone")
    private OffsetDateTime updatedAt;
}

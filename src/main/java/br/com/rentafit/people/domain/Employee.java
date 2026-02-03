package br.com.rentafit.people.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Employee entity extending Person")
public class Employee extends Person {

    @Column(unique = true)
    @Schema(description = "Employee initials", example = "JD")
    private String initials;

    @Column(name = "role_level")
    @Schema(description = "Access level or role rank", example = "1")
    private Integer roleLevel;
}

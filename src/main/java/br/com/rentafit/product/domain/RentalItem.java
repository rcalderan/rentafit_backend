package br.com.rentafit.product.domain;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.product.domain.enums.ProductCondition;
import br.com.rentafit.product.domain.enums.ProductStatus;
import br.com.rentafit.product.dto.rental.RentalItemDetailsDTO;
import br.com.rentafit.product.dto.rental.RentalItemUpdateDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_items")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RentalItem extends Product {

    @Column(name = "legacy_id", unique = true)
    private Integer legacyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes = "";

    @Enumerated(EnumType.STRING)
    private ProductCondition condition;

    private LocalDateTime lastRentalDate;

    @Builder.Default
    private Integer rentalCount = 0;

    private LocalDate maintenanceDueDate;

    @PrePersist
    @SuppressWarnings("unused")
    protected void onRentalCreate() {
        if (this.status == null) {
            this.status = ProductStatus.AVAILABLE;
        }
        if (this.condition == null) {
            this.condition = ProductCondition.NEW;
        }
    }

    @Override
    protected void validateSpecificFields() {
//         if (this.condition == null) {
//            throw new IllegalArgumentException("Condition is required for RentalItem");
//        }
    }

    public RentalItemDetailsDTO toDTO() {
        return RentalItemDetailsDTO.builder()
                .id(this.getId())
                .name(this.getName())
                .categoryName(this.getCategory().getDisplayName())
                .size(this.getSize())
                .color(this.getColor())
                .brand(this.getBrand())
                .value(this.getValue())
                .description(this.getDescription())
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getUpdatedAt())
                .legacyId(this.getLegacyId())
                .status(this.getStatus().name())
                .notes(this.getNotes())
                .condition(this.getCondition() != null ? this.getCondition().name(): null)
                .lastRentalDate(this.getLastRentalDate())
                .rentalCount(this.getRentalCount())
                .build();
    }


    @Override
    public void updateFromDTO(Object objectDTO) {
        if (objectDTO instanceof RentalItemUpdateDTO dto) {

            if (dto.name() != null && !dto.name().equals(this.getName())) {
                this.setName(dto.name());
            }

            if (dto.size() != null && !dto.size().equals(this.getSize())) {
                this.setSize(dto.size());
            }

            if (dto.color() != null && !dto.color().equals(this.getColor())) {
                this.setColor(dto.color());
            }

            if (dto.brand() != null && !dto.brand().equals(this.getBrand())) {
                this.setBrand(dto.brand());
            }

            if (dto.value() != null && !dto.value().equals(this.getValue())) {
                this.setValue(dto.value());
            }

            if (dto.description() != null && !dto.description().equals(this.getDescription())) {
                this.setDescription(dto.description());
            }

            if (dto.status() != null && !dto.status().equals(this.getStatus() != null ? this.getStatus().name() : null)) {
                this.setStatus(ProductStatus.valueOf(dto.status()));
            }

            if (dto.notes() != null && !dto.notes().equals(this.getNotes())) {
                this.setNotes(dto.notes());
            }

            if (dto.condition() != null && !dto.condition().equals(this.getCondition() != null ? this.getCondition().name() : null)) {
                this.setCondition(ProductCondition.valueOf(dto.condition()));
            }

            this.setUpdatedAt(LocalDateTime.now());
        } else {
            throw new ValidationException("DTO inválido para atualização de RentalItem");
        }
    }
}

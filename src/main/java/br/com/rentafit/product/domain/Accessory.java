package br.com.rentafit.product.domain;

import br.com.rentafit.product.dto.AccessoryDetailsDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "accessories")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Accessory extends Product {

    @Column(name = "legacy_id", unique = true)
    private String legacyId;

    @Column(name = "compatible_with")
    private String compatibleWith;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Stock stock;

    @PrePersist
    @SuppressWarnings("unused")
    protected void onAccessoryCreate() {
        super.onCreate();
    }

    @Override
    protected void validateSpecificFields() {
        // Validations specific to Accessory
    }

    @SuppressWarnings("unused")
    public boolean isCompatibleWith(String productCategory) {
        if (compatibleWith == null) return true;
        return compatibleWith.contains(productCategory);
    }

    @SuppressWarnings("unused")
    public boolean hasStockControl() {
        return stock != null;
    }

    public AccessoryDetailsDTO toDTO() {
        return null; // Will be implemented with AccessoryDTO
    }

    @Override
    public void updateFromDTO(Object dto) {
        if(dto instanceof AccessoryDetailsDTO accessoryDetailsDTO) {
            if (accessoryDetailsDTO.name() != null) {
                this.setName(accessoryDetailsDTO.name());
            }
            if (accessoryDetailsDTO.size() != null) {
                this.setSize(accessoryDetailsDTO.size());
            }
            if (accessoryDetailsDTO.color() != null) {
                this.setColor(accessoryDetailsDTO.color());
            }
            if (accessoryDetailsDTO.brand() != null) {
                this.setBrand(accessoryDetailsDTO.brand());
            }
            if (accessoryDetailsDTO.value() != null) {
                this.setValue(accessoryDetailsDTO.value());
            }
            if (accessoryDetailsDTO.description() != null) {
                this.setDescription(accessoryDetailsDTO.description());
            }
            if (accessoryDetailsDTO.legacyId() != null) {
                this.setLegacyId(accessoryDetailsDTO.legacyId());
            }
            if (accessoryDetailsDTO.compatibleWith() != null) {
                this.setLegacyId(accessoryDetailsDTO.compatibleWith());
            }
        } else {
            throw new IllegalArgumentException("Unsupported DTO type");
        }
    }
}

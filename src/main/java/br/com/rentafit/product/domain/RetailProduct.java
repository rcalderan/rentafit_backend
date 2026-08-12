package br.com.rentafit.product.domain;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.product.dto.retail.ProductRetailDetailsDTO;
import br.com.rentafit.product.dto.retail.ProductRetailUpdateDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "retail_products")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RetailProduct extends Product {

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(unique = true)
    private String sku;

    /** Prazo de garantia em dias (ex: 90 = 90 dias). Null = sem garantia. */
    @Column(name = "warranty_days")
    private Integer warrantyDays;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Stock stock;

    @PrePersist
    @SuppressWarnings("unused")
    protected void onRetailCreate() {
        super.onCreate();
        if (this.stock == null) {
            this.stock = new Stock();
            this.stock.setProduct(this);
        }
    }

    @Override
    protected void validateSpecificFields() {
        if (stock != null && stock.getQuantityAvailable() < 0) {
            throw new ValidationException("Estoque não pode ser negativo");
        }
    }

    @SuppressWarnings("unused")
    public boolean hasStockAvailable(Integer quantity) {
        return stock != null && stock.getQuantityAvailable() >= quantity;
    }

    public ProductRetailDetailsDTO toDTO() {
        return ProductRetailDetailsDTO.builder()
                .id(this.getId())
                .name(this.getName())
                .categoryName(this.getCategory().getDisplayName())
                .size(this.getSize())
                .color(this.getColor())
                .brand(this.getBrand())
                .value(this.getValue())
                .details(this.getDetails())
                .description(this.getDescription())
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getUpdatedAt())
                .sku(this.getSku())
                .warrantyDays(this.getWarrantyDays())
                .stock(this.getStock() != null ? this.getStock().toDTO() : null)
                .build();
    }


    @Override
    public void updateFromDTO(Object dto) {
        if(dto instanceof ProductRetailUpdateDTO productRetailDTO) {
            if (productRetailDTO.name() != null) {
                this.setName(productRetailDTO.name());
            }

            if (productRetailDTO.size() != null) {
                this.setSize(productRetailDTO.size());
            }

            if (productRetailDTO.color() != null) {
                this.setColor(productRetailDTO.color());
            }

            if (productRetailDTO.brand() != null) {
                this.setBrand(productRetailDTO.brand());
            }

            if (productRetailDTO.value() != null) {
                this.setValue(productRetailDTO.value());
            }

            if (productRetailDTO.description() != null) {
                this.setDescription(productRetailDTO.description());
            }

            if (productRetailDTO.details() != null) {
                this.setDetails(productRetailDTO.details());
            }

            if (productRetailDTO.sku() != null) {
                this.setSku(productRetailDTO.sku());
            }

            if (productRetailDTO.warrantyDays() != null) {
                this.setWarrantyDays(productRetailDTO.warrantyDays());
            }

            this.setUpdatedAt(LocalDateTime.now());

        } else {
            throw new ValidationException("DTO inválido para atualização de RentalItem");
        }
    }
}

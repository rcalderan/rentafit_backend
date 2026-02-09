package br.com.rentafit.product.domain;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.product.domain.enums.StockMovementType;
import br.com.rentafit.product.dto.StockDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stock")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    @JsonIgnore
    private Product product;

    @Column(name = "quantity_available", nullable = false)
    @Builder.Default
    private Integer quantityAvailable = 0;

    @Column(name = "quantity_reserved", nullable = false)
    @Builder.Default
    private Integer quantityReserved = 0;

    @Column(name = "quantity_total", nullable = false)
    @Builder.Default
    private Integer quantityTotal = 0;

    @Column(name = "min_stock_level", nullable = false)
    @Builder.Default
    private Integer minStockLevel = 5;

    private String location;

    @Column(name = "last_movement_date")
    private LocalDateTime lastMovementDate;

    @OneToMany(mappedBy = "stock", cascade = CascadeType.ALL)
    @Builder.Default
    private List<StockMovement> movements = new ArrayList<>();

    public void reserve(Integer quantity) {
        if (quantityAvailable < quantity) {
            throw new ValidationException("Estoque insuficiente");
        }
        this.quantityAvailable -= quantity;
        this.quantityReserved += quantity;
        this.lastMovementDate = LocalDateTime.now();
    }

    public void release(Integer quantity) {
        if (quantityReserved < quantity) {
            throw new ValidationException("Quantidade reservada insuficiente");
        }
        this.quantityReserved -= quantity;
        this.quantityAvailable += quantity;
        this.lastMovementDate = LocalDateTime.now();
    }

    public void addStock(Integer quantity) {
        this.quantityAvailable += quantity;
        this.quantityTotal += quantity;
        this.lastMovementDate = LocalDateTime.now();
    }

    public void removeStock(Integer quantity) {
        if (quantityAvailable < quantity) {
            throw new ValidationException("Estoque insuficiente");
        }
        this.quantityAvailable -= quantity;
        this.quantityTotal -= quantity;
        this.lastMovementDate = LocalDateTime.now();
    }

    public boolean isLowStock() {
        return quantityAvailable < minStockLevel;
    }

    public StockDTO toDTO(){
        return StockDTO.builder()
                .productId(this.getId())
                .quantityAvailable(this.quantityAvailable)
                .quantityReserved(this.quantityReserved)
                .quantityTotal(this.quantityTotal)
                .minStockLevel(this.minStockLevel)
                .location(this.location)
                .lastMovementDate(this.lastMovementDate)
                .build();
    }
}

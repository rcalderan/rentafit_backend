package br.com.rentafit.product.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected UUID id;

    @Column(nullable = false)
    protected String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    protected Category category;

    protected String size;
    protected String color;
    protected String brand;

    @Column(nullable = false, precision = 10, scale = 2)
    protected BigDecimal value;

    @Column(columnDefinition = "TEXT")
    protected String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    protected LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    protected LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        validateSpecificFields();
    }

    @PreUpdate
    @SuppressWarnings("unused")
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        validateSpecificFields();
    }

    public abstract void updateFromDTO(Object dto);
    protected abstract void validateSpecificFields();
}

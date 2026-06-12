package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "fixed_assets")
public class FixedAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String assetCode;

    @Column(nullable = false, length = 100)
    private String assetName;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(length = 100)
    private String specification;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal originalValue;

    @Column(nullable = false)
    private LocalDate purchaseDate;

    @Column(nullable = false)
    private Integer usefulLife;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal residualValueRate;

    @Column(length = 100)
    private String location;

    @Column(length = 50)
    private String department;

    @Column(length = 50)
    private String responsiblePerson;

    @Column(nullable = false, length = 10)
    private String assetStatus;

    @Column(precision = 14, scale = 2)
    private BigDecimal accumulatedDepreciation;

    @Column(precision = 14, scale = 2)
    private BigDecimal netBookValue;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.assetStatus == null) this.assetStatus = "在用";
        if (this.accumulatedDepreciation == null) this.accumulatedDepreciation = BigDecimal.ZERO;
        if (this.netBookValue == null) this.netBookValue = this.originalValue;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

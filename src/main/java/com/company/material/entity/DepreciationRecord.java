package com.company.material.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "depreciation_records", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"asset_id", "depreciationYear", "depreciationMonth"})
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DepreciationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private FixedAsset asset;

    @Column(nullable = false)
    private Integer depreciationYear;

    @Column(nullable = false)
    private Integer depreciationMonth;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyDepreciation;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal accumulatedDepreciation;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal netBookValue;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}

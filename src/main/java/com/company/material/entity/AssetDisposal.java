package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "asset_disposals")
public class AssetDisposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private FixedAsset asset;

    @Column(nullable = false, length = 10)
    private String disposalMethod;

    @Column(nullable = false, length = 200)
    private String disposalReason;

    @Column(precision = 14, scale = 2)
    private BigDecimal estimatedResidualIncome;

    @Column(nullable = false, length = 10)
    private String approvalStatus;

    @Column(length = 50)
    private String approvedBy;

    private LocalDate approvalDate;

    @Column(precision = 14, scale = 2)
    private BigDecimal actualDisposalIncome;

    @Column(precision = 14, scale = 2)
    private BigDecimal disposalGainLoss;

    @Column(length = 200)
    private String remark;

    private LocalDate completionDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.approvalStatus == null) this.approvalStatus = "待审批";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

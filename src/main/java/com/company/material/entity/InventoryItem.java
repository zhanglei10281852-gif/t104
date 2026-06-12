package com.company.material.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory_items")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private InventoryTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private FixedAsset asset;

    @Column(nullable = false, length = 10)
    private String inventoryResult;

    @Column(length = 200)
    private String remark;

    @Column(length = 50)
    private String inventoriedBy;

    private LocalDateTime inventoriedAt;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.inventoryResult == null) this.inventoryResult = "待盘点";
    }
}

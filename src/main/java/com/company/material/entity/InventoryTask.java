package com.company.material.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory_tasks")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InventoryTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String taskCode;

    @Column(nullable = false, length = 100)
    private String taskName;

    @Column(nullable = false, length = 10)
    private String inventoryScope;

    @Column(length = 50)
    private String department;

    @Column(length = 20)
    private String category;

    @Column(nullable = false, length = 10)
    private String taskStatus;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false, length = 50)
    private String createdBy;

    private Integer totalAssets;

    private Integer matchedCount;

    private Integer lossCount;

    private Integer surplusCount;

    @Column(length = 500)
    private String inventoryRemark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.taskStatus == null) this.taskStatus = "进行中";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

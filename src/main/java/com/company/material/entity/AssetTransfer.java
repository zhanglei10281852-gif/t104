package com.company.material.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "asset_transfers")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AssetTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private FixedAsset asset;

    @Column(nullable = false, length = 50)
    private String oldDepartment;

    @Column(nullable = false, length = 50)
    private String newDepartment;

    @Column(length = 100)
    private String oldLocation;

    @Column(length = 100)
    private String newLocation;

    @Column(length = 50)
    private String oldResponsiblePerson;

    @Column(length = 50)
    private String newResponsiblePerson;

    @Column(nullable = false)
    private LocalDate transferDate;

    @Column(nullable = false, length = 50)
    private String handledBy;

    @Column(length = 200)
    private String remark;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}

package com.company.material.repository;

import com.company.material.entity.AssetTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetTransferRepository extends JpaRepository<AssetTransfer, Long> {

    List<AssetTransfer> findByAssetIdOrderByCreatedAtDesc(Long assetId);

    Page<AssetTransfer> findByNewDepartment(String department, Pageable pageable);
}

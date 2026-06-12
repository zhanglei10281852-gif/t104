package com.company.material.repository;

import com.company.material.entity.AssetStatusChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetStatusChangeRepository extends JpaRepository<AssetStatusChange, Long> {

    List<AssetStatusChange> findByAssetIdOrderByCreatedAtDesc(Long assetId);
}

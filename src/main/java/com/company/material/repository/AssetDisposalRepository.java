package com.company.material.repository;

import com.company.material.entity.AssetDisposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetDisposalRepository extends JpaRepository<AssetDisposal, Long> {

    Optional<AssetDisposal> findByAssetIdAndApprovalStatusNot(Long assetId, String approvalStatus);

    Page<AssetDisposal> findByApprovalStatus(String approvalStatus, Pageable pageable);

    boolean existsByAssetIdAndApprovalStatusNot(Long assetId, String approvalStatus);
}

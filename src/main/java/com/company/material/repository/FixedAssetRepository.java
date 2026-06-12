package com.company.material.repository;

import com.company.material.entity.FixedAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FixedAssetRepository extends JpaRepository<FixedAsset, Long> {

    Optional<FixedAsset> findByAssetCode(String assetCode);

    boolean existsByAssetCode(String assetCode);

    Page<FixedAsset> findByCategory(String category, Pageable pageable);

    Page<FixedAsset> findByAssetStatus(String assetStatus, Pageable pageable);

    List<FixedAsset> findByAssetStatus(String assetStatus);

    Page<FixedAsset> findByDepartment(String department, Pageable pageable);

    List<FixedAsset> findByAssetStatusAndCategory(String assetStatus, String category);

    List<FixedAsset> findByAssetStatusAndDepartment(String assetStatus, String department);

    @Query("SELECT f FROM FixedAsset f WHERE f.assetStatus = '在用' " +
           "AND f.accumulatedDepreciation < f.originalValue * (1 - f.residualValueRate / 100)")
    List<FixedAsset> findAssetsForDepreciation();

    @Query("SELECT f FROM FixedAsset f WHERE f.assetName LIKE %:kw% OR f.assetCode LIKE %:kw%")
    Page<FixedAsset> search(@Param("kw") String kw, Pageable pageable);

    @Query("SELECT SUM(f.originalValue) FROM FixedAsset f")
    BigDecimal sumOriginalValue();

    @Query("SELECT SUM(f.netBookValue) FROM FixedAsset f")
    BigDecimal sumNetBookValue();

    long countByAssetStatus(String assetStatus);

    long countByCategory(String category);

    long countByDepartment(String department);

    @Query("SELECT f.category, COUNT(f), SUM(f.originalValue), SUM(f.netBookValue) " +
           "FROM FixedAsset f GROUP BY f.category")
    List<Object[]> countAndSumByCategory();

    @Query("SELECT f.department, COUNT(f), SUM(f.originalValue), SUM(f.netBookValue) " +
           "FROM FixedAsset f GROUP BY f.department")
    List<Object[]> countAndSumByDepartment();

    @Query("SELECT f FROM FixedAsset f WHERE f.assetStatus = '在用' " +
           "AND f.accumulatedDepreciation >= f.originalValue * (1 - f.residualValueRate / 100) * 0.9")
    List<FixedAsset> findNearlyFullyDepreciated();
}

package com.company.material.repository;

import com.company.material.entity.DepreciationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DepreciationRecordRepository extends JpaRepository<DepreciationRecord, Long> {

    List<DepreciationRecord> findByAssetIdOrderByDepreciationYearDescDepreciationMonthDesc(Long assetId);

    Page<DepreciationRecord> findByDepreciationYearAndDepreciationMonth(Integer year, Integer month, Pageable pageable);

    List<DepreciationRecord> findByDepreciationYearAndDepreciationMonth(Integer year, Integer month);

    Optional<DepreciationRecord> findByAssetIdAndDepreciationYearAndDepreciationMonth(
            Long assetId, Integer year, Integer month);

    boolean existsByAssetIdAndDepreciationYearAndDepreciationMonth(
            Long assetId, Integer year, Integer month);

    @Query("SELECT SUM(d.monthlyDepreciation) FROM DepreciationRecord d " +
           "WHERE d.depreciationYear = :year AND d.depreciationMonth = :month")
    BigDecimal sumMonthlyDepreciation(@Param("year") Integer year, @Param("month") Integer month);

    @Query("SELECT FUNCTION('CONCAT', d.depreciationYear, '-', LPAD(d.depreciationMonth, 2, '0')), " +
           "SUM(d.monthlyDepreciation) FROM DepreciationRecord d " +
           "WHERE d.depreciationYear >= :startYear OR (d.depreciationYear = :startYear AND d.depreciationMonth >= :startMonth) " +
           "GROUP BY d.depreciationYear, d.depreciationMonth " +
           "ORDER BY d.depreciationYear, d.depreciationMonth")
    List<Object[]> findDepreciationTrend(@Param("startYear") Integer startYear, @Param("startMonth") Integer startMonth);
}

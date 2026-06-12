package com.company.material.controller;

import com.company.material.entity.FixedAsset;
import com.company.material.repository.DepreciationRecordRepository;
import com.company.material.repository.FixedAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/fixed-assets/stats")
@RequiredArgsConstructor
public class FixedAssetStatsController {

    private final FixedAssetRepository fixedAssetRepository;
    private final DepreciationRecordRepository depreciationRecordRepository;

    @GetMapping("/overview")
    public ResponseEntity<?> getOverview() {
        BigDecimal totalOriginalValue = fixedAssetRepository.sumOriginalValue();
        BigDecimal totalNetBookValue = fixedAssetRepository.sumNetBookValue();
        long totalCount = fixedAssetRepository.count();
        long inUseCount = fixedAssetRepository.countByAssetStatus("在用");
        BigDecimal usageRate = totalCount > 0 ?
            BigDecimal.valueOf(inUseCount)
                .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")) : BigDecimal.ZERO;

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", totalCount);
        result.put("inUseCount", inUseCount);
        result.put("totalOriginalValue", totalOriginalValue != null ? totalOriginalValue : BigDecimal.ZERO);
        result.put("totalNetBookValue", totalNetBookValue != null ? totalNetBookValue : BigDecimal.ZERO);
        result.put("usageRate", usageRate);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/by-category")
    public ResponseEntity<?> getByCategory() {
        List<Object[]> raw = fixedAssetRepository.countAndSumByCategory();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> item = new HashMap<>();
            item.put("category", row[0]);
            item.put("count", row[1]);
            item.put("totalOriginalValue", row[2] != null ? row[2] : BigDecimal.ZERO);
            item.put("totalNetBookValue", row[3] != null ? row[3] : BigDecimal.ZERO);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/by-department")
    public ResponseEntity<?> getByDepartment() {
        List<Object[]> raw = fixedAssetRepository.countAndSumByDepartment();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> item = new HashMap<>();
            item.put("department", row[0] != null ? row[0] : "未分配");
            item.put("count", row[1]);
            item.put("totalOriginalValue", row[2] != null ? row[2] : BigDecimal.ZERO);
            item.put("totalNetBookValue", row[3] != null ? row[3] : BigDecimal.ZERO);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/monthly-depreciation")
    public ResponseEntity<?> getMonthlyDepreciation(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (year == null || month == null) {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }
        BigDecimal total = depreciationRecordRepository.sumMonthlyDepreciation(year, month);
        return ResponseEntity.ok(Map.of(
            "year", year,
            "month", month,
            "totalDepreciation", total != null ? total : BigDecimal.ZERO
        ));
    }

    @GetMapping("/depreciation-trend")
    public ResponseEntity<?> getDepreciationTrend(
            @RequestParam(defaultValue = "6") int months) {
        LocalDate now = LocalDate.now();
        LocalDate start = now.minusMonths(months - 1);
        List<Object[]> raw = depreciationRecordRepository.findDepreciationTrend(
            start.getYear(), start.getMonthValue());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> item = new HashMap<>();
            item.put("period", row[0]);
            item.put("amount", row[1] != null ? row[1] : BigDecimal.ZERO);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/nearly-fully-depreciated")
    public ResponseEntity<?> getNearlyFullyDepreciated() {
        List<FixedAsset> assets = fixedAssetRepository.findNearlyFullyDepreciated();
        List<Map<String, Object>> result = new ArrayList<>();
        for (FixedAsset asset : assets) {
            BigDecimal maxDepreciation = asset.getOriginalValue().multiply(
                BigDecimal.ONE.subtract(asset.getResidualValueRate().divide(new BigDecimal("100")))
            );
            BigDecimal remaining = maxDepreciation.subtract(asset.getAccumulatedDepreciation());
            BigDecimal monthlyDepr = asset.getOriginalValue().multiply(
                BigDecimal.ONE.subtract(asset.getResidualValueRate().divide(new BigDecimal("100")))
            ).divide(new BigDecimal(asset.getUsefulLife() * 12), 2, RoundingMode.HALF_UP);
            int remainingMonths = monthlyDepr.compareTo(BigDecimal.ZERO) > 0 ?
                remaining.divide(monthlyDepr, 0, RoundingMode.CEILING).intValue() : 0;

            Map<String, Object> item = new HashMap<>();
            item.put("id", asset.getId());
            item.put("assetCode", asset.getAssetCode());
            item.put("assetName", asset.getAssetName());
            item.put("category", asset.getCategory());
            item.put("originalValue", asset.getOriginalValue());
            item.put("accumulatedDepreciation", asset.getAccumulatedDepreciation());
            item.put("netBookValue", asset.getNetBookValue());
            item.put("remainingDepreciation", remaining);
            item.put("remainingMonths", remainingMonths);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/usage-rate")
    public ResponseEntity<?> getUsageRate() {
        long total = fixedAssetRepository.count();
        long inUse = fixedAssetRepository.countByAssetStatus("在用");
        long idle = fixedAssetRepository.countByAssetStatus("闲置");
        long inRepair = fixedAssetRepository.countByAssetStatus("维修中");
        long scrapped = fixedAssetRepository.countByAssetStatus("报废");
        long disposed = fixedAssetRepository.countByAssetStatus("已处置");

        BigDecimal usageRate = total > 0 ?
            BigDecimal.valueOf(inUse)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")) : BigDecimal.ZERO;

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("inUse", inUse);
        result.put("idle", idle);
        result.put("inRepair", inRepair);
        result.put("scrapped", scrapped);
        result.put("disposed", disposed);
        result.put("usageRate", usageRate);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/status-distribution")
    public ResponseEntity<?> getStatusDistribution() {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] statuses = {"在用", "闲置", "维修中", "报废", "已处置"};
        for (String status : statuses) {
            long count = fixedAssetRepository.countByAssetStatus(status);
            Map<String, Object> item = new HashMap<>();
            item.put("status", status);
            item.put("count", count);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }
}

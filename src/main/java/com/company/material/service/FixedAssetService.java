package com.company.material.service;

import com.company.material.entity.*;
import com.company.material.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FixedAssetService {

    private final FixedAssetRepository fixedAssetRepository;
    private final DepreciationRecordRepository depreciationRecordRepository;
    private final AssetTransferRepository assetTransferRepository;
    private final AssetStatusChangeRepository assetStatusChangeRepository;
    private final AssetDisposalRepository assetDisposalRepository;
    private final InventoryTaskRepository inventoryTaskRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final AssetCodeGenerator assetCodeGenerator;

    private static final Set<String> VALID_CATEGORIES = Set.of(
        "生产设备", "办公设备", "运输车辆", "房屋建筑", "电子设备"
    );
    private static final Set<String> VALID_STATUSES = Set.of(
        "在用", "闲置", "维修中", "报废", "已处置"
    );
    private static final Set<String> VALID_DISPOSAL_METHODS = Set.of("报废", "出售", "捐赠");
    private static final Set<String> VALID_INVENTORY_SCOPES = Set.of("部门", "类别", "全部");
    private static final Set<String> VALID_INVENTORY_RESULTS = Set.of(
        "待盘点", "账实相符", "盘亏", "盘盈"
    );

    @Transactional
    public FixedAsset createAsset(FixedAsset asset) {
        if (!VALID_CATEGORIES.contains(asset.getCategory())) {
            throw new IllegalArgumentException("无效的资产类别");
        }
        if (asset.getResidualValueRate().compareTo(BigDecimal.ZERO) < 0 ||
            asset.getResidualValueRate().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("残值率必须在0-100之间");
        }
        if (asset.getUsefulLife() <= 0) {
            throw new IllegalArgumentException("使用年限必须大于0");
        }
        String assetCode = assetCodeGenerator.generateAssetCode(asset.getCategory());
        asset.setId(null);
        asset.setAssetCode(assetCode);
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setNetBookValue(asset.getOriginalValue());
        return fixedAssetRepository.save(asset);
    }

    public Page<FixedAsset> listAssets(Pageable pageable, String category, String status,
                                       String department, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return fixedAssetRepository.search(keyword, pageable);
        }
        if (category != null && !category.isBlank()) {
            return fixedAssetRepository.findByCategory(category, pageable);
        }
        if (status != null && !status.isBlank()) {
            return fixedAssetRepository.findByAssetStatus(status, pageable);
        }
        if (department != null && !department.isBlank()) {
            return fixedAssetRepository.findByDepartment(department, pageable);
        }
        return fixedAssetRepository.findAll(pageable);
    }

    public Optional<FixedAsset> getAsset(Long id) {
        return fixedAssetRepository.findById(id);
    }

    @Transactional
    public FixedAsset updateAsset(Long id, FixedAsset body) {
        return fixedAssetRepository.findById(id).map(asset -> {
            if (body.getAssetName() != null) asset.setAssetName(body.getAssetName());
            if (body.getCategory() != null && VALID_CATEGORIES.contains(body.getCategory())) {
                asset.setCategory(body.getCategory());
            }
            if (body.getSpecification() != null) asset.setSpecification(body.getSpecification());
            if (body.getOriginalValue() != null) asset.setOriginalValue(body.getOriginalValue());
            if (body.getPurchaseDate() != null) asset.setPurchaseDate(body.getPurchaseDate());
            if (body.getUsefulLife() != null) asset.setUsefulLife(body.getUsefulLife());
            if (body.getResidualValueRate() != null) asset.setResidualValueRate(body.getResidualValueRate());
            if (body.getLocation() != null) asset.setLocation(body.getLocation());
            if (body.getDepartment() != null) asset.setDepartment(body.getDepartment());
            if (body.getResponsiblePerson() != null) asset.setResponsiblePerson(body.getResponsiblePerson());
            if (body.getAssetStatus() != null && VALID_STATUSES.contains(body.getAssetStatus())) {
                asset.setAssetStatus(body.getAssetStatus());
            }
            return fixedAssetRepository.save(asset);
        }).orElseThrow(() -> new IllegalArgumentException("资产不存在"));
    }

    @Transactional
    public Map<String, Object> calculateMonthlyDepreciation(Integer year, Integer month) {
        List<FixedAsset> assets = fixedAssetRepository.findAssetsForDepreciation();
        int processedCount = 0;
        BigDecimal totalDepreciation = BigDecimal.ZERO;
        List<Map<String, Object>> skippedAssets = new ArrayList<>();

        for (FixedAsset asset : assets) {
            if (depreciationRecordRepository.existsByAssetIdAndDepreciationYearAndDepreciationMonth(
                    asset.getId(), year, month)) {
                skippedAssets.add(Map.of(
                    "assetCode", asset.getAssetCode(),
                    "assetName", asset.getAssetName(),
                    "reason", "该月已计提折旧"
                ));
                continue;
            }

            BigDecimal monthlyDepr = calculateMonthlyDepreciationAmount(asset);
            BigDecimal newAccumulated = asset.getAccumulatedDepreciation().add(monthlyDepr);
            BigDecimal maxDepreciation = asset.getOriginalValue().multiply(
                BigDecimal.ONE.subtract(asset.getResidualValueRate().divide(new BigDecimal("100")))
            );

            if (newAccumulated.compareTo(maxDepreciation) > 0) {
                monthlyDepr = maxDepreciation.subtract(asset.getAccumulatedDepreciation());
                newAccumulated = maxDepreciation;
            }

            BigDecimal netBookValue = asset.getOriginalValue().subtract(newAccumulated);

            DepreciationRecord record = new DepreciationRecord();
            record.setAsset(asset);
            record.setDepreciationYear(year);
            record.setDepreciationMonth(month);
            record.setMonthlyDepreciation(monthlyDepr);
            record.setAccumulatedDepreciation(newAccumulated);
            record.setNetBookValue(netBookValue);
            depreciationRecordRepository.save(record);

            asset.setAccumulatedDepreciation(newAccumulated);
            asset.setNetBookValue(netBookValue);
            fixedAssetRepository.save(asset);

            totalDepreciation = totalDepreciation.add(monthlyDepr);
            processedCount++;
        }

        return Map.of(
            "processedCount", processedCount,
            "totalDepreciation", totalDepreciation,
            "skippedCount", skippedAssets.size(),
            "skippedAssets", skippedAssets
        );
    }

    public BigDecimal calculateMonthlyDepreciationAmount(FixedAsset asset) {
        BigDecimal depreciableAmount = asset.getOriginalValue().multiply(
            BigDecimal.ONE.subtract(asset.getResidualValueRate().divide(new BigDecimal("100")))
        );
        int totalMonths = asset.getUsefulLife() * 12;
        return depreciableAmount.divide(new BigDecimal(totalMonths), 2, RoundingMode.HALF_UP);
    }

    public List<DepreciationRecord> getAssetDepreciationHistory(Long assetId) {
        return depreciationRecordRepository.findByAssetIdOrderByDepreciationYearDescDepreciationMonthDesc(assetId);
    }

    public Page<DepreciationRecord> getMonthlyDepreciationRecords(Integer year, Integer month, Pageable pageable) {
        return depreciationRecordRepository.findByDepreciationYearAndDepreciationMonth(year, month, pageable);
    }

    public BigDecimal getMonthlyDepreciationSum(Integer year, Integer month) {
        BigDecimal sum = depreciationRecordRepository.sumMonthlyDepreciation(year, month);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Transactional
    public AssetTransfer transferAsset(Long assetId, AssetTransfer transfer, String operator) {
        FixedAsset asset = fixedAssetRepository.findById(assetId)
            .orElseThrow(() -> new IllegalArgumentException("资产不存在"));

        if ("已处置".equals(asset.getAssetStatus()) || "报废".equals(asset.getAssetStatus())) {
            throw new IllegalStateException("已处置或报废的资产不能调拨");
        }

        transfer.setId(null);
        transfer.setAsset(asset);
        transfer.setOldDepartment(asset.getDepartment());
        transfer.setOldLocation(asset.getLocation());
        transfer.setOldResponsiblePerson(asset.getResponsiblePerson());
        transfer.setHandledBy(operator);

        if (transfer.getNewDepartment() != null) {
            asset.setDepartment(transfer.getNewDepartment());
        }
        if (transfer.getNewLocation() != null) {
            asset.setLocation(transfer.getNewLocation());
        }
        if (transfer.getNewResponsiblePerson() != null) {
            asset.setResponsiblePerson(transfer.getNewResponsiblePerson());
        }

        fixedAssetRepository.save(asset);
        return assetTransferRepository.save(transfer);
    }

    public List<AssetTransfer> getAssetTransferHistory(Long assetId) {
        return assetTransferRepository.findByAssetIdOrderByCreatedAtDesc(assetId);
    }

    @Transactional
    public AssetStatusChange changeAssetStatus(Long assetId, String newStatus,
                                               String reason, String operator) {
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException("无效的资产状态");
        }

        FixedAsset asset = fixedAssetRepository.findById(assetId)
            .orElseThrow(() -> new IllegalArgumentException("资产不存在"));

        String oldStatus = asset.getAssetStatus();
        if (oldStatus.equals(newStatus)) {
            throw new IllegalStateException("资产状态未变化");
        }

        if ("已处置".equals(oldStatus)) {
            throw new IllegalStateException("已处置的资产不能变更状态");
        }

        AssetStatusChange change = new AssetStatusChange();
        change.setId(null);
        change.setAsset(asset);
        change.setOldStatus(oldStatus);
        change.setNewStatus(newStatus);
        change.setChangeReason(reason);
        change.setOperatedBy(operator);

        asset.setAssetStatus(newStatus);
        fixedAssetRepository.save(asset);

        return assetStatusChangeRepository.save(change);
    }

    public List<AssetStatusChange> getAssetStatusHistory(Long assetId) {
        return assetStatusChangeRepository.findByAssetIdOrderByCreatedAtDesc(assetId);
    }

    @Transactional
    public AssetDisposal createDisposalApplication(Long assetId, AssetDisposal disposal) {
        FixedAsset asset = fixedAssetRepository.findById(assetId)
            .orElseThrow(() -> new IllegalArgumentException("资产不存在"));

        if (!VALID_DISPOSAL_METHODS.contains(disposal.getDisposalMethod())) {
            throw new IllegalArgumentException("无效的处置方式");
        }

        if (assetDisposalRepository.existsByAssetIdAndApprovalStatusNot(assetId, "已驳回")) {
            throw new IllegalStateException("该资产已有未完成的处置申请");
        }

        if ("已处置".equals(asset.getAssetStatus()) || "报废".equals(asset.getAssetStatus())) {
            throw new IllegalStateException("该资产已处置或已报废");
        }

        disposal.setId(null);
        disposal.setAsset(asset);
        disposal.setApprovalStatus("待审批");

        return assetDisposalRepository.save(disposal);
    }

    @Transactional
    public AssetDisposal approveDisposal(Long disposalId, boolean approved, String approver, String remark) {
        AssetDisposal disposal = assetDisposalRepository.findById(disposalId)
            .orElseThrow(() -> new IllegalArgumentException("处置申请不存在"));

        if (!"待审批".equals(disposal.getApprovalStatus())) {
            throw new IllegalStateException("该申请已审批");
        }

        disposal.setApprovalStatus(approved ? "已审批" : "已驳回");
        disposal.setApprovedBy(approver);
        disposal.setApprovalDate(LocalDate.now());
        if (remark != null) {
            disposal.setRemark(remark);
        }

        return assetDisposalRepository.save(disposal);
    }

    @Transactional
    public AssetDisposal completeDisposal(Long disposalId, BigDecimal actualIncome) {
        AssetDisposal disposal = assetDisposalRepository.findById(disposalId)
            .orElseThrow(() -> new IllegalArgumentException("处置申请不存在"));

        if (!"已审批".equals(disposal.getApprovalStatus())) {
            throw new IllegalStateException("该申请尚未审批通过");
        }

        if (disposal.getCompletionDate() != null) {
            throw new IllegalStateException("该处置已完成");
        }

        FixedAsset asset = disposal.getAsset();
        disposal.setActualDisposalIncome(actualIncome);
        disposal.setDisposalGainLoss(actualIncome.subtract(asset.getNetBookValue()));
        disposal.setCompletionDate(LocalDate.now());

        asset.setAssetStatus("已处置");
        fixedAssetRepository.save(asset);

        return assetDisposalRepository.save(disposal);
    }

    public Page<AssetDisposal> listDisposals(Pageable pageable, String approvalStatus) {
        if (approvalStatus != null && !approvalStatus.isBlank()) {
            return assetDisposalRepository.findByApprovalStatus(approvalStatus, pageable);
        }
        return assetDisposalRepository.findAll(pageable);
    }

    @Transactional
    public InventoryTask createInventoryTask(InventoryTask task, String creator) {
        if (!VALID_INVENTORY_SCOPES.contains(task.getInventoryScope())) {
            throw new IllegalArgumentException("无效的盘点范围");
        }

        List<FixedAsset> assets = findAssetsForInventory(task);
        if (assets.isEmpty()) {
            throw new IllegalStateException("没有符合盘点范围的资产");
        }

        task.setId(null);
        task.setTaskCode(assetCodeGenerator.generateInventoryTaskCode());
        task.setTaskStatus("进行中");
        task.setCreatedBy(creator);
        task.setTotalAssets(assets.size());

        InventoryTask savedTask = inventoryTaskRepository.save(task);

        for (FixedAsset asset : assets) {
            InventoryItem item = new InventoryItem();
            item.setTask(savedTask);
            item.setAsset(asset);
            item.setInventoryResult("待盘点");
            inventoryItemRepository.save(item);
        }

        return savedTask;
    }

    private List<FixedAsset> findAssetsForInventory(InventoryTask task) {
        return switch (task.getInventoryScope()) {
            case "全部" -> fixedAssetRepository.findByAssetStatus("在用");
            case "类别" -> fixedAssetRepository.findByAssetStatusAndCategory("在用", task.getCategory());
            case "部门" -> fixedAssetRepository.findByAssetStatusAndDepartment("在用", task.getDepartment());
            default -> throw new IllegalArgumentException("无效的盘点范围");
        };
    }

    public Page<InventoryTask> listInventoryTasks(Pageable pageable, String status) {
        if (status != null && !status.isBlank()) {
            return inventoryTaskRepository.findByTaskStatus(status, pageable);
        }
        return inventoryTaskRepository.findAll(pageable);
    }

    public List<InventoryItem> getInventoryItems(Long taskId) {
        return inventoryItemRepository.findByTaskIdOrderById(taskId);
    }

    @Transactional
    public InventoryItem updateInventoryItem(Long itemId, String result, String remark, String operator) {
        if (!VALID_INVENTORY_RESULTS.contains(result)) {
            throw new IllegalArgumentException("无效的盘点结果");
        }

        InventoryItem item = inventoryItemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("盘点明细不存在"));

        if ("已完成".equals(item.getTask().getTaskStatus())) {
            throw new IllegalStateException("盘点任务已完成，不能修改");
        }

        item.setInventoryResult(result);
        item.setRemark(remark);
        item.setInventoriedBy(operator);
        item.setInventoriedAt(java.time.LocalDateTime.now());

        return inventoryItemRepository.save(item);
    }

    @Transactional
    public InventoryTask completeInventoryTask(Long taskId, String inventoryRemark) {
        InventoryTask task = inventoryTaskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("盘点任务不存在"));

        if ("已完成".equals(task.getTaskStatus())) {
            throw new IllegalStateException("盘点任务已完成");
        }

        List<InventoryItem> items = inventoryItemRepository.findByTaskIdOrderById(taskId);
        for (InventoryItem item : items) {
            if ("待盘点".equals(item.getInventoryResult())) {
                throw new IllegalStateException("还有未盘点的资产，请先完成所有盘点");
            }
        }

        int matched = (int) inventoryItemRepository.countByTaskIdAndInventoryResult(taskId, "账实相符");
        int loss = (int) inventoryItemRepository.countByTaskIdAndInventoryResult(taskId, "盘亏");
        int surplus = (int) inventoryItemRepository.countByTaskIdAndInventoryResult(taskId, "盘盈");

        task.setMatchedCount(matched);
        task.setLossCount(loss);
        task.setSurplusCount(surplus);
        task.setInventoryRemark(inventoryRemark);
        task.setTaskStatus("已完成");
        task.setEndDate(LocalDate.now());

        return inventoryTaskRepository.save(task);
    }

    public Map<String, Object> getInventoryReport(Long taskId) {
        InventoryTask task = inventoryTaskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("盘点任务不存在"));

        if (!"已完成".equals(task.getTaskStatus())) {
            throw new IllegalStateException("盘点任务尚未完成");
        }

        Map<String, Object> report = new HashMap<>();
        report.put("taskCode", task.getTaskCode());
        report.put("taskName", task.getTaskName());
        report.put("inventoryScope", task.getInventoryScope());
        report.put("totalAssets", task.getTotalAssets());
        report.put("matchedCount", task.getMatchedCount());
        report.put("lossCount", task.getLossCount());
        report.put("surplusCount", task.getSurplusCount());
        report.put("matchRate", task.getTotalAssets() > 0 ?
            BigDecimal.valueOf(task.getMatchedCount())
                .divide(BigDecimal.valueOf(task.getTotalAssets()), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")) : BigDecimal.ZERO);
        report.put("startDate", task.getStartDate());
        report.put("endDate", task.getEndDate());
        report.put("inventoryRemark", task.getInventoryRemark());
        return report;
    }

    @Transactional
    public void deleteAsset(Long id) {
        FixedAsset asset = fixedAssetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("资产不存在"));

        if (!"闲置".equals(asset.getAssetStatus()) && !"在用".equals(asset.getAssetStatus())) {
            throw new IllegalStateException("只能删除闲置或在用状态的资产");
        }

        if (assetDisposalRepository.existsByAssetIdAndApprovalStatusNot(id, "已驳回")) {
            throw new IllegalStateException("该资产有未完成的处置申请，不能删除");
        }

        fixedAssetRepository.deleteById(id);
    }
}

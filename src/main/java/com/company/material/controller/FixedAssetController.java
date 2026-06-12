package com.company.material.controller;

import com.company.material.entity.*;
import com.company.material.service.FixedAssetService;
import com.company.material.util.PermissionUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fixed-assets")
@RequiredArgsConstructor
public class FixedAssetController {

    private final FixedAssetService fixedAssetService;
    private final com.company.material.repository.InventoryTaskRepository inventoryTaskRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody FixedAsset asset) {
        try {
            if (asset.getAssetName() == null || asset.getCategory() == null
                    || asset.getOriginalValue() == null || asset.getPurchaseDate() == null
                    || asset.getUsefulLife() == null || asset.getResidualValueRate() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "资产名称、类别、原值、购入日期、使用年限、残值率为必填"
                ));
            }
            FixedAsset saved = fixedAssetService.createAsset(asset);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String keyword) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<FixedAsset> result = fixedAssetService.listAssets(pr, category, status, department, keyword);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return fixedAssetService.getAsset(id)
                .map(a -> ResponseEntity.ok((Object) a))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody FixedAsset body) {
        try {
            FixedAsset updated = fixedAssetService.updateAsset(id, body);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            fixedAssetService.deleteAsset(id);
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/depreciation/calculate")
    public ResponseEntity<?> calculateDepreciation(
            @RequestBody Map<String, Integer> request,
            HttpServletRequest httpRequest) {
        if (!PermissionUtil.hasDepreciationPermission(httpRequest)) {
            return PermissionUtil.forbiddenResponse();
        }
        try {
            Integer year = request.get("year");
            Integer month = request.get("month");
            if (year == null || month == null || month < 1 || month > 12) {
                return ResponseEntity.badRequest().body(Map.of("error", "请输入有效的年份和月份"));
            }
            Map<String, Object> result = fixedAssetService.calculateMonthlyDepreciation(year, month);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/depreciation-history")
    public ResponseEntity<?> getDepreciationHistory(@PathVariable Long id) {
        if (!fixedAssetService.getAsset(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        List<DepreciationRecord> history = fixedAssetService.getAssetDepreciationHistory(id);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/depreciation/monthly")
    public ResponseEntity<?> getMonthlyDepreciation(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("depreciationYear").descending()
                .and(Sort.by("depreciationMonth").descending()));
        Page<DepreciationRecord> records = fixedAssetService.getMonthlyDepreciationRecords(year, month, pr);
        BigDecimal total = fixedAssetService.getMonthlyDepreciationSum(year, month);
        return ResponseEntity.ok(Map.of(
            "records", records,
            "totalAmount", total
        ));
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<?> transferAsset(
            @PathVariable Long id,
            @RequestBody AssetTransfer transfer,
            HttpServletRequest httpRequest) {
        try {
            String operator = PermissionUtil.getCurrentUsername(httpRequest);
            AssetTransfer result = fixedAssetService.transferAsset(id, transfer, operator);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/transfer-history")
    public ResponseEntity<?> getTransferHistory(@PathVariable Long id) {
        if (!fixedAssetService.getAsset(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        List<AssetTransfer> history = fixedAssetService.getAssetTransferHistory(id);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{id}/status-change")
    public ResponseEntity<?> changeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String newStatus = request.get("newStatus");
            String reason = request.get("reason");
            String operator = PermissionUtil.getCurrentUsername(httpRequest);
            AssetStatusChange result = fixedAssetService.changeAssetStatus(id, newStatus, reason, operator);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/status-history")
    public ResponseEntity<?> getStatusHistory(@PathVariable Long id) {
        if (!fixedAssetService.getAsset(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        List<AssetStatusChange> history = fixedAssetService.getAssetStatusHistory(id);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{id}/disposal")
    public ResponseEntity<?> createDisposal(
            @PathVariable Long id,
            @RequestBody AssetDisposal disposal,
            HttpServletRequest httpRequest) {
        if (!PermissionUtil.hasDisposalPermission(httpRequest)) {
            return PermissionUtil.forbiddenResponse();
        }
        try {
            AssetDisposal result = fixedAssetService.createDisposalApplication(id, disposal);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/disposals/{disposalId}/approve")
    public ResponseEntity<?> approveDisposal(
            @PathVariable Long disposalId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        if (!PermissionUtil.hasDisposalPermission(httpRequest)) {
            return PermissionUtil.forbiddenResponse();
        }
        try {
            Boolean approved = (Boolean) request.get("approved");
            String remark = (String) request.get("remark");
            String approver = PermissionUtil.getCurrentUsername(httpRequest);
            AssetDisposal result = fixedAssetService.approveDisposal(disposalId, approved, approver, remark);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/disposals/{disposalId}/complete")
    public ResponseEntity<?> completeDisposal(
            @PathVariable Long disposalId,
            @RequestBody Map<String, BigDecimal> request,
            HttpServletRequest httpRequest) {
        if (!PermissionUtil.hasDisposalPermission(httpRequest)) {
            return PermissionUtil.forbiddenResponse();
        }
        try {
            BigDecimal actualIncome = request.get("actualIncome");
            if (actualIncome == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "请输入实际处置收入"));
            }
            AssetDisposal result = fixedAssetService.completeDisposal(disposalId, actualIncome);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/disposals")
    public ResponseEntity<?> listDisposals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String approvalStatus) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AssetDisposal> result = fixedAssetService.listDisposals(pr, approvalStatus);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/inventory")
    public ResponseEntity<?> createInventoryTask(
            @RequestBody InventoryTask task,
            HttpServletRequest httpRequest) {
        try {
            String creator = PermissionUtil.getCurrentUsername(httpRequest);
            if (task.getTaskName() == null || task.getInventoryScope() == null
                    || task.getStartDate() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "盘点任务名称、盘点范围、开始日期为必填"
                ));
            }
            InventoryTask result = fixedAssetService.createInventoryTask(task, creator);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/inventory")
    public ResponseEntity<?> listInventoryTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<InventoryTask> result = fixedAssetService.listInventoryTasks(pr, status);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/inventory/{taskId}/items")
    public ResponseEntity<?> getInventoryItems(@PathVariable Long taskId) {
        if (!inventoryTaskRepository.existsById(taskId)) {
            return ResponseEntity.notFound().build();
        }
        List<InventoryItem> items = fixedAssetService.getInventoryItems(taskId);
        return ResponseEntity.ok(items);
    }

    @PutMapping("/inventory/items/{itemId}")
    public ResponseEntity<?> updateInventoryItem(
            @PathVariable Long itemId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String result = request.get("inventoryResult");
            String remark = request.get("remark");
            String operator = PermissionUtil.getCurrentUsername(httpRequest);
            InventoryItem updated = fixedAssetService.updateInventoryItem(itemId, result, remark, operator);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/inventory/{taskId}/complete")
    public ResponseEntity<?> completeInventory(
            @PathVariable Long taskId,
            @RequestBody(required = false) Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String remark = request != null ? request.get("inventoryRemark") : null;
            InventoryTask result = fixedAssetService.completeInventoryTask(taskId, remark);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/inventory/{taskId}/report")
    public ResponseEntity<?> getInventoryReport(@PathVariable Long taskId) {
        try {
            Map<String, Object> report = fixedAssetService.getInventoryReport(taskId);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

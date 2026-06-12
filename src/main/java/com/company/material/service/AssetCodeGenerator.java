package com.company.material.service;

import com.company.material.repository.FixedAssetRepository;
import com.company.material.repository.InventoryTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AssetCodeGenerator {

    private final FixedAssetRepository fixedAssetRepository;
    private final InventoryTaskRepository inventoryTaskRepository;
    private final Map<String, Integer> sequenceCache = new ConcurrentHashMap<>();

    private static final Map<String, String> CATEGORY_CODES = Map.of(
        "生产设备", "SB",
        "办公设备", "BG",
        "运输车辆", "CL",
        "房屋建筑", "FW",
        "电子设备", "DZ"
    );

    @Transactional
    public String generateAssetCode(String category) {
        String categoryCode = CATEGORY_CODES.getOrDefault(category, "QT");
        String prefix = "GD" + categoryCode;
        int sequence = getNextSequence(prefix);
        return prefix + String.format("%05d", sequence);
    }

    @Transactional
    public String generateInventoryTaskCode() {
        String prefix = "PD" + LocalDate.now().getYear();
        int sequence = getNextTaskSequence(prefix);
        return prefix + String.format("%04d", sequence);
    }

    private int getNextSequence(String prefix) {
        String maxCode = fixedAssetRepository.findAll().stream()
            .map(a -> a.getAssetCode())
            .filter(code -> code.startsWith(prefix))
            .max(String::compareTo)
            .orElse(null);
        if (maxCode == null) {
            return 1;
        }
        String seqStr = maxCode.substring(prefix.length());
        return Integer.parseInt(seqStr) + 1;
    }

    private int getNextTaskSequence(String prefix) {
        String maxCode = inventoryTaskRepository.findAll().stream()
            .map(t -> t.getTaskCode())
            .filter(code -> code.startsWith(prefix))
            .max(String::compareTo)
            .orElse(null);
        if (maxCode == null) {
            return 1;
        }
        String seqStr = maxCode.substring(prefix.length());
        return Integer.parseInt(seqStr) + 1;
    }
}

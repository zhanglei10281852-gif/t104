package com.company.material.repository;

import com.company.material.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByTaskIdOrderById(Long taskId);

    @Query("SELECT i.inventoryResult, COUNT(i) FROM InventoryItem i " +
           "WHERE i.task.id = :taskId GROUP BY i.inventoryResult")
    List<Object[]> countByResultForTask(@Param("taskId") Long taskId);

    long countByTaskIdAndInventoryResult(Long taskId, String inventoryResult);
}

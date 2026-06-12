package com.company.material.repository;

import com.company.material.entity.InventoryTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryTaskRepository extends JpaRepository<InventoryTask, Long> {

    Optional<InventoryTask> findByTaskCode(String taskCode);

    boolean existsByTaskCode(String taskCode);

    Page<InventoryTask> findByTaskStatus(String taskStatus, Pageable pageable);

    Page<InventoryTask> findByCreatedBy(String createdBy, Pageable pageable);
}

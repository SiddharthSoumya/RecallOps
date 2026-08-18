package com.recallops.memory.repository;

import com.recallops.memory.entity.WorkingMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkingMemoryRepository
        extends JpaRepository<WorkingMemory, UUID> {

    Optional<WorkingMemory> findByInvestigationId(UUID investigationId);
}
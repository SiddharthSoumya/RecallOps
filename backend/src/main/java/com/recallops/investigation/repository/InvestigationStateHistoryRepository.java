package com.recallops.investigation.repository;

import com.recallops.investigation.entity.InvestigationStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvestigationStateHistoryRepository
        extends JpaRepository<InvestigationStateHistory, UUID> {

    List<InvestigationStateHistory> findByInvestigationIdOrderByTransitionedAtAsc(
            UUID investigationId
    );
}
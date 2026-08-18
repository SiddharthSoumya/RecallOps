package com.recallops.investigation.repository;

import com.recallops.investigation.entity.Investigation;
import com.recallops.investigation.state.InvestigationState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestigationRepository
        extends JpaRepository<Investigation, UUID> {

    Optional<Investigation> findByIncidentId(UUID incidentId);

    List<Investigation> findByResolvedFalse();

    List<Investigation> findByResolvedFalseAndCurrentState(
            InvestigationState currentState
    );
}
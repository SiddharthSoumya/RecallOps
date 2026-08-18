package com.recallops.investigation.service;

import com.recallops.investigation.entity.InvestigationStateHistory;
import com.recallops.investigation.repository.InvestigationStateHistoryRepository;
import com.recallops.common.exception.ConflictException;
import com.recallops.common.exception.ResourceNotFoundException;
import com.recallops.incident.entity.Incident;
import com.recallops.incident.repository.IncidentRepository;
import com.recallops.investigation.dto.InvestigationResponse;
import com.recallops.investigation.entity.Investigation;
import com.recallops.investigation.repository.InvestigationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recallops.investigation.state.InvestigationState;
import com.recallops.investigation.state.InvestigationStateTransition;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvestigationService {

    private final InvestigationRepository investigationRepository;
    private final IncidentRepository incidentRepository;
    private final InvestigationStateHistoryRepository stateHistoryRepository;

    @Transactional
    public InvestigationResponse startInvestigation(UUID incidentId) {

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Incident not found: " + incidentId
                        ));

        investigationRepository.findByIncidentId(incidentId)
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "Investigation already exists for incident: "
                                    + incidentId
                    );
                });

        Investigation investigation = Investigation.builder()
                .incident(incident)
                .build();

        Investigation savedInvestigation =
                investigationRepository.save(investigation);

        return toResponse(savedInvestigation);
    }

    @Transactional(readOnly = true)
    public InvestigationResponse getInvestigation(UUID investigationId) {

        Investigation investigation =
                investigationRepository.findById(investigationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Investigation not found: "
                                                + investigationId
                                ));

        return toResponse(investigation);
    }

    @Transactional(readOnly = true)
    public InvestigationResponse getInvestigationByIncident(
            UUID incidentId) {

        Investigation investigation =
                investigationRepository.findByIncidentId(incidentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Investigation not found for incident: "
                                                + incidentId
                                ));

        return toResponse(investigation);
    }

    private InvestigationResponse toResponse(
            Investigation investigation) {

        return new InvestigationResponse(
                investigation.getId(),
                investigation.getIncident().getId(),
                investigation.getCurrentState(),
                investigation.isResolved(),
                investigation.getResolvedAt(),
                investigation.getCreatedAt(),
                investigation.getUpdatedAt()
        );
    }

    @Transactional
    public InvestigationResponse transition(
            UUID investigationId,
            InvestigationState targetState) {

        Investigation investigation = investigationRepository
                .findById(investigationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Investigation not found: " + investigationId
                        ));

        InvestigationState currentState =
                investigation.getCurrentState();

        if (!InvestigationStateTransition.isAllowed(
                currentState,
                targetState)) {

            throw new ConflictException(
                    "Invalid investigation state transition: "
                            + currentState
                            + " -> "
                            + targetState
            );
        }

        investigation.setCurrentState(targetState);

        if (targetState == InvestigationState.RESOLVED) {
            investigation.setResolved(true);
            investigation.setResolvedAt(Instant.now());
        }

        Investigation saved =
                investigationRepository.save(investigation);

        InvestigationStateHistory history =
                InvestigationStateHistory.builder()
                        .investigation(saved)
                        .fromState(currentState)
                        .toState(targetState)
                        .transitionedAt(Instant.now())
                        .build();

        stateHistoryRepository.save(history);

        return toResponse(saved);
    }
}
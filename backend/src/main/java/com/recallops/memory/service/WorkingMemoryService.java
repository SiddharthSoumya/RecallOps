package com.recallops.memory.service;

import com.recallops.common.exception.ResourceNotFoundException;
import com.recallops.investigation.entity.Investigation;
import com.recallops.investigation.repository.InvestigationRepository;
import com.recallops.memory.dto.UpdateWorkingMemoryRequest;
import com.recallops.memory.dto.WorkingMemoryResponse;
import com.recallops.memory.entity.WorkingMemory;
import com.recallops.memory.repository.WorkingMemoryRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkingMemoryService {

    private final WorkingMemoryRepository workingMemoryRepository;
    private final InvestigationRepository investigationRepository;

    @Transactional
    public WorkingMemoryResponse create(
            UUID investigationId,
            UpdateWorkingMemoryRequest request
    ) {

        Investigation investigation =
                investigationRepository.findById(investigationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Investigation not found: "
                                                + investigationId
                                )
                        );

        if (workingMemoryRepository
                .findByInvestigationId(investigationId)
                .isPresent()) {

            throw new IllegalStateException(
                    "Working memory already exists for investigation: "
                            + investigationId
            );
        }

        WorkingMemory memory = WorkingMemory.builder()
                .investigation(investigation)
                .currentHypothesis(request.currentHypothesis())
                .confidence(request.confidence())
                .observations(request.observations())
                .completedActions(request.completedActions())
                .nextAction(request.nextAction())
                .build();

        return toResponse(
                workingMemoryRepository.save(memory)
        );
    }

    @Transactional(readOnly = true)
    public WorkingMemoryResponse getByInvestigationId(
            UUID investigationId
    ) {

        return workingMemoryRepository
                .findByInvestigationId(investigationId)
                .map(this::toResponse)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Working memory not found for investigation: "
                                        + investigationId
                        )
                );
    }

    @Transactional
    public WorkingMemoryResponse update(
            UUID investigationId,
            UpdateWorkingMemoryRequest request
    ) {

        WorkingMemory memory =
                workingMemoryRepository
                        .findByInvestigationId(investigationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Working memory not found for investigation: "
                                                + investigationId
                                )
                        );

        memory.setCurrentHypothesis(request.currentHypothesis());
        memory.setConfidence(request.confidence());
        memory.setObservations(request.observations());
        memory.setCompletedActions(request.completedActions());
        memory.setNextAction(request.nextAction());

        return toResponse(
                workingMemoryRepository.save(memory)
        );
    }

    private WorkingMemoryResponse toResponse(
            WorkingMemory memory
    ) {

        return new WorkingMemoryResponse(
                memory.getId(),
                memory.getInvestigation().getId(),
                memory.getCurrentHypothesis(),
                memory.getConfidence(),
                memory.getObservations(),
                memory.getCompletedActions(),
                memory.getNextAction(),
                memory.getVersion(),
                memory.getCreatedAt(),
                memory.getUpdatedAt()
        );
    }
}
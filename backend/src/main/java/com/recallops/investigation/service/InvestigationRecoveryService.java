package com.recallops.investigation.service;

import com.recallops.common.exception.ResourceNotFoundException;
import com.recallops.investigation.dto.InvestigationRecoveryResponse;
import com.recallops.investigation.entity.Investigation;
import com.recallops.investigation.entity.InvestigationStateHistory;
import com.recallops.investigation.repository.InvestigationRepository;
import com.recallops.investigation.repository.InvestigationStateHistoryRepository;
import com.recallops.investigation.state.InvestigationState;
import com.recallops.memory.dto.WorkingMemoryResponse;
import com.recallops.memory.repository.WorkingMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestigationRecoveryService implements ApplicationRunner {

    private final InvestigationRepository investigationRepository;
    private final InvestigationStateHistoryRepository stateHistoryRepository;
    private final WorkingMemoryRepository workingMemoryRepository;

    /**
     * Runs automatically when Spring Boot finishes starting.
     *
     * Any unresolved investigation is considered recoverable.
     */
    @Override
    public void run(ApplicationArguments args) {

        log.info("Starting investigation recovery scan...");

        List<Investigation> investigations =
                investigationRepository.findByResolvedFalse();

        if (investigations.isEmpty()) {
            log.info(
                    "No unfinished investigations found during startup recovery."
            );
            return;
        }

        log.info(
                "Found {} unfinished investigation(s) during startup recovery.",
                investigations.size()
        );

        for (Investigation investigation : investigations) {

            try {

                recoverInternal(investigation);

                log.info(
                        "Investigation {} successfully recovered.",
                        investigation.getId()
                );

            } catch (Exception exception) {

                log.error(
                        "Failed to recover investigation {}",
                        investigation.getId(),
                        exception
                );
            }
        }
    }

    /**
     * Manual recovery endpoint.
     */
    @Transactional
    public InvestigationRecoveryResponse recover(
            UUID investigationId
    ) {

        Investigation investigation =
                investigationRepository.findById(investigationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Investigation not found: "
                                                + investigationId
                                )
                        );

        return recoverInternal(investigation);
    }

    /**
     * Recovery is intentionally separate from the normal state machine.
     *
     * A process restart is not a normal business transition.
     * The investigation is restored to RESUMED so the agent can continue.
     */
    @Transactional
    protected InvestigationRecoveryResponse recoverInternal(
            Investigation investigation
    ) {

        InvestigationState previousState =
                investigation.getCurrentState();

        /*
         * Already resolved investigations do not need recovery.
         */
        if (investigation.isResolved()) {

            WorkingMemoryResponse workingMemory =
                    getWorkingMemory(investigation.getId());

            return new InvestigationRecoveryResponse(
                    investigation.getId(),
                    investigation.getIncident().getId(),
                    previousState,
                    previousState,
                    workingMemory
            );
        }

        /*
         * Do not create a duplicate recovery event if the investigation
         * is already RESUMED.
         */
        if (previousState == InvestigationState.RESUMED) {

            WorkingMemoryResponse workingMemory =
                    getWorkingMemory(investigation.getId());

            return new InvestigationRecoveryResponse(
                    investigation.getId(),
                    investigation.getIncident().getId(),
                    previousState,
                    InvestigationState.RESUMED,
                    workingMemory
            );
        }

        Instant now = Instant.now();

        investigation.setCurrentState(
                InvestigationState.RESUMED
        );

        Investigation saved =
                investigationRepository.save(investigation);

        InvestigationStateHistory history =
                InvestigationStateHistory.builder()
                        .investigation(saved)
                        .fromState(previousState)
                        .toState(InvestigationState.RESUMED)
                        .transitionedAt(now)
                        .build();

        stateHistoryRepository.save(history);

        WorkingMemoryResponse workingMemory =
                getWorkingMemory(saved.getId());

        return new InvestigationRecoveryResponse(
                saved.getId(),
                saved.getIncident().getId(),
                previousState,
                InvestigationState.RESUMED,
                workingMemory
        );
    }

    private WorkingMemoryResponse getWorkingMemory(
            UUID investigationId
    ) {

        return workingMemoryRepository
                .findByInvestigationId(investigationId)
                .map(memory -> new WorkingMemoryResponse(
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
                ))
                .orElse(null);
    }
}
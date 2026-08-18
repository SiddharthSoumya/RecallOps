package com.recallops.investigation.agent;

import com.recallops.common.exception.ResourceNotFoundException;
import com.recallops.investigation.entity.Investigation;
import com.recallops.investigation.repository.InvestigationRepository;
import com.recallops.investigation.state.InvestigationState;
import com.recallops.memory.entity.WorkingMemory;
import com.recallops.memory.repository.WorkingMemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvestigationAgentService {

    private final InvestigationRepository investigationRepository;
    private final WorkingMemoryRepository workingMemoryRepository;
    private final ReasoningEngine reasoningEngine;

    @Transactional
    public WorkingMemory step(UUID investigationId) {

        Investigation investigation =
                investigationRepository.findById(investigationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Investigation not found: "
                                                + investigationId
                                )
                        );

        WorkingMemory memory =
                workingMemoryRepository
                        .findByInvestigationId(investigationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Working memory not found for investigation: "
                                                + investigationId
                                )
                        );

        /*
         * Terminal states do not require another reasoning step.
         */
        if (investigation.isResolved()
                || investigation.getCurrentState()
                == InvestigationState.LEARNING) {

            return memory;
        }

        AgentDecision decision =
                reasoningEngine.reason(
                        investigation,
                        memory
                );

        memory.setCurrentHypothesis(
                decision.currentHypothesis()
        );

        memory.setConfidence(
                decision.confidence()
        );

        /*
         * Convert the reasoning result into JSON.
         *
         * We will improve this representation later when we
         * introduce structured observations and action history.
         */
        memory.setObservations(
                com.fasterxml.jackson.databind.node.JsonNodeFactory
                        .instance
                        .arrayNode()
                        .addAll(
                                decision.observations()
                                        .stream()
                                        .map(
                                                com.fasterxml.jackson.databind.node.JsonNodeFactory
                                                        .instance::textNode
                                        )
                                        .toList()
                        )
        );

        memory.setCompletedActions(
                com.fasterxml.jackson.databind.node.JsonNodeFactory
                        .instance
                        .arrayNode()
                        .addAll(
                                decision.completedActions()
                                        .stream()
                                        .map(
                                                com.fasterxml.jackson.databind.node.JsonNodeFactory
                                                        .instance::textNode
                                        )
                                        .toList()
                        )
        );

        memory.setNextAction(
                decision.nextAction()
        );

        return workingMemoryRepository.save(memory);
    }
}
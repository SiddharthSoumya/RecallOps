package com.recallops.investigation;

import com.recallops.incident.entity.Incident;
import com.recallops.incident.entity.IncidentSeverity;
import com.recallops.incident.entity.IncidentStatus;
import com.recallops.incident.repository.IncidentRepository;
import com.recallops.investigation.entity.Investigation;
import com.recallops.investigation.repository.InvestigationRepository;
import com.recallops.investigation.state.InvestigationState;
import com.recallops.investigation.service.InvestigationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InvestigationStateTransitionTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private InvestigationRepository investigationRepository;

    @Autowired
    private InvestigationService investigationService;

    @Test
    void shouldExecuteCompleteInvestigationLifecycle() {

        Incident incident = incidentRepository.save(
                Incident.builder()
                        .title("Payment latency")
                        .description("Payment latency increased")
                        .affectedService("payment-service")
                        .severity(IncidentSeverity.HIGH)
                        .status(IncidentStatus.OPEN)
                        .build()
        );

        Investigation investigation = investigationRepository.save(
                Investigation.builder()
                        .incident(incident)
                        .currentState(InvestigationState.NEW)
                        .resolved(false)
                        .build()
        );

        InvestigationState[] states = {
                InvestigationState.LOG_ANALYSIS,
                InvestigationState.GENERATE_HYPOTHESES,
                InvestigationState.RECALL_SIMILAR_INCIDENTS,
                InvestigationState.REFINE_HYPOTHESES,
                InvestigationState.PLAN_NEXT_ACTION,
                InvestigationState.WAITING,
                InvestigationState.RESUMED,
                InvestigationState.RESOLVED,
                InvestigationState.LEARNING
        };

        for (InvestigationState state : states) {
            investigationService.transition(
                    investigation.getId(),
                    state
            );
        }

        Investigation persisted =
                investigationRepository
                        .findById(investigation.getId())
                        .orElseThrow();

        assertThat(persisted.getCurrentState())
                .isEqualTo(InvestigationState.LEARNING);

        assertThat(persisted.isResolved())
                .isTrue();

        assertThat(persisted.getResolvedAt())
                .isNotNull();
    }
}
package com.recallops.investigation;

import com.recallops.incident.entity.Incident;
import com.recallops.incident.entity.IncidentSeverity;
import com.recallops.incident.entity.IncidentStatus;
import com.recallops.incident.repository.IncidentRepository;
import com.recallops.investigation.entity.Investigation;
import com.recallops.investigation.entity.InvestigationStateHistory;
import com.recallops.investigation.repository.InvestigationRepository;
import com.recallops.investigation.repository.InvestigationStateHistoryRepository;
import com.recallops.investigation.service.InvestigationService;
import com.recallops.investigation.state.InvestigationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InvestigationStateHistoryTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private InvestigationRepository investigationRepository;

    @Autowired
    private InvestigationStateHistoryRepository stateHistoryRepository;

    @Autowired
    private InvestigationService investigationService;

    @Test
    void shouldPersistStateTransitionHistory() {

        Incident incident = incidentRepository.save(
                Incident.builder()
                        .title("Test incident")
                        .description("Test incident for state history")
                        .affectedService("test-service")
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

        investigationService.transition(
                investigation.getId(),
                InvestigationState.LOG_ANALYSIS
        );

        List<InvestigationStateHistory> history =
                stateHistoryRepository
                        .findByInvestigationIdOrderByTransitionedAtAsc(
                                investigation.getId()
                        );

        assertThat(history).hasSize(1);

        InvestigationStateHistory transition = history.getFirst();

        assertThat(transition.getFromState())
                .isEqualTo(InvestigationState.NEW);

        assertThat(transition.getToState())
                .isEqualTo(InvestigationState.LOG_ANALYSIS);

        Investigation persisted =
                investigationRepository
                        .findById(investigation.getId())
                        .orElseThrow();

        assertThat(persisted.getCurrentState())
                .isEqualTo(InvestigationState.LOG_ANALYSIS);
    }
}
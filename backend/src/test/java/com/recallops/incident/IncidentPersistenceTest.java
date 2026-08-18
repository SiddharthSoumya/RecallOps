package com.recallops.incident;

import com.recallops.incident.entity.Incident;
import com.recallops.incident.entity.IncidentSeverity;
import com.recallops.incident.entity.IncidentStatus;
import com.recallops.incident.repository.IncidentRepository;
import com.recallops.investigation.entity.Investigation;
import com.recallops.investigation.repository.InvestigationRepository;
import com.recallops.investigation.state.InvestigationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IncidentPersistenceTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private InvestigationRepository investigationRepository;

    @Test
    void shouldPersistAndRetrieveIncidentWithInvestigation() {

        Incident incident = Incident.builder()
                .title("Payment service latency")
                .description("Payment API latency increased significantly.")
                .affectedService("payment-service")
                .severity(IncidentSeverity.HIGH)
                .build();

        Incident savedIncident = incidentRepository.save(incident);

        assertThat(savedIncident.getId()).isNotNull();

        Investigation investigation = Investigation.builder()
                .incident(savedIncident)
                .build();

        Investigation savedInvestigation =
                investigationRepository.save(investigation);

        assertThat(savedInvestigation.getId()).isNotNull();
        assertThat(savedInvestigation.getCurrentState())
                .isEqualTo(InvestigationState.NEW);
        assertThat(savedInvestigation.isResolved())
                .isFalse();

        UUID investigationId = savedInvestigation.getId();

        Investigation retrievedInvestigation =
                investigationRepository.findById(investigationId)
                        .orElseThrow();

        assertThat(retrievedInvestigation.getIncident().getId())
                .isEqualTo(savedIncident.getId());

        assertThat(retrievedInvestigation.getCurrentState())
                .isEqualTo(InvestigationState.NEW);

        assertThat(retrievedInvestigation.isResolved())
                .isFalse();
    }
}
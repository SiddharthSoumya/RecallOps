package com.recallops.incident.service;

import com.recallops.common.exception.ResourceNotFoundException;
import com.recallops.incident.dto.CreateIncidentRequest;
import com.recallops.incident.dto.IncidentResponse;
import com.recallops.incident.entity.Incident;
import com.recallops.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;

    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request) {

        Incident incident = Incident.builder()
                .title(request.title())
                .description(request.description())
                .affectedService(request.affectedService())
                .severity(request.severity())
                .build();

        Incident savedIncident = incidentRepository.save(incident);

        return toResponse(savedIncident);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncident(UUID incidentId) {

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Incident not found: " + incidentId
                        ));

        return toResponse(incident);
    }

    private IncidentResponse toResponse(Incident incident) {

        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getAffectedService(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }
}
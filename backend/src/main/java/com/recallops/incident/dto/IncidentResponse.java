package com.recallops.incident.dto;

import com.recallops.incident.entity.IncidentSeverity;
import com.recallops.incident.entity.IncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        String title,
        String description,
        String affectedService,
        IncidentSeverity severity,
        IncidentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}

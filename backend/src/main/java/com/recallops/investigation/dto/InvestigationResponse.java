package com.recallops.investigation.dto;

import com.recallops.investigation.state.InvestigationState;

import java.time.Instant;
import java.util.UUID;

public record InvestigationResponse(
        UUID id,
        UUID incidentId,
        InvestigationState state,
        boolean resolved,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
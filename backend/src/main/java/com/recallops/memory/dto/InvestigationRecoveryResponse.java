package com.recallops.investigation.dto;

import com.recallops.investigation.state.InvestigationState;
import com.recallops.memory.dto.WorkingMemoryResponse;

import java.util.UUID;

public record InvestigationRecoveryResponse(
        UUID investigationId,
        UUID incidentId,
        InvestigationState previousState,
        InvestigationState recoveredState,
        WorkingMemoryResponse workingMemory
) {
}
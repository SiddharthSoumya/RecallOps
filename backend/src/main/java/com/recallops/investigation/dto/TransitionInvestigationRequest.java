package com.recallops.investigation.dto;

import com.recallops.investigation.state.InvestigationState;
import jakarta.validation.constraints.NotNull;

public record TransitionInvestigationRequest(
        @NotNull
        InvestigationState targetState
) {
}
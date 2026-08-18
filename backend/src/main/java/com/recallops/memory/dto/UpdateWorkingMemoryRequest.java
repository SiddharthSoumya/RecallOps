package com.recallops.memory.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateWorkingMemoryRequest(

        @Size(max = 2000)
        String currentHypothesis,

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        BigDecimal confidence,

        JsonNode observations,

        JsonNode completedActions,

        @Size(max = 2000)
        String nextAction
) {
}
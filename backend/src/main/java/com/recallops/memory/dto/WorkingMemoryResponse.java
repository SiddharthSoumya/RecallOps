package com.recallops.memory.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WorkingMemoryResponse(
        UUID id,
        UUID investigationId,
        String currentHypothesis,
        BigDecimal confidence,
        JsonNode observations,
        JsonNode completedActions,
        String nextAction,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
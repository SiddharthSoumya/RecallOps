package com.recallops.investigation.agent;

import java.math.BigDecimal;
import java.util.List;

public record AgentDecision(
        String currentHypothesis,
        BigDecimal confidence,
        List<String> observations,
        List<String> completedActions,
        String nextAction
) {
}
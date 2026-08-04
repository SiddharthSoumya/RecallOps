package com.recallops.investigation.state;

public enum InvestigationState {
    NEW,
    LOG_ANALYSIS,
    GENERATE_HYPOTHESES,
    RECALL_SIMILAR_INCIDENTS,
    REFINE_HYPOTHESES,
    PLAN_NEXT_ACTION,
    WAITING,
    RESUMED,
    RESOLVED,
    LEARNING
}

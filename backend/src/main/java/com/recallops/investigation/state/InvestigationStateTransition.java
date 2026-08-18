package com.recallops.investigation.state;

import java.util.Map;
import java.util.Set;

public final class InvestigationStateTransition {

    private static final Map<InvestigationState, Set<InvestigationState>>
            ALLOWED_TRANSITIONS = Map.of(

            InvestigationState.NEW,
            Set.of(InvestigationState.LOG_ANALYSIS),

            InvestigationState.LOG_ANALYSIS,
            Set.of(InvestigationState.GENERATE_HYPOTHESES),

            InvestigationState.GENERATE_HYPOTHESES,
            Set.of(InvestigationState.RECALL_SIMILAR_INCIDENTS),

            InvestigationState.RECALL_SIMILAR_INCIDENTS,
            Set.of(InvestigationState.REFINE_HYPOTHESES),

            InvestigationState.REFINE_HYPOTHESES,
            Set.of(InvestigationState.PLAN_NEXT_ACTION),

            InvestigationState.PLAN_NEXT_ACTION,
            Set.of(InvestigationState.WAITING),

            InvestigationState.WAITING,
            Set.of(InvestigationState.RESUMED),

            InvestigationState.RESUMED,
            Set.of(InvestigationState.RESOLVED),

            InvestigationState.RESOLVED,
            Set.of(InvestigationState.LEARNING),

            InvestigationState.LEARNING,
            Set.of()
    );

    private InvestigationStateTransition() {
    }

    public static boolean isAllowed(
            InvestigationState current,
            InvestigationState target) {

        return ALLOWED_TRANSITIONS
                .getOrDefault(current, Set.of())
                .contains(target);
    }

    /**
     * Recovery transition.
     *
     * Any unfinished investigation can be resumed from its
     * last persisted checkpoint.
     */
    public static boolean isRecoverable(
            InvestigationState current) {

        return current != InvestigationState.RESOLVED
                && current != InvestigationState.LEARNING
                && current != InvestigationState.RESUMED;
    }
}
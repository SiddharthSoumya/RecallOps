package com.recallops.investigation.agent;

import com.recallops.investigation.entity.Investigation;
import com.recallops.investigation.state.InvestigationState;
import com.recallops.memory.entity.WorkingMemory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DeterministicReasoningEngine
        implements ReasoningEngine {

    @Override
    public AgentDecision reason(
            Investigation investigation,
            WorkingMemory workingMemory
    ) {

        InvestigationState state =
                investigation.getCurrentState();

        return switch (state) {

            case NEW, LOG_ANALYSIS -> new AgentDecision(
                    "Database connection pool exhaustion may be causing service latency.",
                    new BigDecimal("0.72"),
                    List.of(
                            "Service latency increased significantly.",
                            "Affected service requires investigation."
                    ),
                    List.of(
                            "Incident created.",
                            "Initial investigation started.",
                            "Entered log analysis state."
                    ),
                    "Inspect database connection pool utilization."
            );

            case GENERATE_HYPOTHESES -> new AgentDecision(
                    "Database connection pool exhaustion is a likely cause.",
                    new BigDecimal("0.78"),
                    List.of(
                            "Connection pool exhaustion is consistent with elevated latency."
                    ),
                    List.of(
                            "Generated initial hypotheses."
                    ),
                    "Recall similar historical incidents."
            );

            case RECALL_SIMILAR_INCIDENTS -> new AgentDecision(
                    "Historical incidents may confirm the connection pool hypothesis.",
                    new BigDecimal("0.82"),
                    List.of(
                            "Similar incidents are being evaluated."
                    ),
                    List.of(
                            "Retrieved similar incidents."
                    ),
                    "Refine the active hypothesis."
            );

            case REFINE_HYPOTHESES -> new AgentDecision(
                    "Connection pool exhaustion remains the strongest hypothesis.",
                    new BigDecimal("0.86"),
                    List.of(
                            "Evidence continues to support the connection pool hypothesis."
                    ),
                    List.of(
                            "Refined investigation hypothesis."
                    ),
                    "Plan the next diagnostic action."
            );

            case PLAN_NEXT_ACTION -> new AgentDecision(
                    workingMemory != null
                            ? workingMemory.getCurrentHypothesis()
                            : "Determine the most likely root cause.",
                    workingMemory != null
                            ? workingMemory.getConfidence()
                            : new BigDecimal("0.50"),
                    List.of(
                            "Investigation has enough evidence to plan an action."
                    ),
                    List.of(
                            "Planned next investigation action."
                    ),
                    "Execute the planned diagnostic action."
            );

            case WAITING, RESUMED -> new AgentDecision(
                    workingMemory != null
                            ? workingMemory.getCurrentHypothesis()
                            : "Continue investigation from persisted checkpoint.",
                    workingMemory != null
                            ? workingMemory.getConfidence()
                            : new BigDecimal("0.50"),
                    List.of(
                            "Investigation resumed from persisted state."
                    ),
                    List.of(
                            "Recovered investigation state."
                    ),
                    "Continue investigation."
            );

            case RESOLVED, LEARNING -> new AgentDecision(
                    workingMemory != null
                            ? workingMemory.getCurrentHypothesis()
                            : "Investigation completed.",
                    workingMemory != null
                            ? workingMemory.getConfidence()
                            : BigDecimal.ONE,
                    List.of(
                            "Investigation lifecycle is complete."
                    ),
                    List.of(
                            "Investigation completed."
                    ),
                    "No further action required."
            );
        };
    }
}
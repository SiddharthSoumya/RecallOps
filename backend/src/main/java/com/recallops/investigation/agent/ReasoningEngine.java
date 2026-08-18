package com.recallops.investigation.agent;

import com.recallops.investigation.entity.Investigation;
import com.recallops.memory.entity.WorkingMemory;

public interface ReasoningEngine {

    AgentDecision reason(
            Investigation investigation,
            WorkingMemory workingMemory
    );
}
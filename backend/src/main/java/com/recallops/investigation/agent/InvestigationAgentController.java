package com.recallops.investigation.agent;

import com.recallops.memory.dto.WorkingMemoryResponse;
import com.recallops.memory.entity.WorkingMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investigations")
@RequiredArgsConstructor
public class InvestigationAgentController {

    private final InvestigationAgentService agentService;

    @PostMapping("/{investigationId}/agent/step")
    public ResponseEntity<WorkingMemoryResponse> step(
            @PathVariable UUID investigationId
    ) {

        WorkingMemory memory =
                agentService.step(investigationId);

        WorkingMemoryResponse response =
                new WorkingMemoryResponse(
                        memory.getId(),
                        memory.getInvestigation().getId(),
                        memory.getCurrentHypothesis(),
                        memory.getConfidence(),
                        memory.getObservations(),
                        memory.getCompletedActions(),
                        memory.getNextAction(),
                        memory.getVersion(),
                        memory.getCreatedAt(),
                        memory.getUpdatedAt()
                );

        return ResponseEntity.ok(response);
    }
}
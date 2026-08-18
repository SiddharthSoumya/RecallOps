package com.recallops.memory.controller;

import com.recallops.memory.dto.UpdateWorkingMemoryRequest;
import com.recallops.memory.dto.WorkingMemoryResponse;
import com.recallops.memory.service.WorkingMemoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investigations/{investigationId}/memory")
@RequiredArgsConstructor
public class WorkingMemoryController {

    private final WorkingMemoryService workingMemoryService;

    @PostMapping
    public ResponseEntity<WorkingMemoryResponse> create(
            @PathVariable UUID investigationId,
            @Valid @RequestBody UpdateWorkingMemoryRequest request
    ) {

        return ResponseEntity
                .status(201)
                .body(
                        workingMemoryService.create(
                                investigationId,
                                request
                        )
                );
    }

    @GetMapping
    public ResponseEntity<WorkingMemoryResponse> get(
            @PathVariable UUID investigationId
    ) {

        return ResponseEntity.ok(
                workingMemoryService.getByInvestigationId(
                        investigationId
                )
        );
    }

    @PutMapping
    public ResponseEntity<WorkingMemoryResponse> update(
            @PathVariable UUID investigationId,
            @Valid @RequestBody UpdateWorkingMemoryRequest request
    ) {

        return ResponseEntity.ok(
                workingMemoryService.update(
                        investigationId,
                        request
                )
        );
    }
}
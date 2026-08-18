package com.recallops.investigation.controller;

import com.recallops.investigation.dto.InvestigationRecoveryResponse;
import com.recallops.investigation.dto.InvestigationResponse;
import com.recallops.investigation.dto.TransitionInvestigationRequest;
import com.recallops.investigation.service.InvestigationRecoveryService;
import com.recallops.investigation.service.InvestigationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InvestigationController {

    private final InvestigationService investigationService;
    private final InvestigationRecoveryService investigationRecoveryService;

    @PostMapping("/incidents/{incidentId}/investigation")
    public ResponseEntity<InvestigationResponse> startInvestigation(
            @PathVariable UUID incidentId) {

        InvestigationResponse response =
                investigationService.startInvestigation(incidentId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/investigations/{investigationId}")
    public ResponseEntity<InvestigationResponse> getInvestigation(
            @PathVariable UUID investigationId) {

        InvestigationResponse response =
                investigationService.getInvestigation(investigationId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/incidents/{incidentId}/investigation")
    public ResponseEntity<InvestigationResponse> getInvestigationByIncident(
            @PathVariable UUID incidentId) {

        InvestigationResponse response =
                investigationService.getInvestigationByIncident(incidentId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/investigations/{investigationId}/state")
    public ResponseEntity<InvestigationResponse> transition(
            @PathVariable UUID investigationId,
            @Valid @RequestBody TransitionInvestigationRequest request) {

        InvestigationResponse response =
                investigationService.transition(
                        investigationId,
                        request.targetState()
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/investigations/{investigationId}/recover")
    public ResponseEntity<InvestigationRecoveryResponse> recover(
            @PathVariable UUID investigationId
    ) {

        return ResponseEntity.ok(
                investigationRecoveryService.recover(
                        investigationId
                )
        );
    }
}
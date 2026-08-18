package com.recallops.incident.controller;

import com.recallops.incident.dto.CreateIncidentRequest;
import com.recallops.incident.dto.IncidentResponse;
import com.recallops.incident.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(
            @Valid @RequestBody CreateIncidentRequest request) {

        IncidentResponse response = incidentService.createIncident(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentResponse> getIncident(
            @PathVariable UUID incidentId) {

        IncidentResponse response = incidentService.getIncident(incidentId);

        return ResponseEntity.ok(response);
    }
}
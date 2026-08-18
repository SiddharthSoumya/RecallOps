package com.recallops.incident.dto;

import com.recallops.incident.entity.IncidentSeverity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest (
    @NotBlank
    @Size(max=200)
    String title,

    @NotBlank
    @Size(max=2000)
    String description,

    @NotBlank
    @Size(max=100)
    String affectedService,

    @NotNull
    IncidentSeverity severity
){
}

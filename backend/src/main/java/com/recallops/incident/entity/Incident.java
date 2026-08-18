package com.recallops.incident.entity;

import com.recallops.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name= "incidents",
        indexes = {
                @Index(name = "idx_incident_status", columnList = "status"),
                @Index(name = "idx_incident_severity", columnList = "severity")
        }
)
public class Incident extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable=false,length=200)
    private String title;

    @NotBlank
    @Column(nullable=false,length=2000)
    private String description;

    @NotBlank
    @Column(nullable=false,length=100)
    private String affectedService;

    @NotNull
    @Column(nullable=false,length=30)
    @Enumerated(EnumType.STRING)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.OPEN;

}

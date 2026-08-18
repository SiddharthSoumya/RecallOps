package com.recallops.investigation.entity;

import com.recallops.common.entity.BaseAuditableEntity;
import com.recallops.incident.entity.Incident;
import com.recallops.investigation.state.InvestigationState;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "investigations",
        indexes = {
                @Index(
                        name = "idx_investigation_state",
                        columnList = "current_state"
                )
        }
)
public class Investigation extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "incident_id",
            nullable = false,
            unique = true
    )
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "current_state",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private InvestigationState currentState = InvestigationState.NEW;

    @Column
    private Instant resolvedAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean resolved = false;
}
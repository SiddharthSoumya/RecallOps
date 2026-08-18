package com.recallops.investigation.entity;

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
        name = "investigation_state_history",
        indexes = {
                @Index(
                        name = "idx_state_history_investigation",
                        columnList = "investigation_id"
                ),
                @Index(
                        name = "idx_state_history_transitioned_at",
                        columnList = "transitioned_at"
                )
        }
)
public class InvestigationStateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "investigation_id",
            nullable = false
    )
    private Investigation investigation;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", nullable = false, length = 30)
    private InvestigationState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, length = 30)
    private InvestigationState toState;

    @Column(name = "transitioned_at", nullable = false)
    private Instant transitionedAt;
}
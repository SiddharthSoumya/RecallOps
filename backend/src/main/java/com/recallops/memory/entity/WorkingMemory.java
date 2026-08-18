package com.recallops.memory.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.recallops.common.entity.BaseAuditableEntity;
import com.recallops.investigation.entity.Investigation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "working_memory",
        indexes = {
                @Index(
                        name = "idx_working_memory_investigation",
                        columnList = "investigation_id"
                )
        }
)
public class WorkingMemory extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "investigation_id",
            nullable = false,
            unique = true
    )
    private Investigation investigation;

    @Column(length = 2000)
    private String currentHypothesis;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private JsonNode observations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private JsonNode completedActions;

    @Column(length = 2000)
    private String nextAction;
}
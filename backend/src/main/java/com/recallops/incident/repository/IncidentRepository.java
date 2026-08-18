package com.recallops.incident.repository;

import com.recallops.incident.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface IncidentRepository
    extends JpaRepository<Incident, UUID> {

}

package com.ziprun.engine.utils.repository;

import com.ziprun.engine.utils.domain.Agent;
import com.ziprun.engine.utils.enums.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentRepository extends JpaRepository<Agent, String> {
    List<Agent> findByStatus(AgentStatus status);
    List<Agent> findByStatusIn(List<AgentStatus> statuses);
}

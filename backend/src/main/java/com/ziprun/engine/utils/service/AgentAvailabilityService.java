package com.ziprun.engine.utils.service;

import com.ziprun.engine.utils.domain.Agent;
import com.ziprun.engine.utils.enums.AgentStatus;
import com.ziprun.engine.utils.repository.AgentRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class AgentAvailabilityService {

    private final AgentRepository agentRepository;

    public AgentAvailabilityService(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    public List<Agent> findEligibleAgents(String excludeAgentId) {
        List<Agent> agents = new ArrayList<>(agentRepository.findByStatusIn(
                Arrays.asList(AgentStatus.AVAILABLE, AgentStatus.BUSY)));

        if (excludeAgentId != null) {
            agents.removeIf(a -> a.getId().equals(excludeAgentId));
        }

        return agents.stream().filter(this::hasCapacity).toList();
    }

    public List<Agent> filterEligible(List<Agent> agents) {
        return agents.stream().filter(this::hasCapacity).toList();
    }

    private boolean hasCapacity(Agent agent) {
        Integer maxCapacity = agent.getMaxCapacity();
        if (maxCapacity == null) {
            return true;
        }
        return agent.getActiveOrderCount() < maxCapacity;
    }
}

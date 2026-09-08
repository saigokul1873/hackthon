package com.ziprun.engine.interfaces.agentcontroller.service.impl;

import com.ziprun.engine.utils.domain.Agent;
import com.ziprun.engine.utils.enums.AgentStatus;
import com.ziprun.engine.utils.event.AgentOfflineEvent;
import com.ziprun.engine.utils.repository.AgentRepository;
import com.ziprun.engine.interfaces.agentcontroller.service.AgentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {
    private final AgentRepository agentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }

    @Override
    @Transactional
    public Agent updateStatus(String id, AgentStatus newStatus) {
        Agent agent = agentRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Agent not found: " + id));
        
        if (agent.getStatus() == newStatus) {
            return agent; // Idempotent: status is already set
        }
        
        agent.setStatus(newStatus);
        Agent saved = agentRepository.save(agent);

        if (newStatus == AgentStatus.OFFLINE) {
            eventPublisher.publishEvent(new AgentOfflineEvent(this, id));
        }

        return saved;
    }
}

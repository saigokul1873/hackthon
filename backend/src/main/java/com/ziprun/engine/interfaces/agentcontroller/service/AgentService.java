package com.ziprun.engine.interfaces.agentcontroller.service;

import com.ziprun.engine.utils.domain.Agent;
import com.ziprun.engine.utils.enums.AgentStatus;
import java.util.List;

public interface AgentService {
    List<Agent> getAllAgents();
    Agent updateStatus(String id, AgentStatus newStatus);
}

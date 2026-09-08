package com.ziprun.engine.utils.event;

import org.springframework.context.ApplicationEvent;

public class AgentOfflineEvent extends ApplicationEvent {
    private final String agentId;

    public AgentOfflineEvent(Object source, String agentId) {
        super(source);
        this.agentId = agentId;
    }

    public String getAgentId() {
        return agentId;
    }
}

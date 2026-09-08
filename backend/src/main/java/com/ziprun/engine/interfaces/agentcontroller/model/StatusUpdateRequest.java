package com.ziprun.engine.interfaces.agentcontroller.model;

import com.ziprun.engine.utils.enums.AgentStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
    @NotNull(message = "Status cannot be null") 
    AgentStatus status
) {}

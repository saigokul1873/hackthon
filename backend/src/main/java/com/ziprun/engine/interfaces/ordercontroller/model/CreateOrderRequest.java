package com.ziprun.engine.interfaces.ordercontroller.model;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
    @NotBlank(message = "Description cannot be blank")
    String description,
    
    @NotBlank(message = "Assigned Agent ID is required")
    String assignedAgentId
) {}

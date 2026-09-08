package com.ziprun.engine.interfaces.routingcontroller.model;

import jakarta.validation.constraints.NotBlank;

public record StrategyUpdateRequest(@NotBlank String strategy) {
}

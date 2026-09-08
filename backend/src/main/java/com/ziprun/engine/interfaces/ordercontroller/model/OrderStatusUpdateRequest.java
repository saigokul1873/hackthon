package com.ziprun.engine.interfaces.ordercontroller.model;

import com.ziprun.engine.utils.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(@NotNull OrderStatus status) {
}

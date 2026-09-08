package com.ziprun.engine.interfaces.ordercontroller.service;

import com.ziprun.engine.interfaces.ordercontroller.model.CreateOrderRequest;
import com.ziprun.engine.interfaces.ordercontroller.model.OrderStatusUpdateRequest;
import com.ziprun.engine.utils.domain.Order;
import com.ziprun.engine.utils.domain.ReassignmentSuggestion;
import com.ziprun.engine.utils.enums.OrderStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface OrderService {
    List<Order> getOrders(OrderStatus status);
    Order createOrder(CreateOrderRequest request);
    ReassignmentSuggestion suggestForOrder(String orderId);
    SseEmitter streamSuggestion(String orderId);
    Order updateOrderStatus(String orderId, OrderStatusUpdateRequest request);
}

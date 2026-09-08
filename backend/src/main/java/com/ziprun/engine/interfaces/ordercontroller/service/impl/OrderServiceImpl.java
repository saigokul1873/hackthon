package com.ziprun.engine.interfaces.ordercontroller.service.impl;

import com.ziprun.engine.interfaces.ordercontroller.model.CreateOrderRequest;
import com.ziprun.engine.interfaces.ordercontroller.model.OrderStatusUpdateRequest;
import com.ziprun.engine.utils.domain.Agent;
import com.ziprun.engine.utils.domain.Order;
import com.ziprun.engine.utils.domain.ReassignmentSuggestion;
import com.ziprun.engine.utils.enums.AgentStatus;
import com.ziprun.engine.utils.enums.OrderStatus;
import com.ziprun.engine.utils.enums.SuggestionStatus;
import com.ziprun.engine.utils.enums.TriggerReason;
import com.ziprun.engine.utils.repository.AgentRepository;
import com.ziprun.engine.utils.repository.OrderRepository;
import com.ziprun.engine.utils.repository.SuggestionRepository;
import com.ziprun.engine.utils.service.RoutingService;
import com.ziprun.engine.utils.service.AgentAvailabilityService;
import com.ziprun.engine.exception.ZycusErrorCode;
import com.ziprun.engine.interfaces.ordercontroller.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;
    private final RoutingService routingService;
    private final SuggestionRepository suggestionRepository;
    private final AgentAvailabilityService agentAvailabilityService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public List<Order> getOrders(OrderStatus status) {
        if (status == null) {
            return orderRepository.findAll();
        }
        return orderRepository.findByStatus(status);
    }

    @Override
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Agent agent = agentRepository.findById(request.assignedAgentId())
                .orElseThrow(() -> ZycusErrorCode.AGENT_NOT_FOUND.exception(request.assignedAgentId()));

        if (agent.getStatus() == AgentStatus.OFFLINE) {
            throw ZycusErrorCode.AGENT_OFFLINE.exception(agent.getId());
        }

        Order order = new Order();
        order.setId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setDescription(request.description());
        order.setAssignedAgent(agent);
        order.setStatus(OrderStatus.ASSIGNED);
        order.setCreatedAt(LocalDateTime.now());

        agent.setActiveOrderCount(agent.getActiveOrderCount() + 1);
        if (agent.getStatus() == AgentStatus.AVAILABLE) {
            agent.setStatus(AgentStatus.BUSY);
        }
        agentRepository.save(agent);

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public ReassignmentSuggestion suggestForOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ZycusErrorCode.ORDER_NOT_FOUND.exception(orderId));

        if (order.getStatus() != OrderStatus.ASSIGNED && order.getStatus() != OrderStatus.REASSIGNMENT_PENDING) {
            throw ZycusErrorCode.ORDER_NOT_ELIGIBLE.exception(order.getStatus().name());
        }

        var existing = suggestionRepository.findFirstByOrderIdAndStatusAndTriggerReason(
                orderId, SuggestionStatus.PENDING, TriggerReason.INITIAL);
        if (existing.isPresent()) {
            return existing.get();
        }

        List<Agent> availableAgents = agentAvailabilityService.findEligibleAgents(
                order.getAssignedAgent() != null ? order.getAssignedAgent().getId() : null);

        List<ReassignmentSuggestion> suggestions = routingService.suggestReassignment(
                order, availableAgents, TriggerReason.INITIAL);
        if (suggestions == null || suggestions.isEmpty() || suggestions.get(0).getRecommendedAgent() == null) {
            throw ZycusErrorCode.ROUTING_NO_AGENT.exception();
        }

        ReassignmentSuggestion best = suggestions.get(0);
        order.setStatus(OrderStatus.REASSIGNMENT_PENDING);
        orderRepository.save(order);
        return suggestionRepository.save(best);
    }

    @Override
    public SseEmitter streamSuggestion(String orderId) {
        SseEmitter emitter = new SseEmitter(120000L);

        executor.execute(() -> {
            try {
                ReassignmentSuggestion suggestion = suggestForOrder(orderId);
                String reasoning = suggestion.getReasoning();

                if (reasoning != null) {
                    String[] words = reasoning.split(" ");
                    for (String word : words) {
                        emitter.send(SseEmitter.event().data(word + " "));
                        Thread.sleep(50);
                    }
                }

                emitter.send(SseEmitter.event().name("complete").data("done"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @Override
    @Transactional
    public Order updateOrderStatus(String orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ZycusErrorCode.ORDER_NOT_FOUND.exception(orderId));

        if (request.status() == OrderStatus.DELIVERED) {
            if (order.getStatus() != OrderStatus.REASSIGNED) {
                throw ZycusErrorCode.ORDER_INVALID_STATUS.exception(
                        "Only REASSIGNED orders can be marked DELIVERED");
            }
            Agent agent = order.getAssignedAgent();
            if (agent != null) {
                agent.setActiveOrderCount(Math.max(0, agent.getActiveOrderCount() - 1));
                if (agent.getActiveOrderCount() == 0 && agent.getStatus() == AgentStatus.BUSY) {
                    agent.setStatus(AgentStatus.AVAILABLE);
                }
                agentRepository.save(agent);
            }
            order.setStatus(OrderStatus.DELIVERED);
            return orderRepository.save(order);
        }

        throw ZycusErrorCode.ORDER_INVALID_STATUS.exception("Unsupported status: " + request.status());
    }
}

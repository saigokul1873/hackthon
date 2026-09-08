package com.ziprun.engine.interfaces.ordercontroller.service.impl;

import com.ziprun.engine.interfaces.ordercontroller.model.CreateOrderRequest;
import com.ziprun.engine.utils.domain.Agent;
import com.ziprun.engine.utils.domain.Order;
import com.ziprun.engine.utils.domain.ReassignmentSuggestion;
import com.ziprun.engine.utils.enums.AgentStatus;
import com.ziprun.engine.utils.enums.OrderStatus;
import com.ziprun.engine.utils.enums.TriggerReason;
import com.ziprun.engine.utils.repository.AgentRepository;
import com.ziprun.engine.utils.repository.OrderRepository;
import com.ziprun.engine.utils.repository.SuggestionRepository;
import com.ziprun.engine.utils.service.RoutingService;
import com.ziprun.engine.interfaces.ordercontroller.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
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
                .orElseThrow(() -> new NoSuchElementException("Agent not found: " + request.assignedAgentId()));

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
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
                
        List<Agent> availableAgents = agentRepository.findByStatusIn(Arrays.asList(AgentStatus.AVAILABLE, AgentStatus.BUSY));
        if (order.getAssignedAgent() != null) {
            availableAgents.removeIf(a -> a.getId().equals(order.getAssignedAgent().getId()));
        }

        List<ReassignmentSuggestion> suggestions = routingService.suggestReassignment(order, availableAgents, TriggerReason.INITIAL);
        if (suggestions == null || suggestions.isEmpty() || suggestions.get(0).getRecommendedAgent() == null) {
            throw new IllegalStateException("Routing strategy could not find a suitable agent.");
        }
        
        ReassignmentSuggestion best = suggestions.get(0);
        order.setStatus(OrderStatus.REASSIGNMENT_PENDING);
        orderRepository.save(order);
        return suggestionRepository.save(best);
    }

    @Override
    public SseEmitter streamSuggestion(String orderId) {
        SseEmitter emitter = new SseEmitter(120000L); // 2 min timeout
        
        executor.execute(() -> {
            try {
                ReassignmentSuggestion suggestion = suggestForOrder(orderId);
                String reasoning = suggestion.getReasoning();
                
                // Simulate SSE token stream
                if (reasoning != null) {
                    String[] words = reasoning.split(" ");
                    for (String word : words) {
                        emitter.send(word + " ");
                        Thread.sleep(50);
                    }
                }
                
                emitter.send(SseEmitter.event().name("complete").data(suggestion));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
}

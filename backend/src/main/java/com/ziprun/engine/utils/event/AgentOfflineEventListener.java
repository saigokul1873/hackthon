package com.ziprun.engine.utils.event;

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
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AgentOfflineEventListener {

    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;
    private final SuggestionRepository suggestionRepository;
    private final RoutingService routingService;

    @Async
    @EventListener
    @Transactional
    public void handleAgentOfflineEvent(AgentOfflineEvent event) {
        String offlineAgentId = event.getAgentId();
        
        List<Order> affectedOrders = orderRepository.findByAssignedAgentId(offlineAgentId);
        if (affectedOrders.isEmpty()) return;

        List<Agent> availableAgents = agentRepository.findByStatusIn(Arrays.asList(AgentStatus.AVAILABLE, AgentStatus.BUSY));
        availableAgents.removeIf(a -> a.getId().equals(offlineAgentId));

        for (Order order : affectedOrders) {
            if (order.getStatus() != OrderStatus.ASSIGNED) continue;

            boolean alreadyPending = suggestionRepository.existsByOrderIdAndStatusAndTriggerReason(
                    order.getId(), SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE);

            if (alreadyPending) continue;

            order.setStatus(OrderStatus.REASSIGNMENT_PENDING);
            orderRepository.save(order);

            List<ReassignmentSuggestion> suggestions = routingService.suggestReassignment(order, availableAgents, TriggerReason.AGENT_OFFLINE);
            if (suggestions != null && !suggestions.isEmpty() && suggestions.get(0).getRecommendedAgent() != null) {
                suggestionRepository.save(suggestions.get(0));
            }
        }
    }
}

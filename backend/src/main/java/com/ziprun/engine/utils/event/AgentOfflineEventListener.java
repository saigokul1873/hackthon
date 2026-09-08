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
import com.ziprun.engine.utils.routing.RoutingContext;
import com.ziprun.engine.utils.service.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AgentOfflineEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgentOfflineEventListener.class);

    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;
    private final SuggestionRepository suggestionRepository;
    private final RoutingService routingService;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAgentOfflineEvent(AgentOfflineEvent event) {
        String offlineAgentId = event.getAgentId();
        log.info("Agentic re-plan triggered for offline agent: {}", offlineAgentId);

        List<Order> affectedOrders = orderRepository.findByAssignedAgentId(offlineAgentId)
                .stream()
                .filter(o -> o.getStatus() == OrderStatus.ASSIGNED)
                .toList();

        if (affectedOrders.isEmpty()) {
            log.info("No ASSIGNED orders found for offline agent {}", offlineAgentId);
            return;
        }

        List<Agent> availableAgents = agentRepository.findByStatusIn(
                Arrays.asList(AgentStatus.AVAILABLE, AgentStatus.BUSY));
        availableAgents.removeIf(a -> a.getId().equals(offlineAgentId));

        RoutingContext context = RoutingContext.forOfflineAgent(offlineAgentId, affectedOrders.size());

        for (Order order : affectedOrders) {
            try {
                processOrder(order, availableAgents, context);
            } catch (Exception e) {
                log.error("Re-plan failed for order {}: {}", order.getId(), e.getMessage(), e);
            }
        }
    }

    private void processOrder(Order order, List<Agent> availableAgents, RoutingContext context) {
        boolean alreadyPending = suggestionRepository.existsByOrderIdAndStatusAndTriggerReason(
                order.getId(), SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE);

        if (alreadyPending) {
            log.debug("Skipping duplicate re-plan suggestion for order {}", order.getId());
            return;
        }

        order.setStatus(OrderStatus.REASSIGNMENT_PENDING);
        orderRepository.save(order);

        List<ReassignmentSuggestion> suggestions = routingService.suggestReassignment(
                order, availableAgents, TriggerReason.AGENT_OFFLINE, context);

        if (suggestions == null || suggestions.isEmpty()) {
            log.warn("No suggestion produced for stranded order {}", order.getId());
            return;
        }

        ReassignmentSuggestion best = suggestions.get(0);
        if (best.getRecommendedAgent() == null) {
            log.warn("Routing could not find agent for order {}", order.getId());
            return;
        }

        suggestionRepository.save(best);
        log.info("Queued AGENT_OFFLINE suggestion for order {} -> agent {}",
                order.getId(), best.getRecommendedAgent().getId());
    }
}

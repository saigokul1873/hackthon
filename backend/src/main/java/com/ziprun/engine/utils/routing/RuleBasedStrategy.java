package com.ziprun.engine.utils.routing;

import com.ziprun.engine.utils.domain.Agent;
import com.ziprun.engine.utils.domain.Order;
import com.ziprun.engine.utils.domain.ReassignmentSuggestion;
import com.ziprun.engine.utils.enums.SuggestionStatus;
import com.ziprun.engine.utils.enums.TriggerReason;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component("ruleBased")
public class RuleBasedStrategy implements RoutingStrategy {

    @Override
    public List<ReassignmentSuggestion> suggest(Order order, List<Agent> availableAgents, TriggerReason triggerReason) {
        return suggest(order, availableAgents, triggerReason, RoutingContext.empty());
    }

    @Override
    public List<ReassignmentSuggestion> suggest(Order order, List<Agent> availableAgents,
                                                TriggerReason triggerReason, RoutingContext context) {
        if (availableAgents == null || availableAgents.isEmpty()) {
            return Collections.singletonList(new ReassignmentSuggestion(
                    order, null, 0.0,
                    "No agents available for reassignment.",
                    SuggestionStatus.PENDING,
                    triggerReason
            ));
        }

        return availableAgents.stream()
                .filter(agent -> agent.getMaxCapacity() == null
                        || agent.getActiveOrderCount() < agent.getMaxCapacity())
                .sorted(Comparator.comparingInt(Agent::getActiveOrderCount))
                .map(agent -> new ReassignmentSuggestion(
                        order,
                        agent,
                        1.0,
                        "Rule-based: Picked agent with fewest active orders (" + agent.getActiveOrderCount() + ").",
                        SuggestionStatus.PENDING,
                        triggerReason
                ))
                .collect(Collectors.toList());
    }
}

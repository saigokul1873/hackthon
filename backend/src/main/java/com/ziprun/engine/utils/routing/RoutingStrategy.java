package com.ziprun.engine.utils.routing;

import com.ziprun.engine.utils.domain.Agent;
import com.ziprun.engine.utils.domain.Order;
import com.ziprun.engine.utils.domain.ReassignmentSuggestion;
import com.ziprun.engine.utils.enums.TriggerReason;

import java.util.List;

public interface RoutingStrategy {

    List<ReassignmentSuggestion> suggest(Order order, List<Agent> availableAgents, TriggerReason triggerReason);

    default List<ReassignmentSuggestion> suggest(Order order, List<Agent> availableAgents,
                                                  TriggerReason triggerReason, RoutingContext context) {
        return suggest(order, availableAgents, triggerReason);
    }
}

package com.ziprun.engine.utils.service;

import com.ziprun.engine.utils.domain.Agent;
import com.ziprun.engine.utils.domain.Order;
import com.ziprun.engine.utils.domain.ReassignmentSuggestion;
import com.ziprun.engine.utils.enums.TriggerReason;
import com.ziprun.engine.utils.routing.RoutingStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RoutingService {

    private final Map<String, RoutingStrategy> strategies;
    private final String activeStrategyName;

    public RoutingService(Map<String, RoutingStrategy> strategies,
                          @Value("${routing.active-strategy:ruleBased}") String activeStrategyName) {
        this.strategies = strategies;
        this.activeStrategyName = activeStrategyName;
        
        if (!strategies.containsKey(activeStrategyName)) {
            throw new IllegalStateException("Configured routing strategy not found: " + activeStrategyName);
        }
    }

    public List<ReassignmentSuggestion> suggestReassignment(Order order, List<Agent> availableAgents, TriggerReason triggerReason) {
        RoutingStrategy strategy = strategies.get(activeStrategyName);
        return strategy.suggest(order, availableAgents, triggerReason);
    }
}

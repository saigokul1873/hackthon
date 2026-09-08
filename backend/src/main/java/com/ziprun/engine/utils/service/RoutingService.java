package com.ziprun.engine.utils.service;

import com.ziprun.engine.utils.domain.Agent;
import com.ziprun.engine.utils.domain.Order;
import com.ziprun.engine.utils.domain.ReassignmentSuggestion;
import com.ziprun.engine.utils.enums.TriggerReason;
import com.ziprun.engine.exception.ZycusErrorCode;
import com.ziprun.engine.utils.routing.RoutingContext;
import com.ziprun.engine.utils.routing.RoutingStrategy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RoutingService {

    private static final String DEFAULT_STRATEGY = "ruleBased";

    private final Map<String, RoutingStrategy> strategies;
    private final Environment environment;

    public RoutingService(Map<String, RoutingStrategy> strategies, Environment environment) {
        this.strategies = strategies;
        this.environment = environment;
        validateActiveStrategyName(resolveActiveStrategyName());
    }

    public List<ReassignmentSuggestion> suggestReassignment(Order order, List<Agent> availableAgents,
                                                            TriggerReason triggerReason) {
        return suggestReassignment(order, availableAgents, triggerReason, RoutingContext.empty());
    }

    public List<ReassignmentSuggestion> suggestReassignment(Order order, List<Agent> availableAgents,
                                                            TriggerReason triggerReason, RoutingContext context) {
        String strategyName = resolveActiveStrategyName();
        RoutingStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            throw ZycusErrorCode.ROUTING_STRATEGY_NOT_FOUND.exception(strategyName);
        }
        return strategy.suggest(order, availableAgents, triggerReason, context);
    }

    public String getActiveStrategyName() {
        return resolveActiveStrategyName();
    }

    private String resolveActiveStrategyName() {
        String strategy = environment.getProperty("routing.strategy");
        if (strategy == null || strategy.isBlank()) {
            strategy = environment.getProperty("routing.active-strategy", DEFAULT_STRATEGY);
        }
        return strategy;
    }

    private void validateActiveStrategyName(String strategyName) {
        if (!strategies.containsKey(strategyName)) {
            throw ZycusErrorCode.ROUTING_STRATEGY_NOT_FOUND.exception(
                    strategyName + ". Available: " + strategies.keySet());
        }
    }
}

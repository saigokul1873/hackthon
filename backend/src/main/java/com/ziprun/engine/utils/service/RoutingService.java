package com.ziprun.engine.utils.service;

import com.ziprun.engine.utils.domain.Agent;
import com.ziprun.engine.utils.domain.Order;
import com.ziprun.engine.utils.domain.ReassignmentSuggestion;
import com.ziprun.engine.utils.enums.TriggerReason;
import com.ziprun.engine.exception.ZycusErrorCode;
import com.ziprun.engine.utils.routing.RoutingContext;
import com.ziprun.engine.utils.routing.RoutingStrategy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RoutingService {

    private final Map<String, RoutingStrategy> strategies;
    private final RoutingStrategyHolder strategyHolder;

    public RoutingService(Map<String, RoutingStrategy> strategies, RoutingStrategyHolder strategyHolder) {
        this.strategies = strategies;
        this.strategyHolder = strategyHolder;
    }

    public List<ReassignmentSuggestion> suggestReassignment(Order order, List<Agent> availableAgents,
                                                            TriggerReason triggerReason) {
        return suggestReassignment(order, availableAgents, triggerReason, RoutingContext.empty());
    }

    public List<ReassignmentSuggestion> suggestReassignment(Order order, List<Agent> availableAgents,
                                                            TriggerReason triggerReason, RoutingContext context) {
        String strategyName = strategyHolder.getActiveStrategy();
        RoutingStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            throw ZycusErrorCode.ROUTING_STRATEGY_NOT_FOUND.exception(strategyName);
        }
        return strategy.suggest(order, availableAgents, triggerReason, context);
    }

    public String getActiveStrategyName() {
        return strategyHolder.getActiveStrategy();
    }

    public void setActiveStrategy(String strategyName) {
        strategyHolder.setActiveStrategy(strategyName);
    }
}

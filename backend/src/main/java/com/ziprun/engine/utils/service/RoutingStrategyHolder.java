package com.ziprun.engine.utils.service;

import com.ziprun.engine.exception.ZycusErrorCode;
import com.ziprun.engine.utils.routing.RoutingStrategy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RoutingStrategyHolder {

    private static final String DEFAULT_STRATEGY = "ruleBased";

    private final Environment environment;
    private final Map<String, RoutingStrategy> strategies;
    private volatile String runtimeOverride;

    public RoutingStrategyHolder(Environment environment, Map<String, RoutingStrategy> strategies) {
        this.environment = environment;
        this.strategies = strategies;
        validateStrategyName(resolveFromConfig());
    }

    public String getActiveStrategy() {
        if (runtimeOverride != null && !runtimeOverride.isBlank()) {
            return runtimeOverride;
        }
        return resolveFromConfig();
    }

    public void setActiveStrategy(String strategyName) {
        validateStrategyName(strategyName);
        this.runtimeOverride = strategyName;
    }

    public Map<String, RoutingStrategy> getStrategies() {
        return strategies;
    }

    private String resolveFromConfig() {
        String strategy = environment.getProperty("routing.strategy");
        if (strategy == null || strategy.isBlank()) {
            strategy = environment.getProperty("routing.active-strategy", DEFAULT_STRATEGY);
        }
        return strategy;
    }

    private void validateStrategyName(String strategyName) {
        if (!strategies.containsKey(strategyName)) {
            throw ZycusErrorCode.ROUTING_STRATEGY_NOT_FOUND.exception(
                    strategyName + ". Available: " + strategies.keySet());
        }
    }
}

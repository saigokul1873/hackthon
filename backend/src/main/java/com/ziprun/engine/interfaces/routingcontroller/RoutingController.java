package com.ziprun.engine.interfaces.routingcontroller;

import com.ziprun.engine.utils.routing.RoutingStrategy;
import com.ziprun.engine.utils.service.RoutingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/routing")
public class RoutingController {

    private final RoutingService routingService;
    private final Map<String, RoutingStrategy> strategies;

    public RoutingController(RoutingService routingService, Map<String, RoutingStrategy> strategies) {
        this.routingService = routingService;
        this.strategies = strategies;
    }

    @GetMapping("/strategy")
    public Map<String, Object> getActiveStrategy() {
        Set<String> available = strategies.keySet();
        return Map.of(
                "activeStrategy", routingService.getActiveStrategyName(),
                "availableStrategies", available
        );
    }
}

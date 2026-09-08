package com.ziprun.engine.interfaces.routingcontroller;

import com.ziprun.engine.interfaces.routingcontroller.model.StrategyUpdateRequest;
import com.ziprun.engine.utils.service.RoutingService;
import com.ziprun.engine.utils.service.RoutingStrategyHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/routing")
public class RoutingController {

    private final RoutingService routingService;
    private final RoutingStrategyHolder strategyHolder;

    public RoutingController(RoutingService routingService, RoutingStrategyHolder strategyHolder) {
        this.routingService = routingService;
        this.strategyHolder = strategyHolder;
    }

    @GetMapping("/strategy")
    public Map<String, Object> getActiveStrategy() {
        return Map.of(
                "activeStrategy", routingService.getActiveStrategyName(),
                "availableStrategies", strategyHolder.getStrategies().keySet()
        );
    }

    @PatchMapping("/strategy")
    public Map<String, Object> updateActiveStrategy(@Valid @RequestBody StrategyUpdateRequest request) {
        routingService.setActiveStrategy(request.strategy());
        return Map.of(
                "activeStrategy", routingService.getActiveStrategyName(),
                "message", "Routing strategy switched at runtime without restart"
        );
    }
}

package com.ziprun.engine.utils.routing;

public record RoutingContext(String offlineAgentId, int strandedOrderCount) {

    public static RoutingContext empty() {
        return new RoutingContext(null, 0);
    }

    public static RoutingContext forOfflineAgent(String offlineAgentId, int strandedOrderCount) {
        return new RoutingContext(offlineAgentId, strandedOrderCount);
    }
}

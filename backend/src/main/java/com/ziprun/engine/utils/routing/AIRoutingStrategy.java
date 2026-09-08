package com.ziprun.engine.utils.routing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziprun.engine.ai.LLMGateway;
import com.ziprun.engine.utils.domain.Agent;
import com.ziprun.engine.utils.domain.Order;
import com.ziprun.engine.utils.domain.ReassignmentSuggestion;
import com.ziprun.engine.utils.enums.SuggestionStatus;
import com.ziprun.engine.utils.enums.TriggerReason;
import com.ziprun.engine.exception.ZycusErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component("ai")
@RequiredArgsConstructor
public class AIRoutingStrategy implements RoutingStrategy {

    private static final Logger log = LoggerFactory.getLogger(AIRoutingStrategy.class);

    private final LLMGateway llmGateway;
    private final RuleBasedStrategy ruleBasedStrategy;
    private final ObjectMapper objectMapper;

    @Override
    public List<ReassignmentSuggestion> suggest(Order order, List<Agent> availableAgents, TriggerReason triggerReason) {
        return suggest(order, availableAgents, triggerReason, RoutingContext.empty());
    }

    @Override
    public List<ReassignmentSuggestion> suggest(Order order, List<Agent> availableAgents,
                                                TriggerReason triggerReason, RoutingContext context) {
        if (availableAgents == null || availableAgents.isEmpty()) {
            return ruleBasedStrategy.suggest(order, availableAgents, triggerReason, context);
        }

        try {
            String prompt = triggerReason == TriggerReason.AGENT_OFFLINE
                    ? buildRecoveryPrompt(order, availableAgents, context)
                    : buildInitialPrompt(order, availableAgents);

            log.info("AI routing for order {} trigger={}", order.getId(), triggerReason);
            String response = llmGateway.callLLM(prompt);
            LlmRecommendation recommendation = parseResponse(response);

            String agentId = recommendation.resolvedAgentId();
            if (agentId == null || agentId.isBlank()) {
                throw ZycusErrorCode.LLM_INVALID_AGENT.exception();
            }

            Agent selectedAgent = availableAgents.stream()
                    .filter(a -> a.getId().equals(agentId))
                    .findFirst()
                    .orElseThrow(() -> ZycusErrorCode.LLM_HALLUCINATED_AGENT.exception(agentId));

            log.info("AI recommended agent {} for order {} confidence={}", agentId, order.getId(), recommendation.resolvedConfidence());

            return Collections.singletonList(new ReassignmentSuggestion(
                    order,
                    selectedAgent,
                    recommendation.resolvedConfidence(),
                    recommendation.reasoning(),
                    SuggestionStatus.PENDING,
                    triggerReason
            ));

        } catch (Exception e) {
            log.warn("AI routing failed for order {}: {}. Falling back to rule-based.", order.getId(), e.getMessage());
            List<ReassignmentSuggestion> fallbacks = ruleBasedStrategy.suggest(order, availableAgents, triggerReason, context);
            if (!fallbacks.isEmpty()) {
                ReassignmentSuggestion fallback = fallbacks.get(0);
                fallback.setReasoning("AI unavailable — rule-based fallback: " + fallback.getReasoning());
            }
            return fallbacks;
        }
    }

    private String buildInitialPrompt(Order order, List<Agent> availableAgents) {
        return "You are an AI logistics dispatcher for ZipRun delivery.\n\n"
                + "SITUATION: Initial manual reassignment request. An ops manager needs the best available agent "
                + "to take over this order. Consider each agent's current load.\n\n"
                + "ORDER TO ASSIGN:\n"
                + "- ID: " + order.getId() + "\n"
                + "- Description: " + order.getDescription() + "\n"
                + "- Current status: " + order.getStatus() + "\n\n"
                + "AVAILABLE AGENT ROSTER:\n" + formatAgentsList(availableAgents) + "\n\n"
                + "Return ONLY a JSON object (no markdown) with these keys:\n"
                + "  agentId (string) — must be one of the agent IDs listed above\n"
                + "  confidence (number 0.0-1.0)\n"
                + "  reasoning (string) — plain English explanation for ops\n\n"
                + "Example: {\"agentId\":\"AGT-002\",\"confidence\":0.85,\"reasoning\":\"Rahul has zero active orders.\"}";
    }

    private String buildRecoveryPrompt(Order order, List<Agent> availableAgents, RoutingContext context) {
        String failedAgentLine = "Unknown (agent record unavailable)";
        if (order.getAssignedAgent() != null) {
            Agent failed = order.getAssignedAgent();
            failedAgentLine = failed.getId() + " (" + failed.getName() + ") — status OFFLINE";
        } else if (context.offlineAgentId() != null) {
            failedAgentLine = context.offlineAgentId() + " — status OFFLINE";
        }

        int strandedCount = context.strandedOrderCount() > 0 ? context.strandedOrderCount() : 1;

        return "You are an AI logistics dispatcher for ZipRun delivery.\n\n"
                + "SITUATION REPORT — RECOVERY MODE\n"
                + "A delivery agent has gone OFFLINE mid-shift. Their assigned orders are now STRANDED.\n"
                + "Previous assignments to the failed agent are VOID. You must recommend a recovery agent immediately.\n\n"
                + "FAILURE DETAILS:\n"
                + "- Failed agent: " + failedAgentLine + "\n"
                + "- Total stranded orders from this agent: " + strandedCount + "\n"
                + "- This recovery request is for order: " + order.getId() + "\n\n"
                + "STRANDED ORDER:\n"
                + "- ID: " + order.getId() + "\n"
                + "- Description: " + order.getDescription() + "\n\n"
                + "AVAILABLE AGENTS FOR RECOVERY (failed agent excluded):\n"
                + formatAgentsList(availableAgents) + "\n\n"
                + "Prioritize agents with lowest load to balance the fleet during recovery.\n"
                + "Return ONLY a JSON object (no markdown) with these keys:\n"
                + "  agentId (string) — must be one of the agent IDs listed above\n"
                + "  confidence (number 0.0-1.0)\n"
                + "  reasoning (string) — explain the recovery decision for ops\n\n"
                + "Example: {\"agentId\":\"AGT-004\",\"confidence\":0.9,"
                + "\"reasoning\":\"Kiran is available with no active orders — ideal for urgent recovery.\"}";
    }

    private String formatAgentsList(List<Agent> agents) {
        return agents.stream()
                .map(a -> "- " + a.getId() + " | " + a.getName()
                        + " | status: " + a.getStatus()
                        + " | active orders: " + a.getActiveOrderCount()
                        + (a.getZoneId() != null ? " | zone: " + a.getZoneId() : ""))
                .collect(Collectors.joining("\n"));
    }

    private LlmRecommendation parseResponse(String raw) throws Exception {
        String json = extractJson(raw);
        return objectMapper.readValue(json, LlmRecommendation.class);
    }

    private String extractJson(String raw) {
        if (raw == null) {
            throw ZycusErrorCode.LLM_EMPTY_RESPONSE.exception();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LlmRecommendation(
            @JsonProperty("agentId") String agentId,
            @JsonProperty("recommendedAgentId") String recommendedAgentId,
            @JsonProperty("confidence") Double confidence,
            @JsonProperty("confidenceScore") Double confidenceScore,
            @JsonProperty("reasoning") String reasoning
    ) {
        String resolvedAgentId() {
            if (agentId != null && !agentId.isBlank()) return agentId;
            return recommendedAgentId;
        }

        double resolvedConfidence() {
            if (confidence != null) return Math.min(1.0, Math.max(0.0, confidence));
            if (confidenceScore != null) return Math.min(1.0, Math.max(0.0, confidenceScore));
            return 0.5;
        }
    }
}

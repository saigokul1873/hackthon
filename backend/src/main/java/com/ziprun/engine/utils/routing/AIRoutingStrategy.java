package com.ziprun.engine.utils.routing;

import com.ziprun.engine.ai.LLMGateway;
import com.ziprun.engine.utils.domain.Agent;
import com.ziprun.engine.utils.domain.Order;
import com.ziprun.engine.utils.domain.ReassignmentSuggestion;
import com.ziprun.engine.utils.enums.SuggestionStatus;
import com.ziprun.engine.utils.enums.TriggerReason;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

@Component("ai")
@RequiredArgsConstructor
public class AIRoutingStrategy implements RoutingStrategy {

    private final LLMGateway llmGateway;
    private final RuleBasedStrategy ruleBasedStrategy;

    @Override
    public List<ReassignmentSuggestion> suggest(Order order, List<Agent> availableAgents, TriggerReason triggerReason) {
        if (availableAgents == null || availableAgents.isEmpty()) {
            return ruleBasedStrategy.suggest(order, availableAgents, triggerReason);
        }

        try {
            String prompt = triggerReason == TriggerReason.AGENT_OFFLINE 
                ? buildRecoveryPrompt(order, availableAgents) 
                : buildInitialPrompt(order, availableAgents);
                
            String response = llmGateway.callLLM(prompt);
            
            String recommendedAgentId = extractValue(response, "recommendedAgentId");
            String reasoning = extractValue(response, "reasoning");
            String scoreStr = extractValue(response, "confidenceScore");
            Double confidenceScore = scoreStr != null ? Double.parseDouble(scoreStr) : 0.0;
            
            Agent selectedAgent = availableAgents.stream()
                    .filter(a -> a.getId().equals(recommendedAgentId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Hallucinated Agent ID: " + recommendedAgentId));

            return Collections.singletonList(new ReassignmentSuggestion(
                    order,
                    selectedAgent,
                    confidenceScore,
                    reasoning,
                    SuggestionStatus.PENDING,
                    triggerReason
            ));

        } catch (Exception e) {
            System.err.println("AI Routing failed: " + e.getMessage() + ". Falling back to rule-based.");
            List<ReassignmentSuggestion> fallbacks = ruleBasedStrategy.suggest(order, availableAgents, triggerReason);
            if (!fallbacks.isEmpty()) {
                fallbacks.get(0).setReasoning("AI Fallback: " + fallbacks.get(0).getReasoning());
            }
            return fallbacks;
        }
    }

    private String buildInitialPrompt(Order order, List<Agent> availableAgents) {
        String agentsList = formatAgentsList(availableAgents);
        return "You are an AI logistics dispatcher.\n" +
                "Context: This is an initial manual assignment request. Find the best available agent to take this newly created order.\n" +
                "Order to Assign: " + order.getId() + " - " + order.getDescription() + "\n" +
                "Available Agents:\n" + agentsList + "\n\n" +
                "Analyze the agent loads and recommend the best agent. Return strictly a JSON object with no markdown formatting.\n" +
                "Keys required: recommendedAgentId (string), confidenceScore (number 0.0-1.0), reasoning (string explanation).";
    }

    private String buildRecoveryPrompt(Order order, List<Agent> availableAgents) {
        String agentsList = formatAgentsList(availableAgents);
        return "You are an AI logistics dispatcher in RECOVERY MODE.\n" +
                "Context: CRITICAL EVENT - An agent just went OFFLINE abruptly. Their orders are now STRANDED. We need to recover and reassign this stranded order immediately to the best available agent to prevent SLA breaches. Do not consider previous assignments.\n" +
                "Stranded Order to Reassign: " + order.getId() + " - " + order.getDescription() + "\n" +
                "Currently Available Agents for Recovery:\n" + agentsList + "\n\n" +
                "Analyze the situation and recommend the best agent for recovery. Return strictly a JSON object with no markdown formatting.\n" +
                "Keys required: recommendedAgentId (string), confidenceScore (number 0.0-1.0), reasoning (string explanation).";
    }

    private String formatAgentsList(List<Agent> agents) {
        return agents.stream()
                .map(a -> a.getId() + " (" + a.getName() + ") - Active Orders: " + a.getActiveOrderCount() + (a.getZoneId() != null ? " Zone: " + a.getZoneId() : ""))
                .collect(Collectors.joining("\n"));
    }

    private String extractValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"?([^\",}]+)\"?";
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
            if (m.find()) {
                return m.group(1).trim();
            }
        } catch(Exception ignored){}
        return null;
    }
}

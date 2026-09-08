package com.ziprun.engine.interfaces.suggestioncontroller.service.impl;

import com.ziprun.engine.utils.domain.Agent;
import com.ziprun.engine.utils.domain.Order;
import com.ziprun.engine.utils.domain.ReassignmentSuggestion;
import com.ziprun.engine.utils.enums.AgentStatus;
import com.ziprun.engine.utils.enums.OrderStatus;
import com.ziprun.engine.utils.enums.SuggestionStatus;
import com.ziprun.engine.utils.repository.AgentRepository;
import com.ziprun.engine.utils.repository.OrderRepository;
import com.ziprun.engine.utils.repository.SuggestionRepository;
import com.ziprun.engine.exception.ZycusErrorCode;
import com.ziprun.engine.interfaces.suggestioncontroller.service.SuggestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SuggestionServiceImpl implements SuggestionService {
    private final SuggestionRepository suggestionRepository;
    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;

    @Override
    public List<ReassignmentSuggestion> getPendingSuggestions() {
        return suggestionRepository.findByStatus(SuggestionStatus.PENDING);
    }

    @Override
    @Transactional
    public ReassignmentSuggestion updateSuggestionStatus(Long id, SuggestionStatus newStatus) {
        ReassignmentSuggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> ZycusErrorCode.SUGGESTION_NOT_FOUND.exception(String.valueOf(id)));

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw ZycusErrorCode.SUGGESTION_ALREADY_PROCESSED.exception(suggestion.getStatus().name());
        }

        suggestion.setStatus(newStatus);

        if (newStatus == SuggestionStatus.ACCEPTED) {
            Order order = suggestion.getOrder();
            Agent oldAgent = order.getAssignedAgent();
            Agent newAgent = suggestion.getRecommendedAgent();

            if (oldAgent != null) {
                oldAgent.setActiveOrderCount(Math.max(0, oldAgent.getActiveOrderCount() - 1));
                if (oldAgent.getActiveOrderCount() == 0 && oldAgent.getStatus() == AgentStatus.BUSY) {
                    oldAgent.setStatus(AgentStatus.AVAILABLE);
                }
                agentRepository.save(oldAgent);
            }

            newAgent.setActiveOrderCount(newAgent.getActiveOrderCount() + 1);
            if (newAgent.getStatus() == AgentStatus.AVAILABLE) {
                newAgent.setStatus(AgentStatus.BUSY);
            }
            agentRepository.save(newAgent);

            order.setAssignedAgent(newAgent);
            order.setStatus(OrderStatus.REASSIGNED);
            orderRepository.save(order);
        } else if (newStatus == SuggestionStatus.REJECTED) {
            Order order = suggestion.getOrder();
            order.setStatus(OrderStatus.ASSIGNED);
            orderRepository.save(order);
        }

        return suggestionRepository.save(suggestion);
    }
}

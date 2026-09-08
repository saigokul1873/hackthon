package com.ziprun.engine.utils.repository;

import com.ziprun.engine.utils.domain.ReassignmentSuggestion;
import com.ziprun.engine.utils.enums.SuggestionStatus;
import com.ziprun.engine.utils.enums.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuggestionRepository extends JpaRepository<ReassignmentSuggestion, Long> {
    boolean existsByOrderIdAndStatusAndTriggerReason(String orderId, SuggestionStatus status, TriggerReason triggerReason);
    List<ReassignmentSuggestion> findByStatus(SuggestionStatus status);
}

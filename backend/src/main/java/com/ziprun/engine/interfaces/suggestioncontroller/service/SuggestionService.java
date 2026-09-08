package com.ziprun.engine.interfaces.suggestioncontroller.service;

import com.ziprun.engine.utils.domain.ReassignmentSuggestion;
import com.ziprun.engine.utils.enums.SuggestionStatus;
import java.util.List;

public interface SuggestionService {
    List<ReassignmentSuggestion> getPendingSuggestions();
    ReassignmentSuggestion updateSuggestionStatus(Long id, SuggestionStatus newStatus);
}

package com.ziprun.engine.interfaces.suggestioncontroller;

import com.ziprun.engine.utils.domain.ReassignmentSuggestion;
import com.ziprun.engine.interfaces.suggestioncontroller.model.StatusUpdateRequest;
import com.ziprun.engine.interfaces.suggestioncontroller.service.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionService suggestionService;

    @GetMapping
    public List<ReassignmentSuggestion> getSuggestions() {
        return suggestionService.getPendingSuggestions();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReassignmentSuggestion> updateSuggestionStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        ReassignmentSuggestion updated = suggestionService.updateSuggestionStatus(id, request.status());
        return ResponseEntity.ok(updated);
    }
}

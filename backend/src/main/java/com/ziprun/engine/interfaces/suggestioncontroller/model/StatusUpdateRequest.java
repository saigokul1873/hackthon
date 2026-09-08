package com.ziprun.engine.interfaces.suggestioncontroller.model;

import com.ziprun.engine.utils.enums.SuggestionStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
    @NotNull(message = "Status cannot be null") 
    SuggestionStatus status
) {}

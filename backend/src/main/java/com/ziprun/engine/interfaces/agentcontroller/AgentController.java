package com.ziprun.engine.interfaces.agentcontroller;

import com.ziprun.engine.interfaces.agentcontroller.model.StatusUpdateRequest;
import com.ziprun.engine.utils.domain.Agent;
import com.ziprun.engine.interfaces.agentcontroller.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @GetMapping
    public List<Agent> getAllAgents() {
        return agentService.getAllAgents();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Agent> updateAgentStatus(@PathVariable String id, @Valid @RequestBody StatusUpdateRequest request) {
        Agent updated = agentService.updateStatus(id, request.status());
        return ResponseEntity.ok(updated);
    }
}

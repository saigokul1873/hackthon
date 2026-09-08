package com.ziprun.engine.utils.domain;

import com.ziprun.engine.utils.enums.SuggestionStatus;
import com.ziprun.engine.utils.enums.TriggerReason;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reassignment_suggestions")
public class ReassignmentSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recommended_agent_id", nullable = false)
    private Agent recommendedAgent;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(length = 2000)
    private String reasoning;

    @Enumerated(EnumType.STRING)
    private SuggestionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_reason")
    private TriggerReason triggerReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ReassignmentSuggestion() {
        this.createdAt = LocalDateTime.now();
    }

    public ReassignmentSuggestion(Order order, Agent recommendedAgent, Double confidenceScore, String reasoning, SuggestionStatus status, TriggerReason triggerReason) {
        this.order = order;
        this.recommendedAgent = recommendedAgent;
        this.confidenceScore = confidenceScore;
        this.reasoning = reasoning;
        this.status = status;
        this.triggerReason = triggerReason;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Agent getRecommendedAgent() {
        return recommendedAgent;
    }

    public void setRecommendedAgent(Agent recommendedAgent) {
        this.recommendedAgent = recommendedAgent;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public SuggestionStatus getStatus() {
        return status;
    }

    public void setStatus(SuggestionStatus status) {
        this.status = status;
    }

    public TriggerReason getTriggerReason() {
        return triggerReason;
    }

    public void setTriggerReason(TriggerReason triggerReason) {
        this.triggerReason = triggerReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

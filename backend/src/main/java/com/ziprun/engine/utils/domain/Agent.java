package com.ziprun.engine.utils.domain;

import com.ziprun.engine.utils.enums.AgentStatus;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "agents")
public class Agent {

    @Id
    private String id;

    private String name;

    @Column(name = "active_order_count")
    private int activeOrderCount;

    @Enumerated(EnumType.STRING)
    private AgentStatus status;

    // Extensibility for Sprint 2
    @Column(name = "zone_id", nullable = true)
    private String zoneId;

    @Column(name = "max_capacity", nullable = true)
    private Integer maxCapacity;

    public Agent() {
    }

    public Agent(String id, String name, int activeOrderCount, AgentStatus status) {
        this.id = id;
        this.name = name;
        this.activeOrderCount = activeOrderCount;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getActiveOrderCount() {
        return activeOrderCount;
    }

    public void setActiveOrderCount(int activeOrderCount) {
        this.activeOrderCount = activeOrderCount;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Agent agent = (Agent) o;
        return Objects.equals(id, agent.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

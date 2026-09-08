package com.ziprun.engine.utils.repository;

import com.ziprun.engine.utils.domain.Order;
import com.ziprun.engine.utils.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByAssignedAgentId(String agentId);
}

package com.ziprun.engine.interfaces.ordercontroller;

import com.ziprun.engine.interfaces.ordercontroller.model.CreateOrderRequest;
import com.ziprun.engine.utils.domain.Order;
import com.ziprun.engine.utils.domain.ReassignmentSuggestion;
import com.ziprun.engine.utils.enums.OrderStatus;
import com.ziprun.engine.interfaces.ordercontroller.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public List<Order> getOrders(@RequestParam(required = false) OrderStatus status) {
        return orderService.getOrders(status);
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order createdOrder = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @PostMapping("/{id}/suggest")
    public ResponseEntity<ReassignmentSuggestion> suggestReassignment(@PathVariable String id) {
        return ResponseEntity.ok(orderService.suggestForOrder(id));
    }

    @PostMapping("/{id}/suggest/stream")
    public SseEmitter streamSuggestion(@PathVariable String id) {
        return orderService.streamSuggestion(id);
    }
}

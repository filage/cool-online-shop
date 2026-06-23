package com.coolonlineshop.order.controller;

import com.coolonlineshop.order.dto.OrderResponse;
import com.coolonlineshop.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@RequestHeader("X-User-Id") Long userId) {
        return orderService.checkout(userId);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id
    ) {
        return orderService.getOrderById(id, userId);
    }

    @GetMapping
    public List<OrderResponse> getOrdersByUserId(@RequestHeader("X-User-Id") Long userId) {
        return orderService.getOrdersByUserId(userId);
    }
}

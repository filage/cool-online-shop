package com.coolonlineshop.order.controller;

import com.coolonlineshop.order.dto.OrderItemResponse;
import com.coolonlineshop.order.dto.OrderResponse;
import com.coolonlineshop.order.entity.OrderStatus;
import com.coolonlineshop.order.exception.CartServiceUnavailableException;
import com.coolonlineshop.order.exception.EmptyCartException;
import com.coolonlineshop.order.exception.GlobalExceptionHandler;
import com.coolonlineshop.order.exception.OrderNotFoundException;
import com.coolonlineshop.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void checkoutReturnsCreatedOrder() throws Exception {
        OrderResponse response = createResponse(1L, 1L);
        when(orderService.checkout(1L)).thenReturn(response);

        mockMvc.perform(post("/orders/checkout")
                        .header("X-User-Id", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(59.98))
                .andExpect(jsonPath("$.items[0].productId").value(10))
                .andExpect(jsonPath("$.items[0].productName").value("Wireless Mouse"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void checkoutReturnsBadRequestWhenUserHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/orders/checkout"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Missing request header"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Required request header is missing: X-User-Id"));
    }

    @Test
    void checkoutReturnsConflictWhenCartIsEmpty() throws Exception {
        when(orderService.checkout(1L)).thenThrow(new EmptyCartException());

        mockMvc.perform(post("/orders/checkout")
                        .header("X-User-Id", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cart is empty"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Cart is empty"));
    }

    @Test
    void checkoutReturnsServiceUnavailableWhenCartServiceIsUnavailable() throws Exception {
        when(orderService.checkout(1L)).thenThrow(new CartServiceUnavailableException());

        mockMvc.perform(post("/orders/checkout")
                        .header("X-User-Id", "1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Cart service unavailable"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value("Cart service is unavailable"));
    }

    @Test
    void getOrderByIdReturnsOrder() throws Exception {
        OrderResponse response = createResponse(1L, 1L);
        when(orderService.getOrderById(1L, 1L)).thenReturn(response);

        mockMvc.perform(get("/orders/1")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(10));
    }

    @Test
    void getOrderByIdReturnsNotFoundWhenOrderDoesNotExist() throws Exception {
        when(orderService.getOrderById(999L, 1L)).thenThrow(new OrderNotFoundException(999L));

        mockMvc.perform(get("/orders/999")
                        .header("X-User-Id", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Order not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Order with id 999 not found"));
    }

    @Test
    void getOrdersByUserIdReturnsOrders() throws Exception {
        when(orderService.getOrdersByUserId(1L)).thenReturn(List.of(
                createResponse(2L, 1L),
                createResponse(1L, 1L)
        ));

        mockMvc.perform(get("/orders")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[1].id").value(1))
                .andExpect(jsonPath("$[1].userId").value(1));
    }

    private OrderResponse createResponse(Long id, Long userId) {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 10, 0);

        return new OrderResponse(
                id,
                userId,
                OrderStatus.CREATED,
                new BigDecimal("59.98"),
                now,
                now,
                List.of(new OrderItemResponse(
                        1L,
                        10L,
                        "Wireless Mouse",
                        new BigDecimal("29.99"),
                        2
                ))
        );
    }
}

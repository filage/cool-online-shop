package com.coolonlineshop.order.service;

import com.coolonlineshop.order.client.CartClient;
import com.coolonlineshop.order.client.CartItemResponse;
import com.coolonlineshop.order.client.CartResponse;
import com.coolonlineshop.order.client.CatalogClient;
import com.coolonlineshop.order.client.CatalogProductResponse;
import com.coolonlineshop.order.dto.OrderResponse;
import com.coolonlineshop.order.entity.Order;
import com.coolonlineshop.order.entity.OrderItem;
import com.coolonlineshop.order.entity.OrderStatus;
import com.coolonlineshop.order.exception.EmptyCartException;
import com.coolonlineshop.order.exception.OrderNotFoundException;
import com.coolonlineshop.order.exception.ProductQuantityNotAvailableException;
import com.coolonlineshop.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartClient cartClient;

    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void checkoutCreatesOrderFromCartAndCatalogThenClearsCart() {
        when(cartClient.getCart(1L)).thenReturn(new CartResponse(
                1L,
                List.of(
                        new CartItemResponse(1L, 10L, 2),
                        new CartItemResponse(1L, 25L, 1)
                )
        ));
        when(catalogClient.getProduct(10L)).thenReturn(new CatalogProductResponse(
                10L,
                "Wireless Mouse",
                new BigDecimal("29.99"),
                10
        ));
        when(catalogClient.getProduct(25L)).thenReturn(new CatalogProductResponse(
                25L,
                "Spring Boot Guide",
                new BigDecimal("39.99"),
                5
        ));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 1L);
            ReflectionTestUtils.setField(order.getItems().get(0), "id", 1L);
            ReflectionTestUtils.setField(order.getItems().get(1), "id", 2L);
            return order;
        });

        OrderResponse response = orderService.checkout(1L);

        InOrder order = inOrder(cartClient, orderRepository);
        order.verify(cartClient).getCart(1L);
        order.verify(orderRepository).save(any(Order.class));
        order.verify(cartClient).clearCart(1L);
        assertEquals(1L, response.id());
        assertEquals(1L, response.userId());
        assertEquals(OrderStatus.CREATED, response.status());
        assertEquals(0, new BigDecimal("99.97").compareTo(response.totalAmount()));
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
        assertEquals(2, response.items().size());
        assertEquals(10L, response.items().getFirst().productId());
        assertEquals("Wireless Mouse", response.items().getFirst().productName());
        assertEquals(0, new BigDecimal("29.99").compareTo(response.items().getFirst().productPrice()));
        assertEquals(2, response.items().getFirst().quantity());
        assertEquals(25L, response.items().get(1).productId());
        assertEquals("Spring Boot Guide", response.items().get(1).productName());
        assertEquals(1, response.items().get(1).quantity());
    }

    @Test
    void checkoutThrowsExceptionWhenCartIsEmpty() {
        when(cartClient.getCart(1L)).thenReturn(new CartResponse(1L, List.of()));

        assertThrows(EmptyCartException.class, () -> orderService.checkout(1L));

        verify(orderRepository, never()).save(any(Order.class));
        verify(cartClient, never()).clearCart(1L);
    }

    @Test
    void checkoutThrowsExceptionWhenProductQuantityIsNotAvailable() {
        when(cartClient.getCart(1L)).thenReturn(new CartResponse(
                1L,
                List.of(new CartItemResponse(1L, 10L, 3))
        ));
        when(catalogClient.getProduct(10L)).thenReturn(new CatalogProductResponse(
                10L,
                "Wireless Mouse",
                new BigDecimal("29.99"),
                2
        ));

        ProductQuantityNotAvailableException exception = assertThrows(
                ProductQuantityNotAvailableException.class,
                () -> orderService.checkout(1L)
        );

        assertEquals("Product with id 10 has only 2 items available, requested 3", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartClient, never()).clearCart(1L);
    }

    @Test
    void getOrderByIdReturnsOrderWhenOrderExists() {
        Order order = createOrder(1L, 1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(1L, 1L);

        verify(orderRepository).findById(1L);
        assertEquals(1L, response.id());
        assertEquals(1L, response.userId());
        assertEquals(OrderStatus.CREATED, response.status());
        assertEquals(1, response.items().size());
    }

    @Test
    void getOrderByIdThrowsExceptionWhenOrderDoesNotExist() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(999L, 1L)
        );

        verify(orderRepository).findById(999L);
        assertEquals("Order with id 999 not found", exception.getMessage());
    }

    @Test
    void getOrderByIdThrowsExceptionWhenOrderBelongsToAnotherUser() {
        Order order = createOrder(1L, 2L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(1L, 1L)
        );

        verify(orderRepository).findById(1L);
        assertEquals("Order with id 1 not found", exception.getMessage());
    }

    @Test
    void getOrdersByUserIdReturnsOrdersNewestFirst() {
        Order firstOrder = createOrder(1L, 1L);
        Order secondOrder = createOrder(2L, 1L);
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(secondOrder, firstOrder));

        List<OrderResponse> responses = orderService.getOrdersByUserId(1L);

        verify(orderRepository).findByUserIdOrderByCreatedAtDesc(1L);
        assertEquals(2, responses.size());
        assertEquals(2L, responses.getFirst().id());
        assertEquals(1L, responses.get(1).id());
    }

    private Order createOrder(Long id, Long userId) {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 10, 0);
        Order order = new Order(
                userId,
                OrderStatus.CREATED,
                new BigDecimal("29.99"),
                now,
                now
        );
        OrderItem item = new OrderItem(
                10L,
                "Wireless Mouse",
                new BigDecimal("29.99"),
                1
        );
        order.addItem(item);
        ReflectionTestUtils.setField(order, "id", id);
        ReflectionTestUtils.setField(item, "id", id);
        return order;
    }
}

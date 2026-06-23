package com.coolonlineshop.order.service;

import com.coolonlineshop.order.client.CartClient;
import com.coolonlineshop.order.client.CartItemResponse;
import com.coolonlineshop.order.client.CartResponse;
import com.coolonlineshop.order.client.CatalogClient;
import com.coolonlineshop.order.client.CatalogProductResponse;
import com.coolonlineshop.order.dto.OrderItemResponse;
import com.coolonlineshop.order.dto.OrderResponse;
import com.coolonlineshop.order.entity.Order;
import com.coolonlineshop.order.entity.OrderItem;
import com.coolonlineshop.order.entity.OrderStatus;
import com.coolonlineshop.order.exception.EmptyCartException;
import com.coolonlineshop.order.exception.OrderNotFoundException;
import com.coolonlineshop.order.exception.ProductQuantityNotAvailableException;
import com.coolonlineshop.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartClient cartClient;
    private final CatalogClient catalogClient;

    public OrderService(
            OrderRepository orderRepository,
            CartClient cartClient,
            CatalogClient catalogClient
    ) {
        this.orderRepository = orderRepository;
        this.cartClient = cartClient;
        this.catalogClient = catalogClient;
    }

    public OrderResponse checkout(Long userId) {
        CartResponse cart = cartClient.getCart(userId);
        List<CartItemResponse> cartItems = cart.items();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new EmptyCartException();
        }

        List<CheckoutItem> checkoutItems = cartItems.stream()
                .map(this::toCheckoutItem)
                .toList();

        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalAmount = calculateTotalAmount(checkoutItems);

        Order order = new Order(
                userId,
                OrderStatus.CREATED,
                totalAmount,
                now,
                now
        );

        checkoutItems
                .stream()
                .map(this::toOrderItem)
                .forEach(order::addItem);

        Order savedOrder = orderRepository.save(order);
        cartClient.clearCart(userId);

        return toResponse(savedOrder);
    }

    public OrderResponse getOrderById(Long id, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (!order.getUserId().equals(userId)) {
            throw new OrderNotFoundException(id);
        }

        return toResponse(order);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CheckoutItem toCheckoutItem(CartItemResponse cartItem) {
        CatalogProductResponse product = catalogClient.getProduct(cartItem.productId());
        if (cartItem.quantity() > product.availableQuantity()) {
            throw new ProductQuantityNotAvailableException(
                    cartItem.productId(),
                    cartItem.quantity(),
                    product.availableQuantity()
            );
        }

        return new CheckoutItem(
                product.id(),
                product.name(),
                product.price(),
                cartItem.quantity()
        );
    }

    private BigDecimal calculateTotalAmount(List<CheckoutItem> items) {
        return items.stream()
                .map(item -> item.productPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderItem toOrderItem(CheckoutItem request) {
        return new OrderItem(
                request.productId(),
                request.productName(),
                request.productPrice(),
                request.quantity()
        );
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems()
                        .stream()
                        .map(this::toItemResponse)
                        .toList()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getProductPrice(),
                item.getQuantity()
        );
    }

    private record CheckoutItem(
            Long productId,
            String productName,
            BigDecimal productPrice,
            Integer quantity
    ) {
    }
}

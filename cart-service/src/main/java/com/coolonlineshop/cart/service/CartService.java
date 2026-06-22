package com.coolonlineshop.cart.service;

import com.coolonlineshop.cart.client.CatalogClient;
import com.coolonlineshop.cart.dto.AddCartItemRequest;
import com.coolonlineshop.cart.dto.CartItemResponse;
import com.coolonlineshop.cart.dto.CartResponse;
import com.coolonlineshop.cart.dto.UpdateCartItemQuantityRequest;
import com.coolonlineshop.cart.exception.CartItemNotFoundException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.Map;

@Service
public class CartService {

    private static final Duration CART_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final CatalogClient catalogClient;

    public CartService(StringRedisTemplate redisTemplate, CatalogClient catalogClient) {
        this.redisTemplate = redisTemplate;
        this.catalogClient = catalogClient;
    }

    public CartItemResponse addItem(AddCartItemRequest request) {
        String key = cartKey(request.userId());
        String field = request.productId().toString();

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        Integer currentQuantity = getCurrentQuantity(hashOperations, key, field);
        Integer requestedQuantity = currentQuantity + request.quantity();
        catalogClient.validateProductAvailable(request.productId(), requestedQuantity);

        Long updatedQuantity = hashOperations.increment(key, field, request.quantity());

        if (updatedQuantity == null) {
            throw new IllegalStateException("Redis did not return updated cart item quantity");
        }

        refreshCartTtl(key);

        return new CartItemResponse(
                request.userId(),
                request.productId(),
                updatedQuantity.intValue()
        );
    }

    public CartResponse getCart(Long userId) {
        String key = cartKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        return new CartResponse(
                userId,
                entries.entrySet()
                        .stream()
                        .map(entry -> new CartItemResponse(
                                userId,
                                Long.valueOf(entry.getKey().toString()),
                                Integer.valueOf(entry.getValue().toString())
                        ))
                        .sorted(Comparator.comparing(CartItemResponse::productId))
                        .toList()
        );
    }

    public CartItemResponse updateItemQuantity(
            Long userId,
            Long productId,
            UpdateCartItemQuantityRequest request
    ) {
        String key = cartKey(userId);
        String field = productId.toString();
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        if (!Boolean.TRUE.equals(hashOperations.hasKey(key, field))) {
            throw new CartItemNotFoundException(userId, productId);
        }

        catalogClient.validateProductAvailable(productId, request.quantity());
        hashOperations.put(key, field, request.quantity().toString());
        refreshCartTtl(key);

        return new CartItemResponse(
                userId,
                productId,
                request.quantity()
        );
    }

    public void deleteItem(Long userId, Long productId) {
        String key = cartKey(userId);
        String field = productId.toString();
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        if (!Boolean.TRUE.equals(hashOperations.hasKey(key, field))) {
            throw new CartItemNotFoundException(userId, productId);
        }

        hashOperations.delete(key, field);
    }

    public void clearCart(Long userId) {
        redisTemplate.delete(cartKey(userId));
    }

    private void refreshCartTtl(String key) {
        redisTemplate.expire(key, CART_TTL);
    }

    private Integer getCurrentQuantity(
            HashOperations<String, Object, Object> hashOperations,
            String key,
            String field
    ) {
        Object value = hashOperations.get(key, field);
        if (value == null) {
            return 0;
        }

        return Integer.valueOf(value.toString());
    }

    private String cartKey(Long userId) {
        return "cart:" + userId;
    }
}

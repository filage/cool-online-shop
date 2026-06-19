package com.coolonlineshop.cart.service;

import com.coolonlineshop.cart.dto.AddCartItemRequest;
import com.coolonlineshop.cart.dto.CartItemResponse;
import com.coolonlineshop.cart.dto.CartResponse;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;

@Service
public class CartService {

    private final StringRedisTemplate redisTemplate;

    public CartService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public CartItemResponse addItem(AddCartItemRequest request) {
        String key = cartKey(request.userId());
        String field = request.productId().toString();

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        Long updatedQuantity = hashOperations.increment(key, field, request.quantity());

        if (updatedQuantity == null) {
            throw new IllegalStateException("Redis did not return updated cart item quantity");
        }

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

    private String cartKey(Long userId) {
        return "cart:" + userId;
    }
}

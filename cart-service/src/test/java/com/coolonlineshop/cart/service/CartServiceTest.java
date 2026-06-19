package com.coolonlineshop.cart.service;

import com.coolonlineshop.cart.dto.AddCartItemRequest;
import com.coolonlineshop.cart.dto.CartItemResponse;
import com.coolonlineshop.cart.dto.CartResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private CartService cartService;

    @Test
    void addItemStoresQuantityInRedisHashAndReturnsResponse() {
        AddCartItemRequest request = new AddCartItemRequest(1L, 10L, 2);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.increment("cart:1", "10", 2)).thenReturn(2L);

        CartItemResponse response = cartService.addItem(request);

        verify(hashOperations).increment("cart:1", "10", 2);
        assertEquals(1L, response.userId());
        assertEquals(10L, response.productId());
        assertEquals(2, response.quantity());
    }

    @Test
    void addItemIncreasesQuantityWhenSameProductAlreadyExists() {
        AddCartItemRequest request = new AddCartItemRequest(1L, 10L, 3);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.increment("cart:1", "10", 3)).thenReturn(5L);

        CartItemResponse response = cartService.addItem(request);

        verify(hashOperations).increment("cart:1", "10", 3);
        assertEquals(5, response.quantity());
    }

    @Test
    void getCartReturnsItemsFromRedisHash() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("cart:1")).thenReturn(Map.<Object, Object>of(
                "25", "1",
                "10", "2"
        ));

        CartResponse response = cartService.getCart(1L);

        verify(hashOperations).entries("cart:1");
        assertEquals(1L, response.userId());
        assertEquals(2, response.items().size());
        assertEquals(10L, response.items().get(0).productId());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(25L, response.items().get(1).productId());
        assertEquals(1, response.items().get(1).quantity());
    }

    @Test
    void getCartReturnsEmptyItemsWhenCartDoesNotExist() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("cart:999")).thenReturn(Map.<Object, Object>of());

        CartResponse response = cartService.getCart(999L);

        verify(hashOperations).entries("cart:999");
        assertEquals(999L, response.userId());
        assertEquals(0, response.items().size());
    }
}

package com.coolonlineshop.cart.service;

import com.coolonlineshop.cart.client.CatalogClient;
import com.coolonlineshop.cart.dto.AddCartItemRequest;
import com.coolonlineshop.cart.dto.CartItemResponse;
import com.coolonlineshop.cart.dto.CartResponse;
import com.coolonlineshop.cart.dto.UpdateCartItemQuantityRequest;
import com.coolonlineshop.cart.exception.CartItemNotFoundException;
import com.coolonlineshop.cart.exception.ProductNotFoundException;
import com.coolonlineshop.cart.exception.ProductQuantityNotAvailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private CartService cartService;

    @Test
    void addItemStoresQuantityInRedisHashAndReturnsResponse() {
        AddCartItemRequest request = new AddCartItemRequest(10L, 2);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.increment("cart:1", "10", 2)).thenReturn(2L);

        CartItemResponse response = cartService.addItem(1L, request);

        verify(hashOperations).get("cart:1", "10");
        verify(catalogClient).validateProductAvailable(10L, 2);
        verify(hashOperations).increment("cart:1", "10", 2);
        verify(redisTemplate).expire("cart:1", Duration.ofDays(7));
        assertEquals(1L, response.userId());
        assertEquals(10L, response.productId());
        assertEquals(2, response.quantity());
    }

    @Test
    void addItemIncreasesQuantityWhenSameProductAlreadyExists() {
        AddCartItemRequest request = new AddCartItemRequest(10L, 3);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("cart:1", "10")).thenReturn("2");
        when(hashOperations.increment("cart:1", "10", 3)).thenReturn(5L);

        CartItemResponse response = cartService.addItem(1L, request);

        verify(hashOperations).get("cart:1", "10");
        verify(catalogClient).validateProductAvailable(10L, 5);
        verify(hashOperations).increment("cart:1", "10", 3);
        verify(redisTemplate).expire("cart:1", Duration.ofDays(7));
        assertEquals(5, response.quantity());
    }

    @Test
    void addItemThrowsExceptionWhenProductDoesNotExist() {
        AddCartItemRequest request = new AddCartItemRequest(999L, 2);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        doThrow(new ProductNotFoundException(999L))
                .when(catalogClient)
                .validateProductAvailable(999L, 2);

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> cartService.addItem(1L, request)
        );

        verify(hashOperations).get("cart:1", "999");
        verify(catalogClient).validateProductAvailable(999L, 2);
        assertEquals("Product with id 999 not found", exception.getMessage());
    }

    @Test
    void addItemThrowsExceptionWhenRequestedQuantityIsGreaterThanAvailableQuantity() {
        AddCartItemRequest request = new AddCartItemRequest(10L, 3);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("cart:1", "10")).thenReturn("4");
        doThrow(new ProductQuantityNotAvailableException(10L, 7, 5))
                .when(catalogClient)
                .validateProductAvailable(10L, 7);

        ProductQuantityNotAvailableException exception = assertThrows(
                ProductQuantityNotAvailableException.class,
                () -> cartService.addItem(1L, request)
        );

        verify(hashOperations).get("cart:1", "10");
        verify(catalogClient).validateProductAvailable(10L, 7);
        assertEquals("Product with id 10 has only 5 available items, requested 7", exception.getMessage());
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

    @Test
    void updateItemQuantityUpdatesExistingCartItemAndReturnsResponse() {
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(5);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.hasKey("cart:1", "10")).thenReturn(true);

        CartItemResponse response = cartService.updateItemQuantity(1L, 10L, request);

        verify(hashOperations).hasKey("cart:1", "10");
        verify(catalogClient).validateProductAvailable(10L, 5);
        verify(hashOperations).put("cart:1", "10", "5");
        verify(redisTemplate).expire("cart:1", Duration.ofDays(7));
        assertEquals(1L, response.userId());
        assertEquals(10L, response.productId());
        assertEquals(5, response.quantity());
    }

    @Test
    void updateItemQuantityThrowsExceptionWhenCartItemDoesNotExist() {
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(5);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.hasKey("cart:1", "999")).thenReturn(false);

        CartItemNotFoundException exception = assertThrows(
                CartItemNotFoundException.class,
                () -> cartService.updateItemQuantity(1L, 999L, request)
        );

        verify(hashOperations).hasKey("cart:1", "999");
        assertEquals("Cart item with product id 999 for user id 1 not found", exception.getMessage());
    }

    @Test
    void updateItemQuantityThrowsExceptionWhenRequestedQuantityIsGreaterThanAvailableQuantity() {
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(7);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.hasKey("cart:1", "10")).thenReturn(true);
        doThrow(new ProductQuantityNotAvailableException(10L, 7, 5))
                .when(catalogClient)
                .validateProductAvailable(10L, 7);

        ProductQuantityNotAvailableException exception = assertThrows(
                ProductQuantityNotAvailableException.class,
                () -> cartService.updateItemQuantity(1L, 10L, request)
        );

        verify(hashOperations).hasKey("cart:1", "10");
        verify(catalogClient).validateProductAvailable(10L, 7);
        assertEquals("Product with id 10 has only 5 available items, requested 7", exception.getMessage());
    }

    @Test
    void deleteItemDeletesExistingCartItem() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.hasKey("cart:1", "10")).thenReturn(true);

        cartService.deleteItem(1L, 10L);

        verify(hashOperations).hasKey("cart:1", "10");
        verify(hashOperations).delete("cart:1", "10");
        verify(redisTemplate).expire("cart:1", Duration.ofDays(7));
    }

    @Test
    void deleteItemThrowsExceptionWhenCartItemDoesNotExist() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.hasKey("cart:1", "999")).thenReturn(false);

        CartItemNotFoundException exception = assertThrows(
                CartItemNotFoundException.class,
                () -> cartService.deleteItem(1L, 999L)
        );

        verify(hashOperations).hasKey("cart:1", "999");
        assertEquals("Cart item with product id 999 for user id 1 not found", exception.getMessage());
    }

    @Test
    void clearCartDeletesCartKey() {
        cartService.clearCart(1L);

        verify(redisTemplate).delete("cart:1");
    }
}

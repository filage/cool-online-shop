package com.coolonlineshop.cart.controller;

import com.coolonlineshop.cart.dto.AddCartItemRequest;
import com.coolonlineshop.cart.dto.CartItemResponse;
import com.coolonlineshop.cart.dto.CartResponse;
import com.coolonlineshop.cart.dto.UpdateCartItemQuantityRequest;
import com.coolonlineshop.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartItemResponse addItem(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return cartService.addItem(userId, request);
    }

    @GetMapping
    public CartResponse getCart(@RequestHeader("X-User-Id") Long userId) {
        return cartService.getCart(userId);
    }

    @PutMapping("/items/{productId}")
    public CartItemResponse updateItemQuantity(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request
    ) {
        return cartService.updateItemQuantity(userId, productId, request);
    }

    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long productId
    ) {
        cartService.deleteItem(userId, productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@RequestHeader("X-User-Id") Long userId) {
        cartService.clearCart(userId);
    }
}

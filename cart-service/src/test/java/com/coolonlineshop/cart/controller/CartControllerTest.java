package com.coolonlineshop.cart.controller;

import com.coolonlineshop.cart.dto.AddCartItemRequest;
import com.coolonlineshop.cart.dto.CartItemResponse;
import com.coolonlineshop.cart.dto.CartResponse;
import com.coolonlineshop.cart.dto.UpdateCartItemQuantityRequest;
import com.coolonlineshop.cart.exception.CartItemNotFoundException;
import com.coolonlineshop.cart.exception.GlobalExceptionHandler;
import com.coolonlineshop.cart.exception.ProductNotFoundException;
import com.coolonlineshop.cart.exception.ProductQuantityNotAvailableException;
import com.coolonlineshop.cart.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import(GlobalExceptionHandler.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Test
    void addItemReturnsCreatedCartItem() throws Exception {
        CartItemResponse response = new CartItemResponse(
                1L,
                10L,
                2
        );
        when(cartService.addItem(any(AddCartItemRequest.class))).thenReturn(response);

        mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "productId": 10,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void addItemReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 0,
                                  "productId": -1,
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.userId").value("must be greater than 0"))
                .andExpect(jsonPath("$.errors.productId").value("must be greater than 0"))
                .andExpect(jsonPath("$.errors.quantity").value("must be greater than 0"));
    }

    @Test
    void addItemReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        when(cartService.addItem(any(AddCartItemRequest.class)))
                .thenThrow(new ProductNotFoundException(999L));

        mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "productId": 999,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Product with id 999 not found"));
    }

    @Test
    void addItemReturnsConflictWhenRequestedQuantityIsGreaterThanAvailableQuantity() throws Exception {
        when(cartService.addItem(any(AddCartItemRequest.class)))
                .thenThrow(new ProductQuantityNotAvailableException(10L, 7, 5));

        mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "productId": 10,
                                  "quantity": 7
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient product quantity"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Product with id 10 has only 5 available items, requested 7"));
    }

    @Test
    void getCartReturnsCartItems() throws Exception {
        CartResponse response = new CartResponse(
                1L,
                List.of(
                        new CartItemResponse(1L, 10L, 2),
                        new CartItemResponse(1L, 25L, 1)
                )
        );
        when(cartService.getCart(1L)).thenReturn(response);

        mockMvc.perform(get("/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.items[0].userId").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(10))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[1].userId").value(1))
                .andExpect(jsonPath("$.items[1].productId").value(25))
                .andExpect(jsonPath("$.items[1].quantity").value(1));
    }

    @Test
    void updateItemQuantityReturnsUpdatedCartItem() throws Exception {
        CartItemResponse response = new CartItemResponse(1L, 10L, 5);
        when(cartService.updateItemQuantity(
                eq(1L),
                eq(10L),
                any(UpdateCartItemQuantityRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put("/cart/1/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void updateItemQuantityReturnsNotFoundWhenCartItemDoesNotExist() throws Exception {
        when(cartService.updateItemQuantity(
                eq(1L),
                eq(999L),
                any(UpdateCartItemQuantityRequest.class)
        )).thenThrow(new CartItemNotFoundException(1L, 999L));

        mockMvc.perform(put("/cart/1/items/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 5
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Cart item not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Cart item with product id 999 for user id 1 not found"));
    }

    @Test
    void updateItemQuantityReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(put("/cart/1/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.quantity").value("must be greater than 0"));
    }

    @Test
    void updateItemQuantityReturnsConflictWhenRequestedQuantityIsGreaterThanAvailableQuantity() throws Exception {
        when(cartService.updateItemQuantity(
                eq(1L),
                eq(10L),
                any(UpdateCartItemQuantityRequest.class)
        )).thenThrow(new ProductQuantityNotAvailableException(10L, 7, 5));

        mockMvc.perform(put("/cart/1/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 7
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient product quantity"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Product with id 10 has only 5 available items, requested 7"));
    }

    @Test
    void deleteItemReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/cart/1/items/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteItemReturnsNotFoundWhenCartItemDoesNotExist() throws Exception {
        doThrow(new CartItemNotFoundException(1L, 999L))
                .when(cartService)
                .deleteItem(1L, 999L);

        mockMvc.perform(delete("/cart/1/items/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Cart item not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Cart item with product id 999 for user id 1 not found"));
    }

    @Test
    void clearCartReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/cart/1"))
                .andExpect(status().isNoContent());
    }
}

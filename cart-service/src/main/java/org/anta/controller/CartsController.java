package org.anta.controller;


import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.anta.dto.request.CartItemsRequest;
import org.anta.dto.response.CartsResponse;
import org.anta.entity.Carts;
import org.anta.mapper.CartsMapper;
import org.anta.service.CartsService;

import java.util.Optional;

@Path("/api/cart")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CartsController {

    @Inject
    CartsService cartsService;

    @Inject
    CartsMapper cartsMapper;

    /**
     * [POST] Thêm sản phẩm vào giỏ hàng
     */
    @POST
    @Path("/add")
    public Response addItemToCart(CartItemsRequest request) {
        Carts cart = cartsService.AddItemsToCarts(request);
        return Response.ok(cartsMapper.toResponse(cart)).build();
    }

    /**
     * [GET] Lấy giỏ hàng hiện tại theo userId hoặc sessionId
     * Ví dụ: /api/cart/current?userId=1
     * Hoặc:  /api/cart/current?sessionId=abc123
     */
    @GET
    @Path("/current")
    public Response getCurrentCart(
            @QueryParam("userId") Long userId,
            @QueryParam("sessionId") String sessionId
    ) {
        Optional<Carts> optionalCart = cartsService.getCurrentCart(userId, sessionId);

        if (optionalCart.isEmpty()) {
            // 204 nếu chưa có giỏ hàng
            return Response.noContent().build();
        }

        // Convert sang response DTO
        CartsResponse response = cartsMapper.toResponse(optionalCart.get());
        return Response.ok(response).build();
    }

    /**
     * [DELETE] Xoá 1 sản phẩm khỏi giỏ hàng
     */
    @DELETE
    @Path("/item/{itemId}")
    public Response removeItemFromCart(@PathParam("itemId") Long itemId) {
        cartsService.DeleteItemsOutCart(itemId);
        return Response.noContent().build();
    }

    /**
     * [DELETE] Xoá toàn bộ sản phẩm khỏi giỏ hàng
     */
    @DELETE
    @Path("/{cartId}/clear")
    public Response clearCart(@PathParam("cartId") Long cartId) {
        cartsService.DeleteFullItemsOutCart(cartId);
        return Response.noContent().build();
    }

    /**
     * [PUT] Cập nhật số lượng sản phẩm trong giỏ hàng
     */
    @PUT
    @Path("/{cartId}/items/quantity")
    public Response updateItemQuantity(
            @PathParam("cartId") Long cartId,
            @QueryParam("productId") Long productId,
            @QueryParam("variantId") Long variantId,
            @QueryParam("newQuantity") Long newQuantity) {

        Carts updatedCart = cartsService.updateItemQuantity(cartId, productId, variantId, newQuantity);

        if (updatedCart != null) {
            return Response.ok(cartsMapper.toResponse(updatedCart)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Không tìm thấy sản phẩm trong giỏ hàng")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }

    /**
     * [POST] Merge giỏ hàng guest → user khi login
     */
    @POST
    @Path("/merge")
    public Response mergeCart(
            @QueryParam("sessionId") String sessionId,
            @QueryParam("userId") Long userId
    ) {
        Carts merged = cartsService.mergeCart(sessionId, userId);
        if (merged == null) {
            return Response.ok(msg("Không có giỏ để merge")).build();
        }
        return Response.ok(msg("Merge thành công", cartsMapper.toResponse(merged))).build();
    }

    // ============================================================
    // Helper chuẩn JSON response
    // ============================================================
    private Object msg(String message) {
        return new Object() {
            public final boolean success = true;
            public final String msg = message;
        };
    }

    private Object msg(String message, Object data) {
        return new Object() {
            public final boolean success = true;
            public final String msg = message;
            public final Object payload = data;
        };
    }
}
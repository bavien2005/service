package org.anta.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.anta.client.CloudClient;
import org.anta.client.FileMetadataDTO;
import org.anta.client.ProductClient;
import org.anta.client.ProductDTO;
import org.anta.dto.request.CartItemsRequest;
import org.anta.entity.CartItems;
import org.anta.entity.Carts;
import org.anta.enums.Status;
import org.anta.repository.CartItemsRepository;
import org.anta.repository.CartsRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class CartsService {

    @Inject
    CartsRepository cartsRepository;

    @Inject
    CartItemsRepository cartItemsRepository;

    @Inject
    ProductClient productClient;

    @Inject
    CloudClient cloudClient;

    private Carts createNewCart(CartItemsRequest req) {
        Carts newCart = new Carts();
        newCart.setUserId(req.getUserId());
        newCart.setSessionId(req.getSessionId());
        newCart.setStatus(Status.OPEN);
        LocalDateTime now = LocalDateTime.now();
        newCart.setCreatedAt(now);
        newCart.setUpdatedAt(now);
        return cartsRepository.save(newCart);
    }

    //them sp vao gio
    // thêm sp vào giỏ
    @Transactional
    public Carts AddItemsToCarts(CartItemsRequest req) {
        Optional<Carts> optionalCarts;
        if (req.getUserId() != null) {
            optionalCarts = cartsRepository.findByUserIdAndStatus(req.getUserId(), Status.OPEN);
        } else {
            optionalCarts = cartsRepository.findBySessionIdAndStatus(req.getSessionId(), Status.OPEN);
        }

        Carts cart = optionalCarts.orElseGet(() -> createNewCart(req));

        Optional<CartItems> cartItems = cartItemsRepository.findByCartIdAndProductIdAndVariantId(
                cart.getId(), req.getProductId(), req.getVariantId()
        );

        CartItems items;

        if (cartItems.isPresent()) {
            // Nếu sản phẩm đã tồn tại thì cộng thêm số lượng
            items = cartItems.get();
            items.setQuantity(
                    (items.getQuantity() == null ? 0 : items.getQuantity()) + req.getQuantity()
            );
            items.setUpdatedAt(LocalDateTime.now());
        } else {
            // Nếu chưa có thì tạo mới
            ProductDTO product = productClient.getProductById(req.getProductId());
            FileMetadataDTO file = null;
            try {
                file = cloudClient.getMainImage(req.getProductId());
            } catch (Exception e) {
                // cloudClient now already handles exceptions, but keep safe-catch
            }

            items = new CartItems();
            items.setCart(cart);
            items.setProductId(req.getProductId());
            items.setVariantId(req.getVariantId());
            items.setProductName(product != null ? product.getName() : null);
            items.setUnitPrice(product != null && product.getPrice() != null ? product.getPrice().doubleValue() : 0.0);
            // prefer cloud image, fallback to image in request (FE may provide)
            items.setImageUrl(file != null ? file.getUrl() : req.getImageUrl());
            // assign size/color/sku if provided from FE
            items.setSize(req.getSize());
            items.setColor(req.getColor());
            // optional: store sku if entity has it (if not, skip)
            // items.setSku(req.getSku()); // add field in entity if you want to store sku
            items.setQuantity(req.getQuantity());
            items.setCreatedAt(LocalDateTime.now());
            items.setUpdatedAt(LocalDateTime.now());
            items.setSku(req.getSku());
        }

        cartItemsRepository.save(items);

        // Cập nhật thời gian giỏ hàng
        cart.setUpdatedAt(LocalDateTime.now());
        cartsRepository.save(cart);

        return cartsRepository.findById(cart.getId()).orElse(cart);
    }

    //xoa 1
    public void DeleteItemsOutCart(Long idItems) {
        cartItemsRepository.deleteById(idItems);
    }

    //xoa full
    public void DeleteFullItemsOutCart(Long cartId) {
        cartItemsRepository.deleteByCartId(cartId);
    }

    /**
     * Xem giỏ hàng hiện tại (theo user hoặc session)
     */
    public Optional<Carts> getCurrentCart(Long userId, String sessionId) {
        if (userId != null) {
            Optional<Carts> userCart = cartsRepository.findByUserIdAndStatus(userId, Status.OPEN);
            if (userCart.isPresent()) {
                return userCart;
            }
        }

        if (sessionId != null) {
            return cartsRepository.findBySessionIdAndStatus(sessionId, Status.OPEN);
        }

        return Optional.empty();
    }

    public Carts updateItemQuantity(Long cartId, Long productId, Long variantId, Long newQuantity) {
        Optional<CartItems> cartItems = cartItemsRepository.findByCartIdAndProductIdAndVariantId(
                cartId, productId, variantId
        );

        if (cartItems.isPresent()) {
            CartItems item = cartItems.get();
            item.setQuantity(newQuantity);
            item.setUpdatedAt(LocalDateTime.now());
            cartItemsRepository.save(item);

            // Cập nhật thời gian sửa giỏ hàng
            Optional<Carts> cart = cartsRepository.findById(cartId);
            if (cart.isPresent()) {
                Carts existingCart = cart.get();
                existingCart.setUpdatedAt(LocalDateTime.now());
                return cartsRepository.save(existingCart);
            }
        }
        return null;
    }

    // ============================================================
    // MERGE GIỎ HÀNG (GUEST → USER) KHI LOGIN
    // ============================================================
    public Carts mergeCart(String sessionId, Long userId) {
        // Giỏ guest theo sessionId
        Optional<Carts> guestOpt = cartsRepository
                .findBySessionIdAndStatus(sessionId, Status.OPEN);

        // Giỏ user theo userId
        Optional<Carts> userOpt = cartsRepository
                .findByUserIdAndStatus(userId, Status.OPEN);

        // 1) Không có giỏ guest -> trả về giỏ user (nếu có), đảm bảo sessionId = null
        if (guestOpt.isEmpty()) {
            if (userOpt.isPresent()) {
                Carts userCart = userOpt.get();
                userCart.setSessionId(null);              // QUAN TRỌNG
                userCart.setUpdatedAt(LocalDateTime.now());
                return cartsRepository.save(userCart);
            }
            return null;
        }

        Carts guestCart = guestOpt.get();

        // 2) User chưa có giỏ -> dùng luôn giỏ guest, đổi sang userId + bỏ sessionId
        if (userOpt.isEmpty()) {
            guestCart.setUserId(userId);
            guestCart.setSessionId(null);                 // bỏ sessionId
            guestCart.setUpdatedAt(LocalDateTime.now());
            return cartsRepository.save(guestCart);
        }

        // 3) Cả guestCart & userCart đều có -> merge item, giữ lại giỏ user
        Carts userCart = userOpt.get();

        for (CartItems gItem : guestCart.getItems()) {
            Optional<CartItems> exist = cartItemsRepository
                    .findByCartIdAndProductIdAndVariantId(
                            userCart.getId(),
                            gItem.getProductId(),
                            gItem.getVariantId()
                    );

            if (exist.isPresent()) {
                // Cộng dồn quantity
                CartItems userItem = exist.get();
                userItem.setQuantity(
                        (userItem.getQuantity() == null ? 0L : userItem.getQuantity())
                                + (gItem.getQuantity() == null ? 0L : gItem.getQuantity())
                );
                userItem.setUpdatedAt(LocalDateTime.now());
                cartItemsRepository.save(userItem);
            } else {
                // Chuyển item guest sang giỏ user
                gItem.setCart(userCart);
                gItem.setUpdatedAt(LocalDateTime.now());
                cartItemsRepository.save(gItem);
            }
        }

        // Xóa giỏ guest
        cartsRepository.delete(guestCart);

        // Đảm bảo giỏ user không còn sessionId
        userCart.setSessionId(null);                      // QUAN TRỌNG
        userCart.setUpdatedAt(LocalDateTime.now());
        return cartsRepository.save(userCart);
    }
}
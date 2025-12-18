package com.scutshop.backend.service;

import com.scutshop.backend.mapper.CartMapper;
import com.scutshop.backend.model.Cart;
import com.scutshop.backend.model.CartItem;
import com.scutshop.backend.model.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {
    private final CartMapper cartMapper;
    private final ProductService productService;

    public CartService(CartMapper cartMapper, ProductService productService) {
        this.cartMapper = cartMapper;
        this.productService = productService;
    }

    @Transactional
    public Cart getOrCreateCartForUser(Long userId) {
        Cart c = null;
        if (userId != null)
            c = cartMapper.selectCartByUserId(userId);
        if (c == null) {
            Cart nc = new Cart();
            nc.setUserId(userId);
            cartMapper.insertCart(nc);
            nc.setItems(List.of());
            return nc;
        }
        c.setItems(cartMapper.selectItemsByCartId(c.getId()));
        return c;
    }

    @Transactional
    public Cart getCartById(Long cartId) {
        Cart c = cartMapper.selectCartById(cartId);
        if (c == null)
            return null;
        c.setItems(cartMapper.selectItemsByCartId(cartId));
        return c;
    }

    @Transactional
    public CartItem addItem(Long cartId, Long productId, int quantity) {
        Product p = productService.findById(productId);
        if (p == null)
            throw new IllegalArgumentException("product_not_found");
        if (quantity <= 0)
            throw new IllegalArgumentException("invalid_quantity");
        CartItem exist = null;
        List<CartItem> items = cartMapper.selectItemsByCartId(cartId);
        for (CartItem it : items)
            if (it.getProductId().equals(productId)) {
                exist = it;
                break;
            }
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + quantity);
            exist.setPrice(p.getPrice());
            cartMapper.updateItem(exist);
            return exist;
        }
        CartItem it = new CartItem();
        it.setCartId(cartId);
        it.setProductId(productId);
        it.setQuantity(quantity);
        it.setPrice(p.getPrice());
        cartMapper.insertItem(it);
        return it;
    }

    @Transactional
    public void updateItemQuantity(Long itemId, int quantity) {
        if (quantity <= 0)
            throw new IllegalArgumentException("invalid_quantity");
        CartItem it = cartMapper.selectItemById(itemId);
        if (it == null)
            throw new IllegalArgumentException("item_not_found");
        it.setQuantity(quantity);
        // refresh price from product
        Product p = productService.findById(it.getProductId());
        it.setPrice(p.getPrice());
        cartMapper.updateItem(it);
    }

    @Transactional
    public void removeItem(Long itemId) {
        cartMapper.deleteItem(itemId);
    }

    @Transactional
    public void mergeCart(Long fromCartId, Long toUserId) {
        if (fromCartId == null)
            return;
        Cart from = getCartById(fromCartId);
        if (from == null)
            return;
        Cart to = getOrCreateCartForUser(toUserId);
        for (CartItem it : from.getItems()) {
            // add to user's cart
            addItem(to.getId(), it.getProductId(), it.getQuantity());
        }
        // clear from cart
        cartMapper.deleteItemsByCartId(fromCartId);
        // optionally, assign from cart to user or leave empty; here we delete items
    }
}

package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.Business;
import cn.edu.tju.elm.model.Cart;
import cn.edu.tju.elm.model.Food;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.service.CartItemService;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.service.FoodService;
import cn.edu.tju.elm.utils.Utils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@Tag(name = "管理购物车", description = "对购物车内的商品增删改查")
public class CartController {

    @Autowired
    private UserService userService;

    @Autowired
    private CartItemService cartItemService;

    @Autowired
    private FoodService foodService;

    @PostMapping("/carts")
    public HttpResult<Cart> addCartItem(@RequestBody Cart cart) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (cart == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Cart CANT BE NULL");
        if (cart.getFood() == null || cart.getFood().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food.Id CANT BE NULL");
        if (cart.getQuantity() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Quantity CANT BE NULL");

        Food food = foodService.getFoodById(cart.getFood().getId());
        if (food == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");
        Business business = food.getBusiness();
        if (business == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");

        Utils.setNewEntity(cart, me);
        cart.setFood(food);
        cart.setBusiness(business);
        cart.setCustomer(me);
        cartItemService.addCart(cart);
        return HttpResult.success(cart);
    }

    @GetMapping("/carts")
    public HttpResult<List<Cart>> getCarts() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();
        return HttpResult.success(cartItemService.getUserCarts(me.getId()));
    }

    @PatchMapping("/carts/{id}")
    public HttpResult<Cart> updateCartItem(@PathVariable("id") Long id, @RequestBody Cart newCart) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (newCart == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Cart CANT BE NULL");

        if (newCart.getQuantity() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Cart.Quantity CANT BE NULL");

        Cart cart = cartItemService.getCartById(id);
        if (cart == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Cart NOT FOUND");
        User owner = cart.getCustomer();

        boolean isAdmin = Utils.hasAuthority(me, "ADMIN");
        if (isAdmin || me.equals(owner)) {
            cart.setQuantity(newCart.getQuantity());

            LocalDateTime now = LocalDateTime.now();
            cart.setUpdateTime(now);
            cart.setUpdater(me.getId());

            cartItemService.updateCart(cart);
            return HttpResult.success(cart);
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @DeleteMapping("/carts/{id}")
    public HttpResult<Cart> deleteCartItem(@PathVariable("id") Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Authority NOT FOUND");
        User me = meOptional.get();

        Cart cart = cartItemService.getCartById(id);
        if (cart == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Cart NOT FOUND");

        boolean isAdmin = Utils.hasAuthority(me, "ADMIN");
        if (isAdmin || me.equals(cart.getCustomer())) {
            cartItemService.deleteCart(cart);
            return HttpResult.success();
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
}

package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Cart;
import cn.edu.tju.elm.model.BO.Food;
import cn.edu.tju.elm.service.CartItemService;
import cn.edu.tju.elm.service.FoodService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.EntityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@Tag(name = "管理购物车", description = "对购物车内的商品增删改查")
public class CartController {
    private final UserService userService;
    private final CartItemService cartItemService;
    private final FoodService foodService;

    public CartController(UserService userService, CartItemService cartItemService, FoodService foodService) {
        this.userService = userService;
        this.cartItemService = cartItemService;
        this.foodService = foodService;
    }

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

        EntityUtils.setNewEntity(cart);
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

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        if (isAdmin || me.equals(owner)) {
            cart.setQuantity(newCart.getQuantity());
            EntityUtils.updateEntity(cart);
            cartItemService.updateCart(cart);
            return HttpResult.success(cart);
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @DeleteMapping("/carts/{id}")
    public HttpResult<String> deleteCartItem(@PathVariable("id") Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Authority NOT FOUND");
        User me = meOptional.get();

        Cart cart = cartItemService.getCartById(id);
        if (cart == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Cart NOT FOUND");

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        if (isAdmin || me.equals(cart.getCustomer())) {
            cartItemService.deleteCart(cart);
            return HttpResult.success("Delete cart successfully.");
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
}

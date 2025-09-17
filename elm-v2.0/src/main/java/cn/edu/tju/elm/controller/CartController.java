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
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
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

    @Autowired
    private BusinessService businessService;

    @PostMapping("/carts")
    public HttpResult<Cart> addCartItem(@RequestBody Cart cart) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (cart.getFood() == null || cart.getFood().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food.Id CANT BE NULL");
        if (cart.getBusiness() == null || cart.getBusiness().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business.Id CANT BE NULL");
        if (cart.getCustomer() == null || cart.getCustomer().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Customer.Id CANT BE NULL");
        if (cart.getQuantity() == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Quantity CANT BE NULL");

        Food food = foodService.getFoodById(cart.getFood().getId());
        if (food == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");
        Business business = businessService.getBusinessById(cart.getBusiness().getId());
        if (business == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
        User user = userService.getUserById(cart.getCustomer().getId());
        if (user == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");

        if (user.getUsername().equals(me.getUsername())) {
            cart.setFood(food);
            cart.setBusiness(business);
            cart.setCustomer(user);
            LocalDateTime now = LocalDateTime.now();
            cart.setCreateTime(now);
            cart.setUpdateTime(now);
            cart.setCreator(me.getId());
            cart.setUpdater(me.getId());
            cart.setDeleted(false);
            if (cart.equals(cartItemService.addCart(cart))) {
                return HttpResult.success(cart);
            }
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "UNKNOWN ERROR");
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
}

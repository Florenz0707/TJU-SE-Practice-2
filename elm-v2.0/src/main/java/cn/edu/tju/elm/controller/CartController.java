package cn.edu.tju.elm.controller;

import cn.edu.tju.elm.model.Cart;
//import cn.edu.tju.elb.service.CartItemService;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.security.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "管理购物车", description = "对购物车内的商品增删改查")
public class CartController {
    @Autowired
    private UserService userService;

//    @Autowired
//    private CartItemService cartItemService;

    @PostMapping("/carts")
    public HttpResult<Cart>  addCartItem(@RequestBody Cart cart){
        return null;
    }
}

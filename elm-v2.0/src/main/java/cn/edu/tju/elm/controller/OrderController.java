package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.elm.model.Order;
//import cn.edu.tju.elb.service.OrderService;
import cn.edu.tju.core.security.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "管理订单", description = "对订单进行增删改查")
public class OrderController {
    @Autowired
    private UserService userService;

//    @Autowired
//    private OrderService ordersService;

    @PostMapping(value = "")
    public HttpResult<Order> addOrders(@RequestBody Order order) throws Exception{
        return null;
    }

    @GetMapping("/{id}")
    public HttpResult getOrderById(@PathVariable Long id) throws Exception{
        return null;
    }

    @GetMapping("")
    public List<Order> listOrdersByUserId(@RequestParam Long userId) throws Exception{
        return null;
    }
}

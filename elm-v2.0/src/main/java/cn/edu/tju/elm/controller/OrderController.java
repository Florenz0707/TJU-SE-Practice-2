package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.repository.UserRepository;
import cn.edu.tju.elm.model.Business;
import cn.edu.tju.elm.model.DeliveryAddress;
import cn.edu.tju.elm.model.Order;
//import cn.edu.tju.elb.service.OrderService;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.repository.OrderRepository;
import cn.edu.tju.elm.service.AddressService;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "管理订单", description = "对订单进行增删改查")
public class OrderController {
    @Autowired
    private UserService userService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService ordersService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AddressService addressService;
    @Autowired
    private BusinessService businessService;

    @PostMapping(value = "")
    public HttpResult<Order> addOrders(@RequestBody Order order) throws Exception {

        //if() return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "CONSUMER NOT ACTIVE");
        if(order.getDeliveryAddress() == null)  return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "DELIVERY ADDRESS NOT FOUND");
        if(order.getDeliveryAddress().getId() == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "DELIVERY ADDRESS NOT FOUND");
        //if(addressService.getById(order.getDeliveryAddress().getId())) return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ADDRESS DELETED");
        if(order.getCustomer() == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "CUSTOMER NOT FOUND");
        if(order.getCustomer().getId() == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "CUSTOMER NOT FOUND");
        if(order.getBusiness() == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BUSINESS NOT FOUND");
        if(order.getBusiness().getId() == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BUSINESS NOT FOUND");
        if(order.getOrderTotal() == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "TOTAL NOT FOUND");

        // token来的
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if(meOptional.isEmpty())  return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        // 前端来的
        User user = userService.getById(order.getCustomer().getId());
        if(user == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "USER NOT FOUND");




        Business business = businessService.getById(order.getBusiness().getId());
        if(business == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BUSINESS NOT FOUND");
        DeliveryAddress address = addressService.getById(order.getDeliveryAddress().getId());
        if (address == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ADDRESS NOT FOUND");
        User customer = userService.getById(order.getCustomer().getId());
        if(customer == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "USER NOT FOUND");

        if(me.getUsername().equals(user.getUsername()) && me.getUsername().equals(address.getCustomer().getUsername())) {

            LocalDateTime now = LocalDateTime.now();
            order.setCreateTime(now);
            order.setUpdateTime(now);
            order.setCreator(user.getId());

            order.setUpdater(user.getId());
            order.setOrderDate(LocalDateTime.now());

            order.setDeleted(false);

            if(order.equals(ordersService.addOrder(order))) {
                return HttpResult.success(order);
            }
        }
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR);
    }

    @GetMapping("/{id}")
    public HttpResult getOrderById(@PathVariable Long id) throws Exception {
        return null;
    }

    @GetMapping("")
    public List<Order> listOrdersByUserId(@RequestParam Long userId) throws Exception {
        return null;
    }
}

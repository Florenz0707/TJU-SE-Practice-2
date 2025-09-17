package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.Business;
import cn.edu.tju.elm.model.DeliveryAddress;
import cn.edu.tju.elm.model.Order;
import cn.edu.tju.elm.service.AddressService;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.service.OrderService;
import cn.edu.tju.core.security.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "管理订单", description = "对订单进行增删改查")
public class OrderController {
    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;
    @Autowired
    private BusinessService businessService;
    @Autowired
    private AddressService addressService;

    @PostMapping(value = "")
    public HttpResult<Order> addOrders(@RequestBody Order order) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (order.getBusiness() == null || order.getBusiness().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business.Id CANT BE NULL");
        if (order.getCustomer() == null || order.getCustomer().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Customer.Id CANT BE NULL");
        if (order.getDeliveryAddress() == null || order.getDeliveryAddress().getId() == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "DeliveryAddress.Id CANT BE NULL");
        }

        Business business = businessService.getBusinessById(order.getBusiness().getId());
        if (business == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
        User customer = userService.getUserById(order.getCustomer().getId());
        if (customer == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Customer NOT FOUND");
        DeliveryAddress address = addressService.getAddressById(order.getDeliveryAddress().getId());
        if (address == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "DeliveryAddress NOT FOUND");

        if (me.equals(customer) && me.equals(address.getCustomer())) {
            // TODO: Complete Order info and OrderDetailet with cart(customer_id, business_id)
            order.setOrderTotal(BigDecimal.valueOf(0));
            order.setOrderState(0);

            order.setBusiness(business);
            order.setCustomer(customer);
            order.setDeliveryAddress(address);

            LocalDateTime now = LocalDateTime.now();
            order.setCreateTime(now);
            order.setUpdateTime(now);
            order.setOrderDate(now);
            order.setCreator(me.getId());
            order.setUpdater(me.getId());
            order.setDeleted(false);
            if (order.equals(orderService.addOrder(order))) {
                return HttpResult.success(order);
            }
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @GetMapping("/{id}")
    public HttpResult getOrderById(@PathVariable Long id) {
        return null;
    }

    @GetMapping("")
    public List<Order> listOrdersByUserId(@RequestParam Long userId) {
        return null;
    }
}

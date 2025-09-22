package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.Authority;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.*;
import cn.edu.tju.elm.service.*;
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

    @Autowired
    private CartItemService cartItemService;

    @Autowired
    private OrderDetailetService orderDetailetService;

    @PostMapping(value = "")
    public HttpResult<Order> addOrders(@RequestBody Order order) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (order == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order CANT BE NULL");

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

        List<Cart> cartList = cartItemService.getCart(business.getId(), customer.getId());
        if (cartList.isEmpty())
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Customer's Cart IS EMPTY");

        if (me.equals(customer) && me.equals(address.getCustomer())) {
            BigDecimal totalPrice = new BigDecimal(0);
            for (Cart cart : cartList) {
                BigDecimal quantity = new BigDecimal(cart.getQuantity());
                totalPrice = totalPrice.add(cart.getFood().getFoodPrice().multiply(quantity));
            }
            order.setOrderTotal(totalPrice);
            order.setOrderState(OrderState.UNPAID);

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

            orderService.addOrder(order);
            for (Cart cart : cartList) {
                OrderDetailet orderDetailet = new OrderDetailet();
                orderDetailet.setCreateTime(now);
                orderDetailet.setUpdateTime(now);
                orderDetailet.setCreator(me.getId());
                orderDetailet.setUpdater(me.getId());
                orderDetailet.setDeleted(false);
                orderDetailet.setOrder(order);
                orderDetailet.setFood(cart.getFood());
                orderDetailet.setQuantity(cart.getQuantity());
                orderDetailetService.addOrderDetailet(orderDetailet);

                cart.setDeleted(true);
                cartItemService.updateCart(cart);
            }
            return HttpResult.success(order);
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @GetMapping("/{id}")
    public HttpResult<Order> getOrderById(@PathVariable Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        Order order = orderService.getOrderById(id);
        if (order == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");

        boolean isAdmin = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) {
                isAdmin = true;
                break;
            }
        }
        if (isAdmin || order.getCustomer().equals(me))
            return HttpResult.success(order);

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @GetMapping("")
    public HttpResult<List<Order>> listOrdersByUserId(@RequestParam Long userId) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        User user = userService.getUserById(userId);
        if (user == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");

        boolean isAdmin = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) {
                isAdmin = true;
                break;
            }
        }
        if (isAdmin || me.equals(user)) {
            return HttpResult.success(orderService.getOrdersByCustomerId(userId));
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @PatchMapping("")
    public HttpResult<Order> updateOrderStatus(
            @RequestBody Order order) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (order == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order CANT BE NULL");
        if (order.getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order.Id CANT BE NULL");

        Order newOrder = orderService.getOrderById(order.getId());
        if (newOrder == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
        if (order.getOrderState() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order.OrderState CANT BE NULL");
        Integer orderState = order.getOrderState();
        if (orderState == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "OrderState CANT BE NULL");
        if (!OrderState.isValidOrderState(orderState))
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "OrderState NOT VALID");

        boolean isAdmin = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) {
                isAdmin = true;
                break;
            }
        }
        if (isAdmin || me.equals(newOrder.getCustomer())) {
            newOrder.setOrderState(orderState);
            LocalDateTime now = LocalDateTime.now();
            newOrder.setUpdateTime(now);
            newOrder.setUpdater(me.getId());
            orderService.updateOrder(newOrder);
            return HttpResult.success(newOrder);
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @GetMapping("/my")
    public HttpResult<List<Order>> getMyOrders() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        List<Order> myOrders = orderService.getOrdersByCustomerId(me.getId());
        return HttpResult.success(myOrders);
    }
}

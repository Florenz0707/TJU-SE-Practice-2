package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.constant.OrderState;
import cn.edu.tju.elm.exception.PointsException;
import cn.edu.tju.elm.model.BO.*;
import cn.edu.tju.elm.repository.PrivateVoucherRepository;
import cn.edu.tju.elm.service.*;
import cn.edu.tju.elm.service.serviceInterface.PrivateVoucherService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.EntityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "管理订单", description = "对订单进行增删改查")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final UserService userService;
    private final OrderService orderService;
    private final BusinessService businessService;
    private final AddressService addressService;
    private final CartItemService cartItemService;
    private final OrderDetailetService orderDetailetService;
    private final PointsService pointsService;
    private final PrivateVoucherRepository privateVoucherRepository;
    private final PrivateVoucherService privateVoucherService;

    public OrderController(UserService userService, OrderService orderService, BusinessService businessService,
                           AddressService addressService, CartItemService cartItemService, OrderDetailetService orderDetailetService,
                           PointsService pointsService, PrivateVoucherRepository privateVoucherRepository, 
                           PrivateVoucherService privateVoucherService) {
        this.userService = userService;
        this.orderService = orderService;
        this.businessService = businessService;
        this.addressService = addressService;
        this.cartItemService = cartItemService;
        this.orderDetailetService = orderDetailetService;
        this.pointsService = pointsService;
        this.privateVoucherRepository = privateVoucherRepository;
        this.privateVoucherService = privateVoucherService;
    }

    @PostMapping(value = "")
    public HttpResult<Order> addOrders(@RequestBody Order order) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (order == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order CANT BE NULL");
        if (order.getBusiness() == null || order.getBusiness().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business.Id CANT BE NULL");
        if (order.getDeliveryAddress() == null || order.getDeliveryAddress().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "DeliveryAddress.Id CANT BE NULL");

        Business business = businessService.getBusinessById(order.getBusiness().getId());
        if (business == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
        DeliveryAddress address = addressService.getAddressById(order.getDeliveryAddress().getId());
        if (address == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "DeliveryAddress NOT FOUND");

        List<Cart> cartList = cartItemService.getCart(business.getId(), me.getId());
        if (cartList.isEmpty())
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Customer's Cart IS EMPTY");

        if (me.equals(address.getCustomer())) {
            BigDecimal totalPrice = new BigDecimal(0);
            for (Cart cart : cartList) {
                BigDecimal quantity = new BigDecimal(cart.getQuantity());
                totalPrice = totalPrice.add(cart.getFood().getFoodPrice().multiply(quantity));
            }
            if (business.getDeliveryPrice() != null)
                totalPrice = totalPrice.add(business.getDeliveryPrice());
            if (business.getStartPrice() != null &&
                    totalPrice.compareTo(business.getStartPrice()) < 0)
                return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Order.TotalPrice IS LESS THAN BUSINESS START PRICE");

            // Handle voucher discount
            BigDecimal voucherDiscount = BigDecimal.ZERO;
            PrivateVoucher usedVoucher = null;
            if (order.getUsedVoucher() != null && order.getUsedVoucher().getId() != null) {
                Optional<PrivateVoucher> voucherOpt = privateVoucherRepository.findById(order.getUsedVoucher().getId());
                if (voucherOpt.isPresent()) {
                    usedVoucher = voucherOpt.get();
                    // Validate voucher ownership and validity
                    if (!usedVoucher.getWallet().getOwner().equals(me)) {
                        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "Voucher does not belong to you");
                    }
                    if (usedVoucher.getDeleted() != null && usedVoucher.getDeleted()) {
                        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Voucher has been used or expired");
                    }
                    if (usedVoucher.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
                        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Voucher has expired");
                    }
                    // Check if voucher threshold is met
                    if (usedVoucher.getPublicVoucher() != null) {
                        BigDecimal threshold = usedVoucher.getPublicVoucher().getThreshold();
                        if (threshold != null && totalPrice.compareTo(threshold) < 0) {
                            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Order total does not meet voucher threshold");
                        }
                    }
                    voucherDiscount = usedVoucher.getFaceValue();
                    if (voucherDiscount.compareTo(totalPrice) > 0) {
                        voucherDiscount = totalPrice; // Voucher cannot exceed order total
                    }
                }
            }

            // Handle points discount
            BigDecimal pointsDiscount = BigDecimal.ZERO;
            Integer pointsUsed = 0;
            if (order.getPointsUsed() != null && order.getPointsUsed() > 0) {
                pointsUsed = order.getPointsUsed();
                pointsDiscount = new BigDecimal(pointsUsed).divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);
                
                // Validate points
                BigDecimal maxPointsDiscount = totalPrice.subtract(voucherDiscount);
                if (pointsDiscount.compareTo(maxPointsDiscount) > 0) {
                    return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Points discount exceeds remaining order total");
                }
            }

            // Calculate final price
            BigDecimal finalPrice = totalPrice.subtract(voucherDiscount).subtract(pointsDiscount);
            if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
                finalPrice = BigDecimal.ZERO;
            }

            EntityUtils.setNewEntity(order);
            order.setOrderTotal(finalPrice);
            order.setOrderState(OrderState.PAID);
            order.setOrderDate(order.getCreateTime());
            order.setBusiness(business);
            order.setCustomer(me);
            order.setDeliveryAddress(address);
            order.setUsedVoucher(usedVoucher);
            order.setVoucherDiscount(voucherDiscount);
            order.setPointsUsed(pointsUsed);
            order.setPointsDiscount(pointsDiscount);
            
            // Redeem voucher if used
            if (usedVoucher != null) {
                try {
                    privateVoucherService.redeemPrivateVoucher(usedVoucher.getId());
                } catch (Exception e) {
                    log.error("Failed to redeem voucher: {}", e.getMessage());
                    return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to redeem voucher: " + e.getMessage());
                }
            }

            // Deduct points if used
            if (pointsUsed > 0) {
                try {
                    String tempOrderId = "TEMP_" + System.currentTimeMillis();
                    pointsService.freezePoints(me.getId(), pointsUsed, tempOrderId);
                    orderService.addOrder(order);
                    pointsService.deductPoints(me.getId(), tempOrderId, order.getId().toString());
                } catch (PointsException e) {
                    log.error("Failed to deduct points: {}", e.getMessage());
                    return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to deduct points: " + e.getMessage());
                }
            } else {
                orderService.addOrder(order);
            }

            for (Cart cart : cartList) {
                cartItemService.deleteCart(cart);

                OrderDetailet orderDetailet = new OrderDetailet();
                EntityUtils.setNewEntity(orderDetailet);
                orderDetailet.setOrder(order);
                orderDetailet.setFood(cart.getFood());
                orderDetailet.setQuantity(cart.getQuantity());
                orderDetailetService.addOrderDetailet(orderDetailet);
            }
            return HttpResult.success(order);
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @GetMapping("/{id}")
    public HttpResult<Order> getOrderById(@PathVariable Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        Order order = orderService.getOrderById(id);
        if (order == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        if (isAdmin || me.equals(order.getCustomer()))
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

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        if (isAdmin || me.equals(user))
            return HttpResult.success(orderService.getOrdersByCustomerId(userId));

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @PatchMapping("")
    public HttpResult<Order> updateOrderStatus(@RequestBody Order order) {
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
        if (order.getOrderState().equals(OrderState.CANCELED) ||
                !OrderState.isValidOrderState(orderState))
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "OrderState NOT VALID");

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
        if (isAdmin || (isBusiness && me.equals(newOrder.getBusiness().getBusinessOwner()))
                || me.equals(newOrder.getCustomer())) {
            newOrder.setOrderState(orderState);
            EntityUtils.updateEntity(newOrder);
            orderService.updateOrder(newOrder);
            return HttpResult.success(newOrder);
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @GetMapping("/user/my")
    public HttpResult<List<Order>> getMyOrders() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        User user = userService.getUserById(me.getId());
        if (user == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");

        return HttpResult.success(orderService.getOrdersByCustomerId(me.getId()));
    }

    @GetMapping("/merchant/my")
    public HttpResult<List<Order>> getMerchantOrders() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
        if (!isBusiness)
            return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");

        List<Business> myBusinesses = businessService.getBusinessByOwner(me);
        List<Order> myOrders = new ArrayList<>();

        for (Business business : myBusinesses) {
            List<Order> orders = orderService.getOrdersByBusinessId(business.getId());
            myOrders.addAll(orders);
        }

        return HttpResult.success(myOrders);
    }

    @GetMapping("/business/{id}")
    public HttpResult<List<Order>> getOrdersByBusinessId(@PathVariable Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        Business business = businessService.getBusinessById(id);
        if (business == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
        if (isAdmin || (isBusiness && me.equals(business.getBusinessOwner())))
            return HttpResult.success(orderService.getOrdersByBusinessId(business.getId()));

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
}

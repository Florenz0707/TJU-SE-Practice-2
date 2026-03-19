package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.service.OrderApplicationService;
import cn.edu.tju.elm.service.OrderService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "管理订单", description = "提供对订单的增删改查功能")
public class OrderController {
  private final UserService userService;
  private final OrderService orderService;
  private final BusinessService businessService;
  private final OrderApplicationService orderApplicationService;

  public OrderController(
      UserService userService,
      OrderService orderService,
      BusinessService businessService,
      OrderApplicationService orderApplicationService) {
    this.userService = userService;
    this.orderService = orderService;
    this.businessService = businessService;
    this.orderApplicationService = orderApplicationService;
  }

  @PostMapping(value = "")
  @Operation(summary = "创建订单", description = "顾客创建新订单，支持优惠券、积分和钱包支付")
  public HttpResult<Order> addOrders(
      @Parameter(description = "订单信息", required = true) @RequestBody Order order,
      @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    return orderApplicationService.addOrder(meOptional.get(), order, requestId);
  }

  @GetMapping("/{id}")
  @Operation(summary = "根据ID获取订单", description = "通过订单ID查询订单详细信息")
  public HttpResult<Order> getOrderById(
      @Parameter(description = "订单ID", required = true) @PathVariable Long id) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    Order order = orderService.getOrderById(id);
    if (order == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    if (isAdmin || me.equals(order.getCustomer())) return HttpResult.success(order);

    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @PostMapping("/{id}/cancel")
  @Operation(summary = "取消订单", description = "取消已支付订单，退还钱包余额、解冻积分、恢复优惠券")
  public HttpResult<Order> cancelOrder(
      @Parameter(description = "订单ID", required = true) @PathVariable Long id) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    return orderApplicationService.cancelOrder(meOptional.get(), id);
  }

  @GetMapping("")
  @Operation(summary = "根据用户ID获取订单列表", description = "查询指定用户的所有订单")
  public HttpResult<List<Order>> listOrdersByUserId(
      @Parameter(description = "用户ID", required = true) @RequestParam Long userId) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    User user = userService.getUserById(userId);
    if (user == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    if (isAdmin || me.equals(user))
      return HttpResult.success(orderService.getOrdersByCustomerId(userId));

    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @PatchMapping("")
  @Operation(summary = "更新订单状态", description = "更新订单状态，订单完成时自动发放积分")
  public HttpResult<Order> updateOrderStatus(
      @Parameter(description = "包含订单ID和新状态的订单对象", required = true) @RequestBody Order order) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    return orderApplicationService.updateOrderStatus(meOptional.get(), order);
  }

  @GetMapping("/user/my")
  @Operation(summary = "获取我的订单", description = "获取当前用户作为顾客的所有订单")
  public HttpResult<List<Order>> getMyOrders() {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    User user = userService.getUserById(me.getId());
    if (user == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");

    return HttpResult.success(orderService.getOrdersByCustomerId(me.getId()));
  }

  @GetMapping("/merchant/my")
  @Operation(summary = "获取商家订单", description = "获取当前商家用户的所有店铺订单")
  public HttpResult<List<Order>> getMerchantOrders() {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
    if (!isBusiness) return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");

    List<Business> myBusinesses = businessService.getBusinessByOwner(me);
    List<Order> myOrders = new ArrayList<>();

    for (Business business : myBusinesses) {
      List<Order> orders = orderService.getOrdersByBusinessId(business.getId());
      myOrders.addAll(orders);
    }

    return HttpResult.success(myOrders);
  }

  @GetMapping("/business/{id}")
  @Operation(summary = "根据店铺ID获取订单", description = "查询指定店铺的所有订单")
  public HttpResult<List<Order>> getOrdersByBusinessId(
      @Parameter(description = "店铺ID", required = true) @PathVariable Long id) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    Business business = businessService.getBusinessById(id);
    if (business == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
    if (isAdmin || (isBusiness && me.equals(business.getBusinessOwner())))
      return HttpResult.success(orderService.getOrdersByBusinessId(business.getId()));

    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }
}

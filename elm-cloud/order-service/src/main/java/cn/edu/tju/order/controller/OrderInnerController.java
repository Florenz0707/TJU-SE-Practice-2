package cn.edu.tju.order.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.order.model.vo.OrderDetailetVO;
import cn.edu.tju.order.model.vo.OrderSnapshotVO;
import cn.edu.tju.order.model.vo.PagedOrderSnapshotVO;
import cn.edu.tju.order.service.OrderInternalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
@RequestMapping("/api/inner/order")
@Tag(name = "订单内部接口", description = "order-service 基础联通接口")
public class OrderInnerController {
  private final OrderInternalService orderInternalService;

  public OrderInnerController(OrderInternalService orderInternalService) {
    this.orderInternalService = orderInternalService;
  }

  @GetMapping("/ping")
  @Operation(summary = "联通检查", description = "用于服务启动后快速验证内部路由可用性")
  public HttpResult<String> ping() {
    return HttpResult.success("pong");
  }

  @GetMapping("/{orderId}")
  @Operation(summary = "查询订单快照", description = "按订单ID查询订单基础数据")
  public HttpResult<OrderSnapshotVO> getOrderById(
      @Parameter(description = "订单ID", required = true) @PathVariable("orderId") Long orderId) {
    OrderSnapshotVO order = orderInternalService.getOrderById(orderId);
    if (order == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
    }
    return HttpResult.success(order);
  }

  @GetMapping("/by-request/{requestId}")
  @Operation(summary = "按请求ID查询订单", description = "用于幂等链路查询订单")
  public HttpResult<OrderSnapshotVO> getOrderByRequestId(
      @Parameter(description = "请求ID", required = true) @PathVariable("requestId") String requestId) {
    OrderSnapshotVO order = orderInternalService.getOrderByRequestId(requestId);
    if (order == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
    }
    return HttpResult.success(order);
  }

  @GetMapping("/customer/{customerId}")
  @Operation(summary = "按用户查询订单列表", description = "查询指定用户的订单快照列表")
  public HttpResult<List<OrderSnapshotVO>> getOrdersByCustomerId(
      @Parameter(description = "用户ID", required = true) @PathVariable("customerId") Long customerId) {
    return HttpResult.success(orderInternalService.getOrdersByCustomerId(customerId));
  }

  @GetMapping("/business/{businessId}")
  @Operation(summary = "按商家查询订单列表", description = "查询指定商家的订单快照列表")
  public HttpResult<List<OrderSnapshotVO>> getOrdersByBusinessId(
      @Parameter(description = "商家ID", required = true) @PathVariable("businessId") Long businessId) {
    return HttpResult.success(orderInternalService.getOrdersByBusinessId(businessId));
  }

  @GetMapping("/customer/{customerId}/page")
  @Operation(summary = "按用户分页查询订单", description = "查询指定用户的订单分页结果")
  public HttpResult<PagedOrderSnapshotVO> getOrdersByCustomerIdPage(
      @Parameter(description = "用户ID", required = true) @PathVariable("customerId") Long customerId,
      @Parameter(description = "页码(从1开始)", required = true) @RequestParam("page") Integer page,
      @Parameter(description = "每页数量", required = true) @RequestParam("size") Integer size) {
    if (page == null || size == null || page < 1 || size < 1) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "page/size NOT VALID");
    }
    return HttpResult.success(orderInternalService.getOrdersByCustomerId(customerId, page, size));
  }

  @GetMapping("/business/{businessId}/page")
  @Operation(summary = "按商家分页查询订单", description = "查询指定商家的订单分页结果")
  public HttpResult<PagedOrderSnapshotVO> getOrdersByBusinessIdPage(
      @Parameter(description = "商家ID", required = true) @PathVariable("businessId") Long businessId,
      @Parameter(description = "页码(从1开始)", required = true) @RequestParam("page") Integer page,
      @Parameter(description = "每页数量", required = true) @RequestParam("size") Integer size) {
    if (page == null || size == null || page < 1 || size < 1) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "page/size NOT VALID");
    }
    return HttpResult.success(orderInternalService.getOrdersByBusinessId(businessId, page, size));
  }

  @GetMapping("/{orderId}/details")
  @Operation(summary = "查询订单明细", description = "按订单ID查询订单明细")
  public HttpResult<List<OrderDetailetVO>> getOrderDetailetsByOrderId(
      @Parameter(description = "订单ID", required = true) @PathVariable("orderId") Long orderId) {
    return HttpResult.success(orderInternalService.getOrderDetailetsByOrderId(orderId));
  }

  @PostMapping("/create")
  @Operation(summary = "创建订单快照", description = "保存订单主信息与明细，按requestId幂等")
  public HttpResult<OrderSnapshotVO> createOrder(@RequestBody CreateOrderRequest request) {
    if (request == null || request.getRequestId() == null || request.getRequestId().isBlank()) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "RequestId CANT BE NULL");
    }
    if (request.getItems() == null || request.getItems().isEmpty()) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "OrderItems CANT BE EMPTY");
    }
    try {
      OrderSnapshotVO created =
          orderInternalService.createOrder(
              new OrderInternalService.CreateOrderCommand(
                  request.getRequestId(),
                  request.getCustomerId(),
                  request.getBusinessId(),
                  request.getDeliveryAddressId(),
                  request.getOrderTotal(),
                  request.getOrderState(),
                  request.getVoucherId(),
                  request.getVoucherDiscount(),
                  request.getPointsUsed(),
                  request.getPointsDiscount(),
                  request.getWalletPaid(),
                  request.getPointsTradeNo(),
                  request.getOrderDate(),
                  request.getItems().stream()
                      .map(
                          item ->
                              new OrderInternalService.OrderItemCommand(
                                  item.getFoodId(), item.getQuantity()))
                      .collect(Collectors.toList())));
      return HttpResult.success(created);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/{orderId}/cancel")
  @Operation(summary = "取消已支付订单", description = "仅订单所属用户可取消，状态从PAID变更为CANCELED")
  public HttpResult<OrderSnapshotVO> cancelOrder(
      @Parameter(description = "订单ID", required = true) @PathVariable("orderId") Long orderId,
      @RequestBody CancelOrderRequest request) {
    if (request == null || request.getOperatorUserId() == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "OperatorUserId CANT BE NULL");
    }
    try {
      OrderSnapshotVO canceled =
          orderInternalService.cancelPaidOrder(orderId, request.getOperatorUserId());
      return HttpResult.success(canceled);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/{orderId}/state")
  @Operation(summary = "更新订单状态", description = "内部订单状态流转接口")
  public HttpResult<OrderSnapshotVO> updateOrderState(
      @Parameter(description = "订单ID", required = true) @PathVariable("orderId") Long orderId,
      @RequestBody UpdateOrderStateRequest request) {
    if (request == null || request.getOrderState() == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "OrderState CANT BE NULL");
    }
    try {
      OrderSnapshotVO updated = orderInternalService.updateOrderState(orderId, request.getOrderState());
      return HttpResult.success(updated);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  public static class CreateOrderRequest {
    private String requestId;
    private Long customerId;
    private Long businessId;
    private Long deliveryAddressId;
    private BigDecimal orderTotal;
    private Integer orderState;
    private Long voucherId;
    private BigDecimal voucherDiscount;
    private Integer pointsUsed;
    private BigDecimal pointsDiscount;
    private BigDecimal walletPaid;
    private String pointsTradeNo;
    private LocalDateTime orderDate;
    private List<OrderItemRequest> items;

    public String getRequestId() {
      return requestId;
    }

    public void setRequestId(String requestId) {
      this.requestId = requestId;
    }

    public Long getCustomerId() {
      return customerId;
    }

    public void setCustomerId(Long customerId) {
      this.customerId = customerId;
    }

    public Long getBusinessId() {
      return businessId;
    }

    public void setBusinessId(Long businessId) {
      this.businessId = businessId;
    }

    public Long getDeliveryAddressId() {
      return deliveryAddressId;
    }

    public void setDeliveryAddressId(Long deliveryAddressId) {
      this.deliveryAddressId = deliveryAddressId;
    }

    public BigDecimal getOrderTotal() {
      return orderTotal;
    }

    public void setOrderTotal(BigDecimal orderTotal) {
      this.orderTotal = orderTotal;
    }

    public Integer getOrderState() {
      return orderState;
    }

    public void setOrderState(Integer orderState) {
      this.orderState = orderState;
    }

    public Long getVoucherId() {
      return voucherId;
    }

    public void setVoucherId(Long voucherId) {
      this.voucherId = voucherId;
    }

    public BigDecimal getVoucherDiscount() {
      return voucherDiscount;
    }

    public void setVoucherDiscount(BigDecimal voucherDiscount) {
      this.voucherDiscount = voucherDiscount;
    }

    public Integer getPointsUsed() {
      return pointsUsed;
    }

    public void setPointsUsed(Integer pointsUsed) {
      this.pointsUsed = pointsUsed;
    }

    public BigDecimal getPointsDiscount() {
      return pointsDiscount;
    }

    public void setPointsDiscount(BigDecimal pointsDiscount) {
      this.pointsDiscount = pointsDiscount;
    }

    public BigDecimal getWalletPaid() {
      return walletPaid;
    }

    public void setWalletPaid(BigDecimal walletPaid) {
      this.walletPaid = walletPaid;
    }

    public String getPointsTradeNo() {
      return pointsTradeNo;
    }

    public void setPointsTradeNo(String pointsTradeNo) {
      this.pointsTradeNo = pointsTradeNo;
    }

    public LocalDateTime getOrderDate() {
      return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
      this.orderDate = orderDate;
    }

    public List<OrderItemRequest> getItems() {
      return items;
    }

    public void setItems(List<OrderItemRequest> items) {
      this.items = items;
    }
  }

  public static class OrderItemRequest {
    private Long foodId;
    private Integer quantity;

    public Long getFoodId() {
      return foodId;
    }

    public void setFoodId(Long foodId) {
      this.foodId = foodId;
    }

    public Integer getQuantity() {
      return quantity;
    }

    public void setQuantity(Integer quantity) {
      this.quantity = quantity;
    }
  }

  public static class CancelOrderRequest {
    private Long operatorUserId;

    public Long getOperatorUserId() {
      return operatorUserId;
    }

    public void setOperatorUserId(Long operatorUserId) {
      this.operatorUserId = operatorUserId;
    }
  }

  public static class UpdateOrderStateRequest {
    private Integer orderState;

    public Integer getOrderState() {
      return orderState;
    }

    public void setOrderState(Integer orderState) {
      this.orderState = orderState;
    }
  }
}

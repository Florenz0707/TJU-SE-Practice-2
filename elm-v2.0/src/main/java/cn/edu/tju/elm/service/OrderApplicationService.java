package cn.edu.tju.elm.service;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.elm.constant.OrderState;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.DeliveryAddress;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.model.BO.PrivateVoucher;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.InternalAccountClient;
import cn.edu.tju.elm.utils.InternalCatalogClient;
import cn.edu.tju.elm.utils.InternalOrderClient;
import cn.edu.tju.elm.utils.InternalServiceClient;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderApplicationService {
  private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);

  private final BusinessService businessService;
  private final InternalAccountClient internalAccountClient;
  private final InternalCatalogClient internalCatalogClient;
  private final InternalOrderClient internalOrderClient;
  private final InternalServiceClient internalServiceClient;
  private final IntegrationOutboxService integrationOutboxService;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public OrderApplicationService(
      BusinessService businessService,
      InternalAccountClient internalAccountClient,
      InternalCatalogClient internalCatalogClient,
      InternalOrderClient internalOrderClient,
      InternalServiceClient internalServiceClient,
      IntegrationOutboxService integrationOutboxService,
      ResponseCompatibilityEnricher compatibilityEnricher) {
    this.businessService = businessService;
    this.internalAccountClient = internalAccountClient;
    this.internalCatalogClient = internalCatalogClient;
    this.internalOrderClient = internalOrderClient;
    this.internalServiceClient = internalServiceClient;
    this.integrationOutboxService = integrationOutboxService;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  @Transactional
  public HttpResult<Order> addOrder(Long currentUserId, Order order, String requestId) {
    if (requestId != null) {
      InternalOrderClient.OrderSnapshot existingOrderSnapshot =
          internalOrderClient.getOrderByRequestId(requestId);
      if (existingOrderSnapshot != null) {
        Order existingOrder = toOrderRef(existingOrderSnapshot);
        existingOrder.setBusiness(buildBusinessRef(existingOrderSnapshot.businessId()));
        existingOrder.setDeliveryAddress(
            buildAddressRef(existingOrderSnapshot.deliveryAddressId()));
        compatibilityEnricher.enrichOrder(existingOrder);
        return HttpResult.success(existingOrder);
      }
    }

    if (order == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order CANT BE NULL");
    if (order.getBusiness() == null || order.getBusiness().getId() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business.Id CANT BE NULL");
    if (order.getDeliveryAddress() == null || order.getDeliveryAddress().getId() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "DeliveryAddress.Id CANT BE NULL");

    InternalCatalogClient.BusinessSnapshot business =
        internalCatalogClient.getBusinessSnapshot(order.getBusiness().getId());
    if (business == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
    if (Boolean.TRUE.equals(business.deleted())) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Business NOT AVAILABLE");
    }

    if (business.openTime() != null && business.closeTime() != null) {
      java.time.LocalTime now = java.time.LocalTime.now();
      if (now.isBefore(business.openTime()) || now.isAfter(business.closeTime())) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "商家未营业");
      }
    }

    InternalOrderClient.AddressSnapshot addressSnapshot =
        internalOrderClient.getAddressById(order.getDeliveryAddress().getId());
    DeliveryAddress address =
        addressSnapshot == null ? null : buildAddressRef(addressSnapshot.id());
    if (addressSnapshot != null) {
      address.setCustomerId(addressSnapshot.customerId());
      address.setContactName(addressSnapshot.contactName());
      address.setContactSex(addressSnapshot.contactSex());
      address.setContactTel(addressSnapshot.contactTel());
      address.setAddress(addressSnapshot.address());
    }
    if (address == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "DeliveryAddress NOT FOUND");

    List<InternalOrderClient.CartSnapshot> cartList =
        internalOrderClient.getCartsByBusinessAndCustomerId(business.businessId(), currentUserId);
    if (cartList.isEmpty())
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Customer's Cart IS EMPTY");

    if (!currentUserId.equals(address.getCustomerId()))
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");

    Map<Long, InternalCatalogClient.FoodSnapshot> foodSnapshots = new HashMap<>();
    BigDecimal totalPrice = BigDecimal.ZERO;
    for (InternalOrderClient.CartSnapshot cart : cartList) {
      if (cart.foodId() == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food.Id CANT BE NULL");
      }
      Long foodId = cart.foodId();
      InternalCatalogClient.FoodSnapshot foodSnapshot =
          internalCatalogClient.getFoodSnapshot(foodId);
      if (foodSnapshot == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");
      }
      if (Boolean.TRUE.equals(foodSnapshot.deleted())) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Food NOT AVAILABLE");
      }
      if (!Objects.equals(foodSnapshot.businessId(), business.businessId())) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Food DOES NOT BELONG TO Business");
      }
      if (foodSnapshot.foodPrice() == null) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "FoodPrice NOT FOUND");
      }
      foodSnapshots.put(foodId, foodSnapshot);
      BigDecimal quantity = new BigDecimal(cart.quantity());
      totalPrice = totalPrice.add(foodSnapshot.foodPrice().multiply(quantity));
    }
    if (business.deliveryPrice() != null) totalPrice = totalPrice.add(business.deliveryPrice());
    if (business.startPrice() != null && totalPrice.compareTo(business.startPrice()) < 0)
      return HttpResult.failure(
          ResultCodeEnum.SERVER_ERROR, "Order.TotalPrice IS LESS THAN BUSINESS START PRICE");

    BigDecimal voucherDiscount = BigDecimal.ZERO;
    PrivateVoucher usedVoucher = null;
    if (order.getUsedVoucher() != null && order.getUsedVoucher().getId() != null) {
      InternalAccountClient.VoucherSnapshot voucherSnapshot =
          internalAccountClient.getVoucherSnapshot(order.getUsedVoucher().getId());
      if (voucherSnapshot == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Voucher NOT FOUND");
      }
      if (!currentUserId.equals(voucherSnapshot.ownerId())) {
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "Voucher does not belong to you");
      }
      if (Boolean.TRUE.equals(voucherSnapshot.deleted())) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Voucher has been used or expired");
      }
      if (voucherSnapshot.expiryDate() != null
          && voucherSnapshot.expiryDate().isBefore(java.time.LocalDateTime.now())) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Voucher has expired");
      }
      if (voucherSnapshot.threshold() != null
          && totalPrice.compareTo(voucherSnapshot.threshold()) < 0) {
        return HttpResult.failure(
            ResultCodeEnum.SERVER_ERROR, "Order total does not meet voucher threshold");
      }
      voucherDiscount =
          voucherSnapshot.faceValue() != null ? voucherSnapshot.faceValue() : BigDecimal.ZERO;
      if (voucherDiscount.compareTo(totalPrice) > 0) {
        voucherDiscount = totalPrice;
      }
      usedVoucher = new PrivateVoucher();
      usedVoucher.setId(voucherSnapshot.voucherId());
    }

    BigDecimal pointsDiscount = BigDecimal.ZERO;
    Integer pointsUsed = 0;
    if (order.getPointsUsed() != null && order.getPointsUsed() > 0) {
      pointsUsed = order.getPointsUsed();
      pointsDiscount =
          new BigDecimal(pointsUsed).divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);
      BigDecimal maxPointsDiscount = totalPrice.subtract(voucherDiscount);
      if (pointsDiscount.compareTo(maxPointsDiscount) > 0) {
        return HttpResult.failure(
            ResultCodeEnum.SERVER_ERROR, "Points discount exceeds remaining order total");
      }
    }

    BigDecimal walletPaid = BigDecimal.ZERO;
    if (order.getWalletPaid() != null && order.getWalletPaid().compareTo(BigDecimal.ZERO) > 0) {
      walletPaid = order.getWalletPaid();
      InternalAccountClient.WalletSnapshot userWallet =
          internalAccountClient.getWalletByUserId(currentUserId, true);
      if (userWallet == null) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to load wallet");
      }

      BigDecimal maxWalletPaid = totalPrice.subtract(voucherDiscount).subtract(pointsDiscount);
      if (walletPaid.compareTo(maxWalletPaid) > 0) {
        return HttpResult.failure(
            ResultCodeEnum.SERVER_ERROR, "Wallet payment exceeds remaining order total");
      }

      if (userWallet.balance() == null || userWallet.balance().compareTo(walletPaid) < 0) {
        return HttpResult.failure(
            ResultCodeEnum.SERVER_ERROR,
            "Insufficient wallet balance. Current balance: "
                + userWallet.balance()
                + ", Required: "
                + walletPaid);
      }
    }

    for (InternalOrderClient.CartSnapshot cart : cartList) {
      InternalCatalogClient.FoodSnapshot foodSnapshot = foodSnapshots.get(cart.foodId());
      if (foodSnapshot == null
          || foodSnapshot.stock() == null
          || foodSnapshot.stock() < cart.quantity()) {
        String foodName = cart.foodId() != null ? "商品#" + cart.foodId() : "未知商品";
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "商品 " + foodName + " 库存不足");
      }
    }

    EntityUtils.setNewEntity(order);
    order.setOrderTotal(totalPrice);
    order.setOrderState(OrderState.PAID);
    order.setOrderDate(order.getCreateTime());
    order.setBusiness(buildBusinessRef(business.businessId()));
    order.setCustomerId(currentUserId);
    order.setDeliveryAddress(address);
    order.setUsedVoucher(usedVoucher);
    order.setVoucherDiscount(voucherDiscount);
    order.setPointsUsed(pointsUsed);
    order.setPointsDiscount(pointsDiscount);
    order.setWalletPaid(walletPaid);
    String createRequestId =
        (requestId == null || requestId.isBlank())
            ? "order-create-" + UUID.randomUUID()
            : requestId;
    order.setRequestId(createRequestId);
    String pointsTradeNo = null;
    String orderBizId = requestId != null ? requestId : "ORDER_" + UUID.randomUUID();
    String voucherRedeemRequestId = buildInternalRequestId(requestId, "voucher-redeem");
    String voucherRollbackRequestId = buildInternalRequestId(requestId, "voucher-rollback");
    String walletDebitRequestId = buildInternalRequestId(requestId, "wallet-debit");
    String walletRefundRequestId = buildInternalRequestId(requestId, "wallet-refund");
    String stockReserveRequestId = buildInternalRequestId(requestId, "stock-reserve");
    String stockRollbackRequestId = buildInternalRequestId(requestId, "stock-reserve-rollback");
    List<InternalOrderClient.OrderItemCommand> orderItems =
        cartList.stream()
            .map(cart -> new InternalOrderClient.OrderItemCommand(cart.foodId(), cart.quantity()))
            .collect(Collectors.toList());
    List<InternalCatalogClient.StockItem> stockItems =
        cartList.stream()
            .map(cart -> new InternalCatalogClient.StockItem(cart.foodId(), cart.quantity()))
            .collect(Collectors.toList());

    if (usedVoucher != null) {
      boolean redeemed =
          internalAccountClient.redeemVoucher(
              voucherRedeemRequestId, currentUserId, usedVoucher.getId(), orderBizId);
      if (!redeemed) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to redeem voucher");
      }
    }

    if (walletPaid.compareTo(BigDecimal.ZERO) > 0) {
      boolean walletDebited =
          internalAccountClient.debitWallet(
              walletDebitRequestId, currentUserId, walletPaid, orderBizId, "order wallet payment");
      if (!walletDebited) {
        if (usedVoucher != null) {
          internalAccountClient.rollbackVoucher(
              voucherRollbackRequestId,
              currentUserId,
              usedVoucher.getId(),
              orderBizId,
              "wallet debit failed");
        }
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to deduct wallet balance");
      }
    }

    boolean stockReserved =
        internalCatalogClient.reserveStock(stockReserveRequestId, orderBizId, stockItems);
    if (!stockReserved) {
      if (walletPaid.compareTo(BigDecimal.ZERO) > 0) {
        internalAccountClient.refundWallet(
            walletRefundRequestId,
            currentUserId,
            walletPaid,
            orderBizId,
            "stock reserve failed rollback");
      }
      if (usedVoucher != null) {
        internalAccountClient.rollbackVoucher(
            voucherRollbackRequestId,
            currentUserId,
            usedVoucher.getId(),
            orderBizId,
            "stock reserve failed rollback");
      }
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to reserve stock");
    }

    if (pointsUsed > 0) {
      try {
        pointsTradeNo = "ORDER_" + UUID.randomUUID();
        java.util.Map<String, Object> freezeResult =
            internalServiceClient.freezePoints(currentUserId, pointsUsed, pointsTradeNo);
        if (freezeResult == null) {
          return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to freeze points");
        }
        order.setPointsTradeNo(pointsTradeNo);
        Boolean deductSuccess =
            internalServiceClient.deductPoints(currentUserId, pointsTradeNo, pointsTradeNo);
        if (!Boolean.TRUE.equals(deductSuccess)) {
          throw new IllegalStateException("failed to deduct points");
        }
      } catch (Exception e) {
        log.error("Failed to deduct points: {}", e.getMessage());
        if (walletPaid.compareTo(BigDecimal.ZERO) > 0) {
          internalAccountClient.refundWallet(
              walletRefundRequestId,
              currentUserId,
              walletPaid,
              orderBizId,
              "points deduct failed rollback");
        }
        if (usedVoucher != null) {
          internalAccountClient.rollbackVoucher(
              voucherRollbackRequestId,
              currentUserId,
              usedVoucher.getId(),
              orderBizId,
              "points deduct failed rollback");
        }
        internalCatalogClient.releaseStock(stockRollbackRequestId, orderBizId, stockItems);
        return HttpResult.failure(
            ResultCodeEnum.SERVER_ERROR, "Failed to deduct points: " + e.getMessage());
      }
    }

    try {
      InternalOrderClient.OrderSnapshot createdOrderSnapshot =
          internalOrderClient.createOrder(
              new InternalOrderClient.CreateOrderCommand(
                  createRequestId,
                  currentUserId,
                  business.businessId(),
                  address.getId(),
                  totalPrice,
                  OrderState.PAID,
                  usedVoucher == null ? null : usedVoucher.getId(),
                  voucherDiscount,
                  pointsUsed,
                  pointsDiscount,
                  walletPaid,
                  pointsTradeNo,
                  order.getOrderDate(),
                  orderItems));
      if (createdOrderSnapshot == null || createdOrderSnapshot.id() == null) {
        throw new IllegalStateException("Failed to persist order");
      }
      order.setId(createdOrderSnapshot.id());
      order.setCreateTime(createdOrderSnapshot.orderDate());
      order.setUpdateTime(createdOrderSnapshot.orderDate());
    } catch (Exception e) {
      if (pointsUsed > 0 && pointsTradeNo != null) {
        internalServiceClient.refundDeductedPoints(currentUserId, pointsTradeNo, "订单创建失败返还积分");
      }
      if (walletPaid.compareTo(BigDecimal.ZERO) > 0) {
        internalAccountClient.refundWallet(
            walletRefundRequestId, currentUserId, walletPaid, orderBizId, "order persist failed");
      }
      if (usedVoucher != null) {
        internalAccountClient.rollbackVoucher(
            voucherRollbackRequestId,
            currentUserId,
            usedVoucher.getId(),
            orderBizId,
            "order persist failed");
      }
      internalCatalogClient.releaseStock(stockRollbackRequestId, orderBizId, stockItems);
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to persist order");
    }

    try {
      for (InternalOrderClient.CartSnapshot cart : cartList) {
        if (cart.id() != null) {
          internalOrderClient.deleteCart(cart.id());
        }
      }
    } catch (Exception e) {
      log.warn("Failed to cleanup cart items after order creation: {}", e.getMessage());
    }

    log.info(
        "Order created: orderId={}, userId={}, total={}", order.getId(), currentUserId, totalPrice);
    compatibilityEnricher.enrichOrder(order);
    return HttpResult.success(order);
  }

  @Transactional
  public HttpResult<Order> cancelOrder(Long currentUserId, Long id) {
    InternalOrderClient.OrderSnapshot snapshot = internalOrderClient.getOrderById(id);
    if (snapshot == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
    Order order = toOrderRef(snapshot);

    if (!currentUserId.equals(order.getCustomerId()))
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");

    if (!order.getOrderState().equals(OrderState.PAID)) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "只能取消已支付订单");
    }

    try {
      if (order.getWalletPaid() != null && order.getWalletPaid().compareTo(BigDecimal.ZERO) > 0) {
        String orderBizId = "ORDER_" + order.getId();
        boolean walletRefunded =
            internalAccountClient.refundWallet(
                "order-cancel-" + order.getId() + "-wallet-refund",
                currentUserId,
                order.getWalletPaid(),
                orderBizId,
                "order cancel refund");
        if (!walletRefunded) {
          return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "取消订单失败: 钱包退款失败");
        }
      }

      if (order.getPointsUsed() != null && order.getPointsUsed() > 0) {
        String orderBizId =
            order.getPointsTradeNo() != null ? order.getPointsTradeNo() : "ORDER_" + order.getId();
        boolean refunded =
            internalServiceClient.refundDeductedPoints(currentUserId, orderBizId, "订单取消返还积分");
        if (!refunded) {
          internalServiceClient.rollbackPoints(currentUserId, orderBizId, "订单取消");
        }
      }

      if (order.getUsedVoucher() != null) {
        boolean voucherRolledBack =
            internalAccountClient.rollbackVoucher(
                "order-cancel-" + order.getId() + "-voucher-rollback",
                currentUserId,
                order.getUsedVoucher().getId(),
                "ORDER_" + order.getId(),
                "order cancel rollback voucher");
        if (!voucherRolledBack) {
          return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "取消订单失败: 优惠券回滚失败");
        }
      }

      List<InternalOrderClient.OrderDetailSnapshot> orderDetails =
          internalOrderClient.getOrderDetailsByOrderId(order.getId());
      List<InternalCatalogClient.StockItem> stockItems =
          orderDetails.stream()
              .map(
                  detail -> new InternalCatalogClient.StockItem(detail.foodId(), detail.quantity()))
              .collect(Collectors.toList());
      boolean stockReleased =
          internalCatalogClient.releaseStock(
              "order-cancel-" + order.getId() + "-stock-release",
              "ORDER_" + order.getId(),
              stockItems);
      if (!stockReleased) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "取消订单失败: 库存回补失败");
      }

      InternalOrderClient.OrderSnapshot canceledSnapshot =
          internalOrderClient.cancelOrder(order.getId(), currentUserId);
      if (canceledSnapshot == null) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "取消订单失败: 订单状态更新失败");
      }
      order = toOrderRef(canceledSnapshot);

      log.warn(
          "Order canceled: orderId={}, userId={}, reason=user_request",
          order.getId(),
          currentUserId);
      compatibilityEnricher.enrichOrder(order);
      return HttpResult.success(order);
    } catch (Exception e) {
      log.error("Failed to cancel order: {}", e.getMessage());
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "取消订单失败: " + e.getMessage());
    }
  }

  @Transactional
  public HttpResult<Order> updateOrderStatus(
      Long currentUserId, boolean isAdmin, boolean isBusiness, Order order) {
    if (order == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order CANT BE NULL");
    if (order.getId() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order.Id CANT BE NULL");

    InternalOrderClient.OrderSnapshot currentSnapshot =
        internalOrderClient.getOrderById(order.getId());
    Order newOrder = currentSnapshot == null ? null : toOrderRef(currentSnapshot);
    if (newOrder == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
    if (order.getOrderState() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order.OrderState CANT BE NULL");
    Integer orderState = order.getOrderState();
    if (!OrderState.isValidOrderState(orderState))
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "OrderState NOT VALID");

    if (!isValidStateTransition(newOrder.getOrderState(), orderState))
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "非法的状态转换");

    Business orderBusiness =
        newOrder.getBusiness() != null && newOrder.getBusiness().getId() != null
            ? businessService.getBusinessById(newOrder.getBusiness().getId())
            : null;
    if (!(isAdmin
        || (isBusiness
            && orderBusiness != null
            && currentUserId.equals(orderBusiness.getBusinessOwnerId()))
        || currentUserId.equals(newOrder.getCustomerId()))) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    Integer oldOrderState = newOrder.getOrderState();
    InternalOrderClient.OrderSnapshot updatedSnapshot =
        internalOrderClient.updateOrderState(order.getId(), orderState);
    if (updatedSnapshot == null) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to update order state");
    }
    newOrder = toOrderRef(updatedSnapshot);

    if (orderState.equals(OrderState.COMPLETE) && !oldOrderState.equals(OrderState.COMPLETE)) {
      try {
        BigDecimal pointsBase =
            newOrder.getOrderTotal() != null ? newOrder.getOrderTotal() : BigDecimal.ZERO;
        if (newOrder.getVoucherDiscount() != null) {
          pointsBase = pointsBase.subtract(newOrder.getVoucherDiscount());
        }
        if (newOrder.getPointsDiscount() != null) {
          pointsBase = pointsBase.subtract(newOrder.getPointsDiscount());
        }
        if (pointsBase.compareTo(BigDecimal.ZERO) < 0) {
          pointsBase = BigDecimal.ZERO;
        }

        Double orderAmount = pointsBase.doubleValue();
        integrationOutboxService.enqueuePointsOrderSuccess(
            newOrder.getCustomerId(),
            newOrder.getId().toString(),
            orderAmount,
            newOrder.getOrderDate() != null ? newOrder.getOrderDate().toString() : null,
            "订单完成");
      } catch (Exception e) {
        log.error("Failed to notify order success for points: {}", e.getMessage());
      }
    }

    compatibilityEnricher.enrichOrder(newOrder);
    return HttpResult.success(newOrder);
  }

  @Transactional(readOnly = true)
  public Order getOrderById(Long id) {
    if (id == null) {
      return null;
    }
    InternalOrderClient.OrderSnapshot snapshot = internalOrderClient.getOrderById(id);
    if (snapshot == null) {
      return null;
    }
    Order order = toOrderRef(snapshot);
    compatibilityEnricher.enrichOrder(order);
    return order;
  }

  @Transactional(readOnly = true)
  public List<Order> getOrdersByCustomerId(Long customerId) {
    if (customerId == null) {
      return List.of();
    }
    List<Order> orders =
        internalOrderClient.getOrdersByCustomerId(customerId).stream()
            .map(this::toOrderRef)
            .collect(Collectors.toList());
    compatibilityEnricher.enrichOrders(orders);
    return orders;
  }

  @Transactional(readOnly = true)
  public List<Order> getOrdersByBusinessId(Long businessId) {
    if (businessId == null) {
      return List.of();
    }
    List<Order> orders =
        internalOrderClient.getOrdersByBusinessId(businessId).stream()
            .map(this::toOrderRef)
            .collect(Collectors.toList());
    compatibilityEnricher.enrichOrders(orders);
    return orders;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getOrdersByCustomerId(Long customerId, int page, int size) {
    InternalOrderClient.PagedOrderSnapshot pagedSnapshot =
        internalOrderClient.getOrdersByCustomerId(customerId, page, size);
    List<Order> orders =
        pagedSnapshot.orders().stream().map(this::toOrderRef).collect(Collectors.toList());
    compatibilityEnricher.enrichOrders(orders);
    Map<String, Object> result = new HashMap<>();
    result.put("records", orders);
    result.put("total", pagedSnapshot.total());
    result.put("page", pagedSnapshot.page());
    result.put("size", pagedSnapshot.size());
    return result;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getOrdersByBusinessId(Long businessId, int page, int size) {
    InternalOrderClient.PagedOrderSnapshot pagedSnapshot =
        internalOrderClient.getOrdersByBusinessId(businessId, page, size);
    List<Order> orders =
        pagedSnapshot.orders().stream().map(this::toOrderRef).collect(Collectors.toList());
    compatibilityEnricher.enrichOrders(orders);
    Map<String, Object> result = new HashMap<>();
    result.put("records", orders);
    result.put("total", pagedSnapshot.total());
    result.put("page", pagedSnapshot.page());
    result.put("size", pagedSnapshot.size());
    return result;
  }

  private String buildInternalRequestId(String requestId, String action) {
    if (requestId == null || requestId.isEmpty()) {
      return "order-" + action + "-" + UUID.randomUUID();
    }
    return requestId + ":" + action;
  }

  private boolean isValidStateTransition(Integer from, Integer to) {
    if (from == null || to == null) {
      return false;
    }
    if (from.equals(OrderState.CANCELED) || from.equals(OrderState.COMMENTED)) {
      return false;
    }
    if (to.equals(OrderState.CANCELED)) {
      return from.equals(OrderState.PAID);
    }
    return to > from;
  }

  private Business buildBusinessRef(Long businessId) {
    Business business = new Business();
    business.setId(businessId);
    return business;
  }

  private DeliveryAddress buildAddressRef(Long addressId) {
    DeliveryAddress address = new DeliveryAddress();
    address.setId(addressId);
    return address;
  }

  private Order toOrderRef(InternalOrderClient.OrderSnapshot snapshot) {
    Order order = new Order();
    order.setId(snapshot.id());
    order.setCustomerId(snapshot.customerId());
    order.setBusiness(buildBusinessRef(snapshot.businessId()));
    order.setDeliveryAddress(buildAddressRef(snapshot.deliveryAddressId()));
    order.setOrderState(snapshot.orderState());
    order.setOrderTotal(snapshot.orderTotal());
    if (snapshot.voucherId() != null) {
      PrivateVoucher voucher = new PrivateVoucher();
      voucher.setId(snapshot.voucherId());
      order.setUsedVoucher(voucher);
    }
    order.setVoucherDiscount(snapshot.voucherDiscount());
    order.setPointsUsed(snapshot.pointsUsed());
    order.setPointsDiscount(snapshot.pointsDiscount());
    order.setWalletPaid(snapshot.walletPaid());
    order.setPointsTradeNo(snapshot.pointsTradeNo());
    order.setRequestId(snapshot.requestId());
    order.setOrderDate(snapshot.orderDate());
    return order;
  }
}

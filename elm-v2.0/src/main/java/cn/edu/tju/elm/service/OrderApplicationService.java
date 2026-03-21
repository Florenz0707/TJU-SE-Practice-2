package cn.edu.tju.elm.service;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.elm.constant.OrderState;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Cart;
import cn.edu.tju.elm.model.BO.DeliveryAddress;
import cn.edu.tju.elm.model.BO.Food;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.model.BO.OrderDetailet;
import cn.edu.tju.elm.model.BO.PrivateVoucher;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.InternalAccountClient;
import cn.edu.tju.elm.utils.InternalCatalogClient;
import cn.edu.tju.elm.utils.InternalServiceClient;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderApplicationService {
  private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);

  private final OrderService orderService;
  private final FoodService foodService;
  private final AddressService addressService;
  private final CartItemService cartItemService;
  private final OrderDetailetService orderDetailetService;
  private final InternalAccountClient internalAccountClient;
  private final InternalCatalogClient internalCatalogClient;
  private final InternalServiceClient internalServiceClient;
  private final IntegrationOutboxService integrationOutboxService;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public OrderApplicationService(
      OrderService orderService,
      FoodService foodService,
      AddressService addressService,
      CartItemService cartItemService,
      OrderDetailetService orderDetailetService,
      InternalAccountClient internalAccountClient,
      InternalCatalogClient internalCatalogClient,
      InternalServiceClient internalServiceClient,
      IntegrationOutboxService integrationOutboxService,
      ResponseCompatibilityEnricher compatibilityEnricher) {
    this.orderService = orderService;
    this.foodService = foodService;
    this.addressService = addressService;
    this.cartItemService = cartItemService;
    this.orderDetailetService = orderDetailetService;
    this.internalAccountClient = internalAccountClient;
    this.internalCatalogClient = internalCatalogClient;
    this.internalServiceClient = internalServiceClient;
    this.integrationOutboxService = integrationOutboxService;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  @Transactional
  public HttpResult<Order> addOrder(Long currentUserId, Order order, String requestId) {
    if (requestId != null) {
      Order existingOrder = orderService.getOrderByRequestId(requestId);
      if (existingOrder != null) {
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

    DeliveryAddress address = addressService.getAddressById(order.getDeliveryAddress().getId());
    if (address == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "DeliveryAddress NOT FOUND");

    List<Cart> cartList = cartItemService.getCart(business.businessId(), currentUserId);
    if (cartList.isEmpty())
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Customer's Cart IS EMPTY");

    if (!currentUserId.equals(address.getCustomerId()))
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");

    Map<Long, InternalCatalogClient.FoodSnapshot> foodSnapshots = new HashMap<>();
    BigDecimal totalPrice = BigDecimal.ZERO;
    for (Cart cart : cartList) {
      if (cart.getFood() == null || cart.getFood().getId() == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food.Id CANT BE NULL");
      }
      Long foodId = cart.getFood().getId();
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
      BigDecimal quantity = new BigDecimal(cart.getQuantity());
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

    for (Cart cart : cartList) {
      InternalCatalogClient.FoodSnapshot foodSnapshot = foodSnapshots.get(cart.getFood().getId());
      if (foodSnapshot == null
          || foodSnapshot.stock() == null
          || foodSnapshot.stock() < cart.getQuantity()) {
        String foodName =
            cart.getFood().getFoodName() != null ? cart.getFood().getFoodName() : "未知商品";
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
    order.setRequestId(requestId);
    String pointsTradeNo = null;
    String orderBizId = requestId != null ? requestId : "ORDER_" + UUID.randomUUID();
    String voucherRedeemRequestId = buildInternalRequestId(requestId, "voucher-redeem");
    String voucherRollbackRequestId = buildInternalRequestId(requestId, "voucher-rollback");
    String walletDebitRequestId = buildInternalRequestId(requestId, "wallet-debit");
    String walletRefundRequestId = buildInternalRequestId(requestId, "wallet-refund");

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

    if (pointsUsed > 0) {
      try {
        pointsTradeNo = "ORDER_" + UUID.randomUUID();
        java.util.Map<String, Object> freezeResult =
            internalServiceClient.freezePoints(currentUserId, pointsUsed, pointsTradeNo);
        if (freezeResult == null) {
          return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to freeze points");
        }
        order.setPointsTradeNo(pointsTradeNo);
        orderService.addOrder(order);
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
        return HttpResult.failure(
            ResultCodeEnum.SERVER_ERROR, "Failed to deduct points: " + e.getMessage());
      }
    } else {
      orderService.addOrder(order);
    }

    for (Cart cart : cartList) {
      Food food = cart.getFood();
      if (food == null || food.getId() == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food.Id CANT BE NULL");
      }
      if (!food.decreaseStock(cart.getQuantity())) {
        return HttpResult.failure(
            ResultCodeEnum.SERVER_ERROR, "商品 " + food.getFoodName() + " 库存不足");
      }
      EntityUtils.updateEntity(food);
      foodService.updateFood(food);

      cartItemService.deleteCart(cart);

      OrderDetailet orderDetailet = new OrderDetailet();
      EntityUtils.setNewEntity(orderDetailet);
      orderDetailet.setOrder(order);
      orderDetailet.setFood(cart.getFood());
      orderDetailet.setQuantity(cart.getQuantity());
      orderDetailetService.addOrderDetailet(orderDetailet);
    }

    log.info(
        "Order created: orderId={}, userId={}, total={}", order.getId(), currentUserId, totalPrice);
    compatibilityEnricher.enrichOrder(order);
    return HttpResult.success(order);
  }

  @Transactional
  public HttpResult<Order> cancelOrder(Long currentUserId, Long id) {
    Order order = orderService.getOrderById(id);
    if (order == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");

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

      List<OrderDetailet> orderDetails =
          orderDetailetService.getOrderDetailetsByOrderId(order.getId());
      for (OrderDetailet detail : orderDetails) {
        Food food = detail.getFood();
        food.increaseStock(detail.getQuantity());
        EntityUtils.updateEntity(food);
        foodService.updateFood(food);
      }

      order.setOrderState(OrderState.CANCELED);
      EntityUtils.updateEntity(order);
      orderService.updateOrder(order);

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

    Order newOrder = orderService.getOrderById(order.getId());
    if (newOrder == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
    if (order.getOrderState() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order.OrderState CANT BE NULL");
    Integer orderState = order.getOrderState();
    if (!OrderState.isValidOrderState(orderState))
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "OrderState NOT VALID");

    if (!orderService.isValidStateTransition(newOrder.getOrderState(), orderState))
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "非法的状态转换");

    if (!(isAdmin
        || (isBusiness && currentUserId.equals(newOrder.getBusiness().getBusinessOwnerId()))
        || currentUserId.equals(newOrder.getCustomerId()))) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    Integer oldOrderState = newOrder.getOrderState();
    newOrder.setOrderState(orderState);
    EntityUtils.updateEntity(newOrder);
    orderService.updateOrder(newOrder);

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

  private String buildInternalRequestId(String requestId, String action) {
    if (requestId == null || requestId.isEmpty()) {
      return "order-" + action + "-" + UUID.randomUUID();
    }
    return requestId + ":" + action;
  }

  private Business buildBusinessRef(Long businessId) {
    Business business = new Business();
    business.setId(businessId);
    return business;
  }
}

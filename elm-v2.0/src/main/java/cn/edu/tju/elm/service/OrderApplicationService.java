package cn.edu.tju.elm.service;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.elm.constant.OrderState;
import cn.edu.tju.elm.exception.PointsException;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Cart;
import cn.edu.tju.elm.model.BO.DeliveryAddress;
import cn.edu.tju.elm.model.BO.Food;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.model.BO.OrderDetailet;
import cn.edu.tju.elm.model.BO.PrivateVoucher;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.repository.PrivateVoucherRepository;
import cn.edu.tju.elm.repository.WalletRepository;
import cn.edu.tju.elm.service.serviceInterface.PrivateVoucherService;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderApplicationService {
  private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);

  private final OrderService orderService;
  private final BusinessService businessService;
  private final FoodService foodService;
  private final AddressService addressService;
  private final CartItemService cartItemService;
  private final OrderDetailetService orderDetailetService;
  private final PointsService pointsService;
  private final IntegrationOutboxService integrationOutboxService;
  private final PrivateVoucherRepository privateVoucherRepository;
  private final PrivateVoucherService privateVoucherService;
  private final WalletRepository walletRepository;
  private final WalletService walletService;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public OrderApplicationService(
      OrderService orderService,
      BusinessService businessService,
      FoodService foodService,
      AddressService addressService,
      CartItemService cartItemService,
      OrderDetailetService orderDetailetService,
      PointsService pointsService,
      IntegrationOutboxService integrationOutboxService,
      PrivateVoucherRepository privateVoucherRepository,
      PrivateVoucherService privateVoucherService,
      WalletRepository walletRepository,
      WalletService walletService,
      ResponseCompatibilityEnricher compatibilityEnricher) {
    this.orderService = orderService;
    this.businessService = businessService;
    this.foodService = foodService;
    this.addressService = addressService;
    this.cartItemService = cartItemService;
    this.orderDetailetService = orderDetailetService;
    this.pointsService = pointsService;
    this.integrationOutboxService = integrationOutboxService;
    this.privateVoucherRepository = privateVoucherRepository;
    this.privateVoucherService = privateVoucherService;
    this.walletRepository = walletRepository;
    this.walletService = walletService;
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

    Business business = businessService.getBusinessById(order.getBusiness().getId());
    if (business == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");

    if (business.getOpenTime() != null && business.getCloseTime() != null) {
      java.time.LocalTime now = java.time.LocalTime.now();
      if (now.isBefore(business.getOpenTime()) || now.isAfter(business.getCloseTime())) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "商家未营业");
      }
    }

    DeliveryAddress address = addressService.getAddressById(order.getDeliveryAddress().getId());
    if (address == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "DeliveryAddress NOT FOUND");

    List<Cart> cartList = cartItemService.getCart(business.getId(), currentUserId);
    if (cartList.isEmpty())
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Customer's Cart IS EMPTY");

    if (!currentUserId.equals(address.getCustomerId()))
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");

    BigDecimal totalPrice = BigDecimal.ZERO;
    for (Cart cart : cartList) {
      BigDecimal quantity = new BigDecimal(cart.getQuantity());
      totalPrice = totalPrice.add(cart.getFood().getFoodPrice().multiply(quantity));
    }
    if (business.getDeliveryPrice() != null)
      totalPrice = totalPrice.add(business.getDeliveryPrice());
    if (business.getStartPrice() != null && totalPrice.compareTo(business.getStartPrice()) < 0)
      return HttpResult.failure(
          ResultCodeEnum.SERVER_ERROR, "Order.TotalPrice IS LESS THAN BUSINESS START PRICE");

    BigDecimal voucherDiscount = BigDecimal.ZERO;
    PrivateVoucher usedVoucher = null;
    if (order.getUsedVoucher() != null && order.getUsedVoucher().getId() != null) {
      Optional<PrivateVoucher> voucherOpt =
          privateVoucherRepository.findById(order.getUsedVoucher().getId());
      if (voucherOpt.isPresent()) {
        usedVoucher = voucherOpt.get();
        if (!currentUserId.equals(usedVoucher.getWallet().getOwnerId())) {
          return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "Voucher does not belong to you");
        }
        if (usedVoucher.getDeleted() != null && usedVoucher.getDeleted()) {
          return HttpResult.failure(
              ResultCodeEnum.SERVER_ERROR, "Voucher has been used or expired");
        }
        if (usedVoucher.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
          return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Voucher has expired");
        }
        if (usedVoucher.getPublicVoucher() != null) {
          BigDecimal threshold = usedVoucher.getPublicVoucher().getThreshold();
          if (threshold != null && totalPrice.compareTo(threshold) < 0) {
            return HttpResult.failure(
                ResultCodeEnum.SERVER_ERROR, "Order total does not meet voucher threshold");
          }
        }
        voucherDiscount = usedVoucher.getFaceValue();
        if (voucherDiscount.compareTo(totalPrice) > 0) {
          voucherDiscount = totalPrice;
        }
      }
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
    Wallet userWallet = null;
    if (order.getWalletPaid() != null && order.getWalletPaid().compareTo(BigDecimal.ZERO) > 0) {
      walletPaid = order.getWalletPaid();

      Optional<Wallet> walletOpt = walletRepository.findByOwnerId(currentUserId);
      if (walletOpt.isEmpty()) {
        try {
          walletService.createWallet(currentUserId);
          walletOpt = walletRepository.findByOwnerId(currentUserId);
          if (walletOpt.isEmpty()) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to create wallet");
          }
        } catch (Exception e) {
          log.error("Failed to create wallet: {}", e.getMessage());
          return HttpResult.failure(
              ResultCodeEnum.SERVER_ERROR, "Failed to create wallet: " + e.getMessage());
        }
      }
      userWallet = walletOpt.get();

      BigDecimal maxWalletPaid = totalPrice.subtract(voucherDiscount).subtract(pointsDiscount);
      if (walletPaid.compareTo(maxWalletPaid) > 0) {
        return HttpResult.failure(
            ResultCodeEnum.SERVER_ERROR, "Wallet payment exceeds remaining order total");
      }

      if (userWallet.getBalance().compareTo(walletPaid) < 0) {
        return HttpResult.failure(
            ResultCodeEnum.SERVER_ERROR,
            "Insufficient wallet balance. Current balance: "
                + userWallet.getBalance()
                + ", Required: "
                + walletPaid);
      }
    }

    for (Cart cart : cartList) {
      if (cart.getFood().getStock() < cart.getQuantity()) {
        return HttpResult.failure(
            ResultCodeEnum.SERVER_ERROR, "商品 " + cart.getFood().getFoodName() + " 库存不足");
      }
    }

    EntityUtils.setNewEntity(order);
    order.setOrderTotal(totalPrice);
    order.setOrderState(OrderState.PAID);
    order.setOrderDate(order.getCreateTime());
    order.setBusiness(business);
    order.setCustomerId(currentUserId);
    order.setDeliveryAddress(address);
    order.setUsedVoucher(usedVoucher);
    order.setVoucherDiscount(voucherDiscount);
    order.setPointsUsed(pointsUsed);
    order.setPointsDiscount(pointsDiscount);
    order.setWalletPaid(walletPaid);
    order.setRequestId(requestId);
    String pointsTradeNo = null;

    if (usedVoucher != null) {
      try {
        privateVoucherService.redeemPrivateVoucher(usedVoucher.getId());
      } catch (Exception e) {
        log.error("Failed to redeem voucher: {}", e.getMessage());
        return HttpResult.failure(
            ResultCodeEnum.SERVER_ERROR, "Failed to redeem voucher: " + e.getMessage());
      }
    }

    if (walletPaid.compareTo(BigDecimal.ZERO) > 0 && userWallet != null) {
      if (!userWallet.decBalance(walletPaid)) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to deduct wallet balance");
      }
      EntityUtils.updateEntity(userWallet);
      walletRepository.save(userWallet);
    }

    if (pointsUsed > 0) {
      try {
        pointsTradeNo = "ORDER_" + UUID.randomUUID();
        pointsService.freezePoints(currentUserId, pointsUsed, pointsTradeNo);
        order.setPointsTradeNo(pointsTradeNo);
        orderService.addOrder(order);
        pointsService.deductPoints(currentUserId, pointsTradeNo, pointsTradeNo);
      } catch (PointsException e) {
        log.error("Failed to deduct points: {}", e.getMessage());
        if (walletPaid.compareTo(BigDecimal.ZERO) > 0 && userWallet != null) {
          userWallet.addBalance(walletPaid);
          EntityUtils.updateEntity(userWallet);
          walletRepository.save(userWallet);
        }
        if (usedVoucher != null) {
          try {
            privateVoucherService.restoreVoucher(usedVoucher.getId());
          } catch (Exception ex) {
            log.error("Failed to restore voucher: {}", ex.getMessage());
          }
        }
        return HttpResult.failure(
            ResultCodeEnum.SERVER_ERROR, "Failed to deduct points: " + e.getMessage());
      }
    } else {
      orderService.addOrder(order);
    }

    for (Cart cart : cartList) {
      Food food = cart.getFood();
      food.decreaseStock(cart.getQuantity());
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
        Optional<Wallet> walletOpt = walletRepository.findByOwnerId(currentUserId);
        if (walletOpt.isPresent()) {
          Wallet wallet = walletOpt.get();
          wallet.addBalance(order.getWalletPaid());
          EntityUtils.updateEntity(wallet);
          walletRepository.save(wallet);
        }
      }

      if (order.getPointsUsed() != null && order.getPointsUsed() > 0) {
        String orderBizId =
            order.getPointsTradeNo() != null ? order.getPointsTradeNo() : "ORDER_" + order.getId();
        boolean refunded =
            pointsService.refundDeductedPoints(currentUserId, orderBizId, "订单取消返还积分");
        if (!refunded) {
          pointsService.rollbackPoints(currentUserId, orderBizId, "订单取消");
        }
      }

      if (order.getUsedVoucher() != null) {
        privateVoucherService.restoreVoucher(order.getUsedVoucher().getId());
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
}

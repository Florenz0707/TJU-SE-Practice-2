package cn.edu.tju.order.service;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.order.constant.OrderState;
import cn.edu.tju.order.model.bo.Order;
import cn.edu.tju.order.model.bo.OrderDetailet;
import cn.edu.tju.order.model.vo.OrderDetailetVO;
import cn.edu.tju.order.model.vo.OrderSnapshotVO;
import cn.edu.tju.order.model.vo.PagedOrderSnapshotVO;
import cn.edu.tju.order.repository.OrderDetailetRepository;
import cn.edu.tju.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class OrderInternalService {
  private static final Logger log = LoggerFactory.getLogger(OrderInternalService.class);
  private static final String WALLET_SERVICE_CB = "walletService";
  private static final String MERCHANT_SERVICE_CB = "merchantService";
  private static final String ADDRESS_SERVICE_CB = "addressService";
  private static final String PRODUCT_SERVICE_CB = "productService";

  private final OrderRepository orderRepository;
  private final OrderDetailetRepository orderDetailetRepository;
  private final RestTemplate restTemplate;

  public OrderInternalService(
      OrderRepository orderRepository, OrderDetailetRepository orderDetailetRepository,
      RestTemplate restTemplate) {
    this.orderRepository = orderRepository;
    this.orderDetailetRepository = orderDetailetRepository;
    this.restTemplate = restTemplate;
  }

  @Transactional
  public OrderSnapshotVO createOrder(CreateOrderCommand command) {
    if (command == null || command.requestId() == null || command.requestId().isBlank()) {
      throw new IllegalArgumentException("requestId CANT BE NULL");
    }
    if (command.customerId() == null
        || command.businessId() == null
        || command.deliveryAddressId() == null) {
      throw new IllegalArgumentException("customerId/businessId/addressId CANT BE NULL");
    }
    if (command.orderTotal() == null || command.orderTotal().compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("orderTotal NOT VALID");
    }
    if (command.items() == null || command.items().isEmpty()) {
      throw new IllegalArgumentException("orderItems CANT BE EMPTY");
    }

    Order existed = orderRepository.findByRequestId(command.requestId());
    if (existed != null) {
      return new OrderSnapshotVO(existed);
    }

    // 1. 如果有 walletPaid，先从用户钱包扣款
    if (command.walletPaid() != null && command.walletPaid().compareTo(BigDecimal.ZERO) > 0) {
      try {
        debitWallet(command);
      } catch (Exception e) {
        log.error("钱包扣款失败: {}", e.getMessage(), e);
        throw new RuntimeException("钱包扣款失败: " + e.getMessage(), e);
      }
    }

    // 2. 创建订单
    Order order = new Order();
    LocalDateTime now = LocalDateTime.now();
    order.setCreateTime(now);
    order.setUpdateTime(now);
    order.setDeleted(false);
    order.setCustomerId(command.customerId());
    order.setBusinessId(command.businessId());
    order.setDeliveryAddressId(command.deliveryAddressId());
    order.setOrderDate(command.orderDate() == null ? now : command.orderDate());
    order.setOrderTotal(command.orderTotal());
    order.setOrderState(command.orderState() == null ? OrderState.PAID : command.orderState());
    order.setVoucherId(command.voucherId());
    order.setVoucherDiscount(command.voucherDiscount());
    order.setPointsUsed(command.pointsUsed());
    order.setPointsDiscount(command.pointsDiscount());
    order.setWalletPaid(command.walletPaid());
    order.setRequestId(command.requestId());
    order.setPointsTradeNo(command.pointsTradeNo());
    Order savedOrder = orderRepository.save(order);

    // 3. 核销优惠券（如果使用了优惠券）
    if (command.voucherId() != null && command.voucherDiscount() != null && command.voucherDiscount().compareTo(BigDecimal.ZERO) > 0) {
      try {
        redeemVoucher(command, savedOrder);
      } catch (Exception e) {
        log.error("优惠券核销失败: {}", e.getMessage(), e);
        // 优惠券核销失败不影响订单创建成功，继续执行
      }
    }

    List<OrderDetailet> details = new ArrayList<>(command.items().size());
    for (OrderItemCommand item : command.items()) {
      if (item.foodId() == null || item.quantity() == null || item.quantity() <= 0) {
        throw new IllegalArgumentException("orderItem NOT VALID");
      }
      OrderDetailet detail = new OrderDetailet();
      detail.setCreateTime(now);
      detail.setUpdateTime(now);
      detail.setDeleted(false);
      detail.setOrderId(savedOrder.getId());
      detail.setFoodId(item.foodId());
      detail.setQuantity(item.quantity());
      details.add(detail);
    }
    orderDetailetRepository.saveAll(details);
    return new OrderSnapshotVO(savedOrder);
  }

  @CircuitBreaker(name = WALLET_SERVICE_CB, fallbackMethod = "debitWalletFallback")
  @Retry(name = WALLET_SERVICE_CB)
  private void debitWallet(CreateOrderCommand command) {
    Map<String, Object> debitRequest = new HashMap<>();
    debitRequest.put("requestId", command.requestId());
    debitRequest.put("userId", command.customerId());
    debitRequest.put("amount", command.walletPaid());
    debitRequest.put("bizId", command.requestId());
    debitRequest.put("reason", "订单支付");

    String debitUrl = "http://wallet-service/elm/api/inner/account/wallet/debit";
    restTemplate.postForObject(debitUrl, debitRequest, Object.class);
  }

  private void debitWalletFallback(CreateOrderCommand command, Exception e) {
    log.warn("Fallback triggered for debitWallet: {}", e.getMessage());
    throw new RuntimeException("钱包服务暂时不可用，请稍后重试", e);
  }

  @CircuitBreaker(name = WALLET_SERVICE_CB, fallbackMethod = "redeemVoucherFallback")
  @Retry(name = WALLET_SERVICE_CB)
  private void redeemVoucher(CreateOrderCommand command, Order savedOrder) {
    Map<String, Object> redeemRequest = new HashMap<>();
    redeemRequest.put("requestId", command.requestId() + "_voucher");
    redeemRequest.put("userId", command.customerId());
    redeemRequest.put("voucherId", command.voucherId());
    redeemRequest.put("orderId", String.valueOf(savedOrder.getId()));

    String redeemUrl = "http://wallet-service/elm/api/inner/account/voucher/redeem";
    restTemplate.postForObject(redeemUrl, redeemRequest, Object.class);
  }

  private void redeemVoucherFallback(CreateOrderCommand command, Order savedOrder, Exception e) {
    log.warn("Fallback triggered for redeemVoucher: {}", e.getMessage());
    throw new RuntimeException("优惠券服务暂时不可用，请稍后重试", e);
  }

  @Transactional
  public OrderSnapshotVO cancelPaidOrder(Long orderId, Long operatorUserId) {
    if (orderId == null || operatorUserId == null) {
      throw new IllegalArgumentException("orderId/operatorUserId CANT BE NULL");
    }
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order NOT FOUND"));
    if (!operatorUserId.equals(order.getCustomerId())) {
      throw new IllegalStateException("AUTHORITY LACKED");
    }
    if (!Integer.valueOf(OrderState.PAID).equals(order.getOrderState())) {
      throw new IllegalStateException("OrderState NOT PAID");
    }
    
    // 回滚优惠券（如果使用了优惠券）
    if (order.getVoucherId() != null && order.getVoucherDiscount() != null && order.getVoucherDiscount().compareTo(BigDecimal.ZERO) > 0) {
      try {
        rollbackVoucher(order);
      } catch (Exception e) {
        log.error("优惠券回滚失败: {}", e.getMessage(), e);
        // 优惠券回滚失败不影响订单取消，继续执行
      }
    }
    
    order.setOrderState(OrderState.CANCELED);
    order.setUpdateTime(LocalDateTime.now());
    Order saved = orderRepository.save(order);
    return new OrderSnapshotVO(saved);
  }

  @CircuitBreaker(name = WALLET_SERVICE_CB, fallbackMethod = "rollbackVoucherFallback")
  @Retry(name = WALLET_SERVICE_CB)
  private void rollbackVoucher(Order order) {
    Map<String, Object> rollbackRequest = new HashMap<>();
    rollbackRequest.put("requestId", order.getRequestId() + "_voucher_rollback");
    rollbackRequest.put("userId", order.getCustomerId());
    rollbackRequest.put("voucherId", order.getVoucherId());
    rollbackRequest.put("orderId", String.valueOf(order.getId()));
    rollbackRequest.put("reason", "订单取消");

    String rollbackUrl = "http://wallet-service/elm/api/inner/account/voucher/rollback";
    restTemplate.postForObject(rollbackUrl, rollbackRequest, Object.class);
  }

  private void rollbackVoucherFallback(Order order, Exception e) {
    log.warn("Fallback triggered for rollbackVoucher: {}", e.getMessage());
    throw new RuntimeException("优惠券服务暂时不可用，请稍后重试", e);
  }

  private static final String POINTS_SERVICE_CB = "pointsService";

  @CircuitBreaker(name = POINTS_SERVICE_CB, fallbackMethod = "awardPointsForOrderFallback")
  @Retry(name = POINTS_SERVICE_CB)
  private void awardPointsForOrder(Order order) {
    Map<String, Object> pointsRequest = new HashMap<>();
    pointsRequest.put("userId", order.getCustomerId());
    pointsRequest.put("bizId", String.valueOf(order.getId()));
    pointsRequest.put("amount", order.getOrderTotal() != null ? order.getOrderTotal().doubleValue() : 0.0);
    pointsRequest.put("eventTime", LocalDateTime.now().toString());
    pointsRequest.put("extraInfo", "");

    String pointsUrl = "http://points-service/elm/api/inner/points/notify/order-success";
    restTemplate.postForObject(pointsUrl, pointsRequest, Object.class);
  }

  private void awardPointsForOrderFallback(Order order, Exception e) {
    log.warn("Fallback triggered for awardPointsForOrder: {}", e.getMessage());
    throw new RuntimeException("积分服务暂时不可用，请稍后重试", e);
  }

  @Transactional
  public OrderSnapshotVO updateOrderState(Long orderId, Integer targetState) {
    if (orderId == null || targetState == null) {
      throw new IllegalArgumentException("orderId/orderState CANT BE NULL");
    }
    if (!isValidOrderState(targetState)) {
      throw new IllegalArgumentException("OrderState NOT VALID");
    }
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order NOT FOUND"));
    if (!isValidStateTransition(order.getOrderState(), targetState)) {
      throw new IllegalStateException("非法的状态转换");
    }
    
    Integer oldState = order.getOrderState();
    
    // 状态转换业务逻辑
    if (oldState.equals(OrderState.PAID)) {
      if (targetState.equals(OrderState.CANCELED)) {
        // 商家拒单或用户取消：给用户退款
        if (order.getWalletPaid() != null && order.getWalletPaid().compareTo(BigDecimal.ZERO) > 0) {
          try {
            refundWallet(order);
          } catch (Exception e) {
            log.error("钱包退款失败: {}", e.getMessage(), e);
            throw new RuntimeException("钱包退款失败: " + e.getMessage(), e);
          }
        }
      }
    }
    
    // 订单完成时：给商家钱包入账 + 给用户发放积分
    if (!oldState.equals(OrderState.COMPLETE) && targetState.equals(OrderState.COMPLETE)) {
      if (order.getWalletPaid() != null && order.getWalletPaid().compareTo(BigDecimal.ZERO) > 0) {
        try {
          // 先通过 businessId 查询店铺信息，获取 ownerId
          Long ownerId = getBusinessOwnerId(order.getBusinessId());
          if (ownerId == null) {
            throw new RuntimeException("无法获取店铺 ownerId");
          }
          creditWallet(order, ownerId);
        } catch (Exception e) {
          log.error("商家钱包入账失败: {}", e.getMessage(), e);
          throw new RuntimeException("商家钱包入账失败: " + e.getMessage(), e);
        }
      }
      
      // 给用户发放订单完成积分
      try {
        awardPointsForOrder(order);
      } catch (Exception e) {
        log.error("订单积分发放失败: {}", e.getMessage(), e);
        // 积分发放失败不影响订单完成，继续执行
      }
    }
    
    order.setOrderState(targetState);
    order.setUpdateTime(LocalDateTime.now());
    Order saved = orderRepository.save(order);
    return new OrderSnapshotVO(saved);
  }

  @CircuitBreaker(name = WALLET_SERVICE_CB, fallbackMethod = "refundWalletFallback")
  @Retry(name = WALLET_SERVICE_CB)
  private void refundWallet(Order order) {
    Map<String, Object> refundRequest = new HashMap<>();
    refundRequest.put("requestId", order.getRequestId() + "_refund");
    refundRequest.put("userId", order.getCustomerId());
    refundRequest.put("amount", order.getWalletPaid());
    refundRequest.put("bizId", String.valueOf(order.getId()));
    refundRequest.put("reason", "订单取消退款");

    String refundUrl = "http://wallet-service/elm/api/inner/account/wallet/refund";
    restTemplate.postForObject(refundUrl, refundRequest, Object.class);
  }

  private void refundWalletFallback(Order order, Exception e) {
    log.warn("Fallback triggered for refundWallet: {}", e.getMessage());
    throw new RuntimeException("钱包退款服务暂时不可用，请稍后重试", e);
  }

  @CircuitBreaker(name = MERCHANT_SERVICE_CB, fallbackMethod = "getBusinessOwnerIdFallback")
  private Long getBusinessOwnerId(Long businessId) {
    String businessUrl = "http://merchant-service/elm/api/businesses/" + businessId;
    ResponseEntity<HttpResult<Object>> businessResp = restTemplate.exchange(
            businessUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<HttpResult<Object>>() {}
    );
    HttpResult<Object> businessBody = businessResp.getBody();
    if (businessBody != null && Boolean.TRUE.equals(businessBody.getSuccess()) && businessBody.getData() instanceof Map<?, ?> map) {
      Object ownerIdObj = map.get("businessOwnerId");
      if (ownerIdObj != null) {
        return Long.parseLong(ownerIdObj.toString());
      }
    }
    return null;
  }

  private Long getBusinessOwnerIdFallback(Long businessId, Exception e) {
    log.warn("Fallback triggered for getBusinessOwnerId: {}", e.getMessage());
    throw new RuntimeException("获取店铺信息失败: " + e.getMessage(), e);
  }

  @CircuitBreaker(name = WALLET_SERVICE_CB, fallbackMethod = "creditWalletFallback")
  @Retry(name = WALLET_SERVICE_CB)
  private void creditWallet(Order order, Long ownerId) {
    Map<String, Object> creditRequest = new HashMap<>();
    creditRequest.put("requestId", order.getRequestId() + "_merchant");
    creditRequest.put("userId", ownerId);
    creditRequest.put("amount", order.getWalletPaid());
    creditRequest.put("bizId", String.valueOf(order.getId()));
    creditRequest.put("reason", "订单收入");

    String creditUrl = "http://wallet-service/elm/api/inner/account/wallet/credit";
    restTemplate.postForObject(creditUrl, creditRequest, Object.class);
  }

  private void creditWalletFallback(Order order, Long ownerId, Exception e) {
    log.warn("Fallback triggered for creditWallet: {}", e.getMessage());
    throw new RuntimeException("商家钱包入账服务暂时不可用，请稍后重试", e);
  }

  @Transactional(readOnly = true)
  public OrderSnapshotVO getOrderById(Long orderId) {
    if (orderId == null) {
      return null;
    }
    return orderRepository.findById(orderId).map(OrderSnapshotVO::new).orElse(null);
  }

  @Transactional(readOnly = true)
  public OrderSnapshotVO getOrderByRequestId(String requestId) {
    if (requestId == null || requestId.isEmpty()) {
      return null;
    }
    var order = orderRepository.findByRequestId(requestId);
    return order == null ? null : new OrderSnapshotVO(order);
  }

  @Transactional(readOnly = true)
  public List<OrderSnapshotVO> getOrdersByCustomerId(Long customerId) {
    if (customerId == null) {
      return List.of();
    }
    return orderRepository.findAllByCustomerId(customerId).stream()
        .map(OrderSnapshotVO::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<OrderSnapshotVO> getOrdersByBusinessId(Long businessId) {
    if (businessId == null) {
      return List.of();
    }
    return orderRepository.findAllByBusinessId(businessId).stream()
        .map(OrderSnapshotVO::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<OrderSnapshotVO> getOrdersByBusinessIds(List<Long> businessIds) {
    if (businessIds == null || businessIds.isEmpty()) {
      return List.of();
    }
    return orderRepository.findAllByBusinessIdIn(businessIds).stream()
        .map(OrderSnapshotVO::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public PagedOrderSnapshotVO getOrdersByCustomerId(Long customerId, int page, int size) {
    if (customerId == null) {
      return new PagedOrderSnapshotVO(List.of(), 0L, page, size);
    }
    int safePage = Math.max(page, 1);
    int safeSize = Math.max(size, 1);
    Page<Order> orderPage =
        orderRepository.findAllByCustomerId(customerId, PageRequest.of(safePage - 1, safeSize));
    return new PagedOrderSnapshotVO(
        orderPage.getContent().stream().map(OrderSnapshotVO::new).toList(),
        orderPage.getTotalElements(),
        safePage,
        safeSize);
  }

  @Transactional(readOnly = true)
  public PagedOrderSnapshotVO getOrdersByBusinessId(Long businessId, int page, int size) {
    if (businessId == null) {
      return new PagedOrderSnapshotVO(List.of(), 0L, page, size);
    }
    int safePage = Math.max(page, 1);
    int safeSize = Math.max(size, 1);
    Page<Order> orderPage =
        orderRepository.findAllByBusinessId(businessId, PageRequest.of(safePage - 1, safeSize));
    return new PagedOrderSnapshotVO(
        orderPage.getContent().stream().map(OrderSnapshotVO::new).toList(),
        orderPage.getTotalElements(),
        safePage,
        safeSize);
  }

  @Transactional(readOnly = true)
  public PagedOrderSnapshotVO getOrdersByBusinessIds(List<Long> businessIds, int page, int size) {
    if (businessIds == null || businessIds.isEmpty()) {
      return new PagedOrderSnapshotVO(List.of(), 0L, page, size);
    }
    int safePage = Math.max(page, 1);
    int safeSize = Math.max(size, 1);
    Page<Order> orderPage =
        orderRepository.findAllByBusinessIdIn(businessIds, PageRequest.of(safePage - 1, safeSize));
    return new PagedOrderSnapshotVO(
        orderPage.getContent().stream().map(OrderSnapshotVO::new).toList(),
        orderPage.getTotalElements(),
        safePage,
        safeSize);
  }

  @Transactional(readOnly = true)
  public List<OrderDetailetVO> getOrderDetailetsByOrderId(Long orderId) {
    if (orderId == null) {
      return List.of();
    }
    return orderDetailetRepository.findAllByOrderId(orderId).stream()
        .map(OrderDetailetVO::new)
        .toList();
  }

  public record OrderItemCommand(Long foodId, Integer quantity) {}

  public record CreateOrderCommand(
      String requestId,
      Long customerId,
      Long businessId,
      Long deliveryAddressId,
      BigDecimal orderTotal,
      Integer orderState,
      Long voucherId,
      BigDecimal voucherDiscount,
      Integer pointsUsed,
      BigDecimal pointsDiscount,
      BigDecimal walletPaid,
      String pointsTradeNo,
      LocalDateTime orderDate,
      List<OrderItemCommand> items) {}

  private boolean isValidOrderState(Integer orderState) {
    return orderState >= OrderState.CANCELED && orderState <= OrderState.COMMENTED;
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

  private void enrichOrderSnapshot(OrderSnapshotVO vo) {
    Map<Long, Map<String, Object>> foodCache = new HashMap<>();
    Map<Long, Map<String, Object>> businessCache = new HashMap<>();
    Map<Long, Map<String, Object>> addressCache = new HashMap<>();
    Map<Long, Map<String, Object>> customerCache = new HashMap<>();

    try {
      if (vo.getCustomerId() != null) {
        Map<String, Object> cachedCustomer = customerCache.get(vo.getCustomerId());
        if (cachedCustomer != null) {
          vo.setCustomer(cachedCustomer);
        } else {
          Map<String, Object> customerView = new HashMap<>();
          customerView.put("id", vo.getCustomerId());
          customerView.put("username", "用户" + vo.getCustomerId());
          customerCache.put(vo.getCustomerId(), customerView);
          vo.setCustomer(customerView);
        }
      }

      if (vo.getBusinessId() != null) {
        Map<String, Object> cachedBusiness = businessCache.get(vo.getBusinessId());
        if (cachedBusiness != null) {
          vo.setBusiness(cachedBusiness);
        } else {
          try {
            getBusinessFromService(vo, businessCache);
          } catch (Exception ignored) {}
        }
      }

      if (vo.getDeliveryAddressId() != null) {
        Map<String, Object> cachedAddress = addressCache.get(vo.getDeliveryAddressId());
        if (cachedAddress != null) {
          vo.setDeliveryAddress(cachedAddress);
        } else {
          try {
            getAddressFromService(vo, addressCache);
          } catch (Exception ignored) {}
        }
      }

      if (vo.getOrderDetails() != null) {
        for (OrderDetailetVO detail : vo.getOrderDetails()) {
          if (detail.getFoodId() != null) {
            Map<String, Object> cachedFood = foodCache.get(detail.getFoodId());
            if (cachedFood != null) {
              detail.setFood(cachedFood);
            } else {
              try {
                getFoodFromService(detail, foodCache);
              } catch (Exception ignored) {}
            }
          }
        }
      }
    } catch (Exception ignored) {}
  }

  @CircuitBreaker(name = MERCHANT_SERVICE_CB, fallbackMethod = "getBusinessFromServiceFallback")
  private void getBusinessFromService(OrderSnapshotVO vo, Map<Long, Map<String, Object>> businessCache) {
    String businessUrl = "http://merchant-service/elm/api/businesses/" + vo.getBusinessId();
    ResponseEntity<HttpResult<Object>> businessResp = restTemplate.exchange(
            businessUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<HttpResult<Object>>() {}
    );
    HttpResult<Object> businessBody = businessResp.getBody();
    if (businessBody != null && Boolean.TRUE.equals(businessBody.getSuccess()) && businessBody.getData() instanceof Map<?, ?> map) {
      Map<String, Object> businessView = new HashMap<>();
      businessView.put("id", vo.getBusinessId());
      businessView.put("businessName", map.get("businessName"));
      businessView.put("businessAddress", map.get("businessAddress"));
      businessView.put("businessExplain", map.get("businessExplain"));
      businessView.put("businessImg", map.get("businessImg"));
      businessView.put("deliveryPrice", map.get("deliveryPrice"));
      businessView.put("startPrice", map.get("startPrice"));
      businessCache.put(vo.getBusinessId(), businessView);
      vo.setBusiness(businessView);
    }
  }

  private void getBusinessFromServiceFallback(OrderSnapshotVO vo, Map<Long, Map<String, Object>> businessCache, Exception e) {
    log.warn("Fallback triggered for getBusinessFromService: {}", e.getMessage());
  }

  @CircuitBreaker(name = ADDRESS_SERVICE_CB, fallbackMethod = "getAddressFromServiceFallback")
  private void getAddressFromService(OrderSnapshotVO vo, Map<Long, Map<String, Object>> addressCache) {
    String addressUrl = "http://address-service/elm/api/addresses/" + vo.getDeliveryAddressId();
    ResponseEntity<HttpResult<Object>> addressResp = restTemplate.exchange(
            addressUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<HttpResult<Object>>() {}
    );
    HttpResult<Object> addressBody = addressResp.getBody();
    if (addressBody != null && Boolean.TRUE.equals(addressBody.getSuccess()) && addressBody.getData() instanceof Map<?, ?> map) {
      Map<String, Object> addressView = new HashMap<>();
      addressView.put("id", vo.getDeliveryAddressId());
      addressView.put("contactName", map.get("contactName"));
      addressView.put("contactSex", map.get("contactSex"));
      addressView.put("contactTel", map.get("contactTel"));
      addressView.put("address", map.get("address"));
      addressCache.put(vo.getDeliveryAddressId(), addressView);
      vo.setDeliveryAddress(addressView);
    }
  }

  private void getAddressFromServiceFallback(OrderSnapshotVO vo, Map<Long, Map<String, Object>> addressCache, Exception e) {
    log.warn("Fallback triggered for getAddressFromService: {}", e.getMessage());
  }

  @CircuitBreaker(name = PRODUCT_SERVICE_CB, fallbackMethod = "getFoodFromServiceFallback")
  private void getFoodFromService(OrderDetailetVO detail, Map<Long, Map<String, Object>> foodCache) {
    String foodUrl = "http://product-service/elm/api/foods/" + detail.getFoodId();
    ResponseEntity<HttpResult<Object>> foodResp = restTemplate.exchange(
            foodUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<HttpResult<Object>>() {}
    );
    HttpResult<Object> foodBody = foodResp.getBody();
    if (foodBody != null && Boolean.TRUE.equals(foodBody.getSuccess()) && foodBody.getData() instanceof Map<?, ?> map) {
      Map<String, Object> foodView = new HashMap<>();
      foodView.put("id", detail.getFoodId());
      foodView.put("foodName", map.get("foodName"));
      foodView.put("foodPrice", map.get("foodPrice"));
      foodView.put("foodImg", map.get("foodImg"));
      foodView.put("foodExplain", map.get("foodExplain"));
      foodCache.put(detail.getFoodId(), foodView);
      detail.setFood(foodView);
    }
  }

  private void getFoodFromServiceFallback(OrderDetailetVO detail, Map<Long, Map<String, Object>> foodCache, Exception e) {
    log.warn("Fallback triggered for getFoodFromService: {}", e.getMessage());
  }

  @Transactional(readOnly = true)
  public OrderSnapshotVO getOrderByIdWithDetails(Long orderId) {
    OrderSnapshotVO vo = getOrderById(orderId);
    if (vo == null) return null;
    
    List<OrderDetailetVO> details = getOrderDetailetsByOrderId(orderId);
    vo.setOrderDetails(details);
    
    enrichOrderSnapshot(vo);
    return vo;
  }

  @Transactional(readOnly = true)
  public List<OrderSnapshotVO> getOrdersByCustomerIdWithDetails(Long customerId) {
    List<OrderSnapshotVO> orders = getOrdersByCustomerId(customerId);
    for (OrderSnapshotVO vo : orders) {
      List<OrderDetailetVO> details = getOrderDetailetsByOrderId(vo.getId());
      vo.setOrderDetails(details);
      enrichOrderSnapshot(vo);
    }
    return orders;
  }

  @Transactional(readOnly = true)
  public List<OrderSnapshotVO> getOrdersByBusinessIdWithDetails(Long businessId) {
    List<OrderSnapshotVO> orders = getOrdersByBusinessId(businessId);
    for (OrderSnapshotVO vo : orders) {
      List<OrderDetailetVO> details = getOrderDetailetsByOrderId(vo.getId());
      vo.setOrderDetails(details);
      enrichOrderSnapshot(vo);
    }
    return orders;
  }

  @Transactional(readOnly = true)
  public List<OrderSnapshotVO> getOrdersByBusinessIdsWithDetails(List<Long> businessIds) {
    List<OrderSnapshotVO> orders = getOrdersByBusinessIds(businessIds);
    for (OrderSnapshotVO vo : orders) {
      List<OrderDetailetVO> details = getOrderDetailetsByOrderId(vo.getId());
      vo.setOrderDetails(details);
      enrichOrderSnapshot(vo);
    }
    return orders;
  }
}

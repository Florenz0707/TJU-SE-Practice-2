package cn.edu.tju.order.service;

import cn.edu.tju.order.constant.OrderState;
import cn.edu.tju.order.model.bo.Order;
import cn.edu.tju.order.model.bo.OrderDetailet;
import cn.edu.tju.order.model.vo.OrderDetailetVO;
import cn.edu.tju.order.model.vo.OrderSnapshotVO;
import cn.edu.tju.order.repository.OrderDetailetRepository;
import cn.edu.tju.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderInternalService {
  private final OrderRepository orderRepository;
  private final OrderDetailetRepository orderDetailetRepository;

  public OrderInternalService(
      OrderRepository orderRepository, OrderDetailetRepository orderDetailetRepository) {
    this.orderRepository = orderRepository;
    this.orderDetailetRepository = orderDetailetRepository;
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
    order.setOrderState(OrderState.CANCELED);
    order.setUpdateTime(LocalDateTime.now());
    Order saved = orderRepository.save(order);
    return new OrderSnapshotVO(saved);
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
    order.setOrderState(targetState);
    order.setUpdateTime(LocalDateTime.now());
    Order saved = orderRepository.save(order);
    return new OrderSnapshotVO(saved);
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
}

package cn.edu.tju.order.model.vo;

import cn.edu.tju.order.model.bo.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderSnapshotVO {
  private final Long id;
  private final Long customerId;
  private final Long businessId;
  private final Long deliveryAddressId;
  private final Integer orderState;
  private final BigDecimal orderTotal;
  private final String requestId;
  private final LocalDateTime orderDate;

  public OrderSnapshotVO(Order order) {
    this.id = order.getId();
    this.customerId = order.getCustomerId();
    this.businessId = order.getBusinessId();
    this.deliveryAddressId = order.getDeliveryAddressId();
    this.orderState = order.getOrderState();
    this.orderTotal = order.getOrderTotal();
    this.requestId = order.getRequestId();
    this.orderDate = order.getOrderDate();
  }

  public Long getId() {
    return id;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public Long getBusinessId() {
    return businessId;
  }

  public Long getDeliveryAddressId() {
    return deliveryAddressId;
  }

  public Integer getOrderState() {
    return orderState;
  }

  public BigDecimal getOrderTotal() {
    return orderTotal;
  }

  public String getRequestId() {
    return requestId;
  }

  public LocalDateTime getOrderDate() {
    return orderDate;
  }
}

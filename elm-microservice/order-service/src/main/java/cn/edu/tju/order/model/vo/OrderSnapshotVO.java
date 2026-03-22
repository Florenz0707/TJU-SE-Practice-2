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
  private final Long voucherId;
  private final BigDecimal voucherDiscount;
  private final Integer pointsUsed;
  private final BigDecimal pointsDiscount;
  private final BigDecimal walletPaid;
  private final String pointsTradeNo;
  private final String requestId;
  private final LocalDateTime orderDate;

  public OrderSnapshotVO(Order order) {
    this.id = order.getId();
    this.customerId = order.getCustomerId();
    this.businessId = order.getBusinessId();
    this.deliveryAddressId = order.getDeliveryAddressId();
    this.orderState = order.getOrderState();
    this.orderTotal = order.getOrderTotal();
    this.voucherId = order.getVoucherId();
    this.voucherDiscount = order.getVoucherDiscount();
    this.pointsUsed = order.getPointsUsed();
    this.pointsDiscount = order.getPointsDiscount();
    this.walletPaid = order.getWalletPaid();
    this.pointsTradeNo = order.getPointsTradeNo();
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

  public Long getVoucherId() {
    return voucherId;
  }

  public BigDecimal getVoucherDiscount() {
    return voucherDiscount;
  }

  public Integer getPointsUsed() {
    return pointsUsed;
  }

  public BigDecimal getPointsDiscount() {
    return pointsDiscount;
  }

  public BigDecimal getWalletPaid() {
    return walletPaid;
  }

  public String getPointsTradeNo() {
    return pointsTradeNo;
  }

  public String getRequestId() {
    return requestId;
  }

  public LocalDateTime getOrderDate() {
    return orderDate;
  }
}

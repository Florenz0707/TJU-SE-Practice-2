package cn.edu.tju.order.model.vo;

import cn.edu.tju.order.model.bo.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
  private final LocalDateTime createTime;
  private final LocalDateTime updateTime;
  
  private Map<String, Object> customer;
  private Map<String, Object> business;
  private Map<String, Object> deliveryAddress;
  private List<OrderDetailetVO> orderDetails;

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
    this.createTime = order.getCreateTime();
    this.updateTime = order.getUpdateTime();
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

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public LocalDateTime getUpdateTime() {
    return updateTime;
  }

  public Map<String, Object> getCustomer() {
    return customer;
  }

  public void setCustomer(Map<String, Object> customer) {
    this.customer = customer;
  }

  public Map<String, Object> getBusiness() {
    return business;
  }

  public void setBusiness(Map<String, Object> business) {
    this.business = business;
  }

  public Map<String, Object> getDeliveryAddress() {
    return deliveryAddress;
  }

  public void setDeliveryAddress(Map<String, Object> deliveryAddress) {
    this.deliveryAddress = deliveryAddress;
  }

  public List<OrderDetailetVO> getOrderDetails() {
    return orderDetails;
  }

  public void setOrderDetails(List<OrderDetailetVO> orderDetails) {
    this.orderDetails = orderDetails;
  }
}

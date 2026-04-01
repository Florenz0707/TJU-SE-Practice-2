package cn.edu.tju.order.model.bo;

import cn.edu.tju.core.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

  @Column(name = "customer_id", nullable = false)
  private Long customerId;

  @Column(name = "business_id", nullable = false)
  private Long businessId;

  private LocalDateTime orderDate;

  @Column(precision = 10, scale = 2, nullable = false)
  private BigDecimal orderTotal;

  @Column(name = "address_id", nullable = false)
  private Long deliveryAddressId;

  private Integer orderState;

  @Column(name = "voucher_id")
  private Long voucherId;

  @Column(precision = 10, scale = 2)
  private BigDecimal voucherDiscount;

  @Column(precision = 10, scale = 0)
  private Integer pointsUsed;

  @Column(precision = 10, scale = 2)
  private BigDecimal pointsDiscount;

  @Column(precision = 10, scale = 2)
  private BigDecimal walletPaid;

  @Column(unique = true)
  private String requestId;

  @Column(name = "points_trade_no", length = 100)
  private String pointsTradeNo;

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

  public LocalDateTime getOrderDate() {
    return orderDate;
  }

  public void setOrderDate(LocalDateTime orderDate) {
    this.orderDate = orderDate;
  }

  public BigDecimal getOrderTotal() {
    return orderTotal;
  }

  public void setOrderTotal(BigDecimal orderTotal) {
    this.orderTotal = orderTotal;
  }

  public Long getDeliveryAddressId() {
    return deliveryAddressId;
  }

  public void setDeliveryAddressId(Long deliveryAddressId) {
    this.deliveryAddressId = deliveryAddressId;
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

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getPointsTradeNo() {
    return pointsTradeNo;
  }

  public void setPointsTradeNo(String pointsTradeNo) {
    this.pointsTradeNo = pointsTradeNo;
  }
}

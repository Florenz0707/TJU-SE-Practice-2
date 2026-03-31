package cn.edu.tju.business.model.bo;

import cn.edu.tju.core.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
public class Business extends BaseEntity {

  @Column(nullable = false)
  private String businessName;

  @Column(name = "user_id", nullable = false)
  private Long businessOwnerId;

  private String businessAddress;
  private String businessExplain;

  @Column(columnDefinition = "TEXT")
  private String businessImg;

  private Integer orderTypeId;

  @Column(precision = 10, scale = 2)
  private BigDecimal startPrice;

  @Column(precision = 10, scale = 2)
  private BigDecimal deliveryPrice;

  private String remarks;
  private LocalTime openTime;
  private LocalTime closeTime;

  public String getBusinessName() { return businessName; }
  public void setBusinessName(String businessName) { this.businessName = businessName; }
  public Long getBusinessOwnerId() { return businessOwnerId; }
  public void setBusinessOwnerId(Long businessOwnerId) { this.businessOwnerId = businessOwnerId; }
  public String getBusinessAddress() { return businessAddress; }
  public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }
  public String getBusinessExplain() { return businessExplain; }
  public void setBusinessExplain(String businessExplain) { this.businessExplain = businessExplain; }
  public String getBusinessImg() { return businessImg; }
  public void setBusinessImg(String businessImg) { this.businessImg = businessImg; }
  public Integer getOrderTypeId() { return orderTypeId; }
  public void setOrderTypeId(Integer orderTypeId) { this.orderTypeId = orderTypeId; }
  public BigDecimal getStartPrice() { return startPrice; }
  public void setStartPrice(BigDecimal startPrice) { this.startPrice = startPrice; }
  public BigDecimal getDeliveryPrice() { return deliveryPrice; }
  public void setDeliveryPrice(BigDecimal deliveryPrice) { this.deliveryPrice = deliveryPrice; }
  public String getRemarks() { return remarks; }
  public void setRemarks(String remarks) { this.remarks = remarks; }
  public LocalTime getOpenTime() { return openTime; }
  public void setOpenTime(LocalTime openTime) { this.openTime = openTime; }
  public LocalTime getCloseTime() { return closeTime; }
  public void setCloseTime(LocalTime closeTime) { this.closeTime = closeTime; }
}

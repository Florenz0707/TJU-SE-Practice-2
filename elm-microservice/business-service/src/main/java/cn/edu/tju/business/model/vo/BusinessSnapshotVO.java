package cn.edu.tju.business.model.vo;

import cn.edu.tju.business.model.bo.Business;
import java.math.BigDecimal;
import java.time.LocalTime;

public class BusinessSnapshotVO {
  private final Long id;
  private final String businessName;
  private final Long businessOwnerId;
  private final String businessAddress;
  private final String businessExplain;
  private final String businessImg;
  private final Integer orderTypeId;
  private final Boolean deleted;
  private final BigDecimal startPrice;
  private final BigDecimal deliveryPrice;
  private final String remarks;
  private final LocalTime openTime;
  private final LocalTime closeTime;

  public BusinessSnapshotVO(Business business) {
    this.id = business.getId();
    this.businessName = business.getBusinessName();
    this.businessOwnerId = business.getBusinessOwnerId();
    this.businessAddress = business.getBusinessAddress();
    this.businessExplain = business.getBusinessExplain();
    this.businessImg = business.getBusinessImg();
    this.orderTypeId = business.getOrderTypeId();
    this.deleted = business.getDeleted();
    this.startPrice = business.getStartPrice();
    this.deliveryPrice = business.getDeliveryPrice();
    this.remarks = business.getRemarks();
    this.openTime = business.getOpenTime();
    this.closeTime = business.getCloseTime();
  }

  public Long getId() { return id; }
  public String getBusinessName() { return businessName; }
  public Long getBusinessOwnerId() { return businessOwnerId; }
  public String getBusinessAddress() { return businessAddress; }
  public String getBusinessExplain() { return businessExplain; }
  public String getBusinessImg() { return businessImg; }
  public Integer getOrderTypeId() { return orderTypeId; }
  public Boolean getDeleted() { return deleted; }
  public BigDecimal getStartPrice() { return startPrice; }
  public BigDecimal getDeliveryPrice() { return deliveryPrice; }
  public String getRemarks() { return remarks; }
  public LocalTime getOpenTime() { return openTime; }
  public LocalTime getCloseTime() { return closeTime; }
}

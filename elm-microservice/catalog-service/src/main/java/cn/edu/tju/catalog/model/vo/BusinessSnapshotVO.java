package cn.edu.tju.catalog.model.vo;

import cn.edu.tju.catalog.model.bo.Business;
import java.math.BigDecimal;
import java.time.LocalTime;

public class BusinessSnapshotVO {
  private final Long id;
  private final Boolean deleted;
  private final BigDecimal startPrice;
  private final BigDecimal deliveryPrice;
  private final LocalTime openTime;
  private final LocalTime closeTime;

  public BusinessSnapshotVO(Business business) {
    this.id = business.getId();
    this.deleted = business.getDeleted();
    this.startPrice = business.getStartPrice();
    this.deliveryPrice = business.getDeliveryPrice();
    this.openTime = business.getOpenTime();
    this.closeTime = business.getCloseTime();
  }

  public Long getId() {
    return id;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public BigDecimal getStartPrice() {
    return startPrice;
  }

  public BigDecimal getDeliveryPrice() {
    return deliveryPrice;
  }

  public LocalTime getOpenTime() {
    return openTime;
  }

  public LocalTime getCloseTime() {
    return closeTime;
  }
}

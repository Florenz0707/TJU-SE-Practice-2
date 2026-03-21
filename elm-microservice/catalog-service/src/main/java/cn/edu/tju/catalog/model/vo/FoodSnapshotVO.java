package cn.edu.tju.catalog.model.vo;

import cn.edu.tju.catalog.model.bo.Food;
import java.math.BigDecimal;

public class FoodSnapshotVO {
  private final Long id;
  private final Long businessId;
  private final Boolean deleted;
  private final BigDecimal foodPrice;
  private final Integer stock;

  public FoodSnapshotVO(Food food) {
    this.id = food.getId();
    this.businessId = food.getBusiness() != null ? food.getBusiness().getId() : null;
    this.deleted = food.getDeleted();
    this.foodPrice = food.getFoodPrice();
    this.stock = food.getStock();
  }

  public Long getId() {
    return id;
  }

  public Long getBusinessId() {
    return businessId;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public BigDecimal getFoodPrice() {
    return foodPrice;
  }

  public Integer getStock() {
    return stock;
  }
}

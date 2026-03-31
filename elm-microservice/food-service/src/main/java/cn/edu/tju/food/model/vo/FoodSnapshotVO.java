package cn.edu.tju.food.model.vo;

import cn.edu.tju.food.model.bo.Food;
import java.math.BigDecimal;

public class FoodSnapshotVO {
  private final Long id;
  private final String foodName;
  private final String foodExplain;
  private final String foodImg;
  private final Long businessId;
  private final Boolean deleted;
  private final BigDecimal foodPrice;
  private final Integer stock;
  private final String remarks;

  public FoodSnapshotVO(Food food) {
    this.id = food.getId();
    this.foodName = food.getFoodName();
    this.foodExplain = food.getFoodExplain();
    this.foodImg = food.getFoodImg();
    this.businessId = food.getBusinessId();
    this.deleted = food.getDeleted();
    this.foodPrice = food.getFoodPrice();
    this.stock = food.getStock();
    this.remarks = food.getRemarks();
  }

  public Long getId() { return id; }
  public String getFoodName() { return foodName; }
  public String getFoodExplain() { return foodExplain; }
  public String getFoodImg() { return foodImg; }
  public Long getBusinessId() { return businessId; }
  public Boolean getDeleted() { return deleted; }
  public BigDecimal getFoodPrice() { return foodPrice; }
  public Integer getStock() { return stock; }
  public String getRemarks() { return remarks; }
}

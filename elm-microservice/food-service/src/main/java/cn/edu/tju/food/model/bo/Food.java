package cn.edu.tju.food.model.bo;

import cn.edu.tju.core.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.math.BigDecimal;

@Entity
public class Food extends BaseEntity {

  @Column(nullable = false)
  private String foodName;

  private String foodExplain;

  @Column(columnDefinition = "TEXT")
  private String foodImg;

  @Column(precision = 10, scale = 2, nullable = false)
  private BigDecimal foodPrice;

  @Column(name = "business_id", nullable = false)
  private Long businessId;

  @Column(nullable = false)
  private Integer stock = 0;

  private String remarks;

  public String getFoodName() { return foodName; }
  public void setFoodName(String foodName) { this.foodName = foodName; }
  public String getFoodExplain() { return foodExplain; }
  public void setFoodExplain(String foodExplain) { this.foodExplain = foodExplain; }
  public String getFoodImg() { return foodImg; }
  public void setFoodImg(String foodImg) { this.foodImg = foodImg; }
  public BigDecimal getFoodPrice() { return foodPrice; }
  public void setFoodPrice(BigDecimal foodPrice) { this.foodPrice = foodPrice; }
  public Long getBusinessId() { return businessId; }
  public void setBusinessId(Long businessId) { this.businessId = businessId; }
  public Integer getStock() { return stock; }
  public void setStock(Integer stock) { this.stock = stock; }
  public String getRemarks() { return remarks; }
  public void setRemarks(String remarks) { this.remarks = remarks; }
}

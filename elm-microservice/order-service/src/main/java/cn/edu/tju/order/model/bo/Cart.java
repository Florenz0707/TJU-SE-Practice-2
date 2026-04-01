package cn.edu.tju.order.model.bo;

import cn.edu.tju.core.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cart")
public class Cart extends BaseEntity {

  @Column(name = "food_id", nullable = false)
  private Long foodId;

  @Column(name = "customer_id", nullable = false)
  private Long customerId;

  @Column(name = "business_id", nullable = false)
  private Long businessId;

  private Integer quantity;

  public Long getFoodId() {
    return foodId;
  }

  public void setFoodId(Long foodId) {
    this.foodId = foodId;
  }

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

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }
}

package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.elm.model.VO.UserSummaryView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

@Entity
public class Cart extends BaseEntity {
  @ManyToOne
  @JoinColumn(name = "food_id", nullable = false)
  private Food food;

  @Column(name = "customer_id", nullable = false)
  private Long customerId;

  @Transient private UserSummaryView customer;

  @ManyToOne
  @JoinColumn(name = "business_id", nullable = false)
  private Business business;

  private Integer quantity;

  public Food getFood() {
    return food;
  }

  public void setFood(Food food) {
    this.food = food;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }

  public UserSummaryView getCustomer() {
    return customer;
  }

  public void setCustomer(UserSummaryView customer) {
    this.customer = customer;
  }

  public Business getBusiness() {
    return business;
  }

  public void setBusiness(Business business) {
    this.business = business;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }
}

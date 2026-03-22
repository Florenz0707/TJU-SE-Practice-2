package cn.edu.tju.order.model.vo;

import cn.edu.tju.order.model.bo.Cart;

public class CartSnapshotVO {
  private final Long id;
  private final Long foodId;
  private final Long customerId;
  private final Long businessId;
  private final Integer quantity;

  public CartSnapshotVO(Cart cart) {
    this.id = cart.getId();
    this.foodId = cart.getFoodId();
    this.customerId = cart.getCustomerId();
    this.businessId = cart.getBusinessId();
    this.quantity = cart.getQuantity();
  }

  public Long getId() {
    return id;
  }

  public Long getFoodId() {
    return foodId;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public Long getBusinessId() {
    return businessId;
  }

  public Integer getQuantity() {
    return quantity;
  }
}

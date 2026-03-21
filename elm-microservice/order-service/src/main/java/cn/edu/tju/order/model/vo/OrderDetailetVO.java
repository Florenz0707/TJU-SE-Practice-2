package cn.edu.tju.order.model.vo;

import cn.edu.tju.order.model.bo.OrderDetailet;

public class OrderDetailetVO {
  private final Long id;
  private final Long orderId;
  private final Long foodId;
  private final Integer quantity;

  public OrderDetailetVO(OrderDetailet detailet) {
    this.id = detailet.getId();
    this.orderId = detailet.getOrderId();
    this.foodId = detailet.getFoodId();
    this.quantity = detailet.getQuantity();
  }

  public Long getId() {
    return id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public Long getFoodId() {
    return foodId;
  }

  public Integer getQuantity() {
    return quantity;
  }
}

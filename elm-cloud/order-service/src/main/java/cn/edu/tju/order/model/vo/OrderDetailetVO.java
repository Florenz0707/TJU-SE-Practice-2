package cn.edu.tju.order.model.vo;

import cn.edu.tju.order.model.bo.OrderDetailet;
import java.util.Map;

public class OrderDetailetVO {
  private final Long id;
  private final Long orderId;
  private final Long foodId;
  private final Integer quantity;
  
  private Map<String, Object> food;

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

  public Map<String, Object> getFood() {
    return food;
  }

  public void setFood(Map<String, Object> food) {
    this.food = food;
  }
}

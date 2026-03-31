package cn.edu.tju.food.model.bo;

import cn.edu.tju.core.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_request_log")
public class StockRequestLog extends BaseEntity {
  @Column(nullable = false, unique = true, length = 128)
  private String requestId;

  @Column(nullable = false, length = 32)
  private String action;

  @Column(length = 128)
  private String orderId;

  @Column(nullable = false)
  private Boolean success;

  public String getRequestId() { return requestId; }
  public void setRequestId(String requestId) { this.requestId = requestId; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public String getOrderId() { return orderId; }
  public void setOrderId(String orderId) { this.orderId = orderId; }
  public Boolean getSuccess() { return success; }
  public void setSuccess(Boolean success) { this.success = success; }
}

package cn.edu.tju.order.model.vo;

import cn.edu.tju.order.model.bo.Review;

public class ReviewSnapshotVO {
  private Long id;
  private Long customerId;
  private Long businessId;
  private Long orderId;
  private Boolean anonymous;
  private Integer stars;
  private String content;

  public ReviewSnapshotVO() {}

  public ReviewSnapshotVO(Review review) {
    if (review == null) {
      return;
    }
    this.id = review.getId();
    this.customerId = review.getCustomerId();
    this.businessId = review.getBusinessId();
    this.orderId = review.getOrderId();
    this.anonymous = review.getAnonymous();
    this.stars = review.getStars();
    this.content = review.getContent();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public Boolean getAnonymous() {
    return anonymous;
  }

  public void setAnonymous(Boolean anonymous) {
    this.anonymous = anonymous;
  }

  public Integer getStars() {
    return stars;
  }

  public void setStars(Integer stars) {
    this.stars = stars;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}

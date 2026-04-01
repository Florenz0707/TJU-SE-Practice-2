package cn.edu.tju.elm.model.event;

public class PointsReviewSuccessEvent {
  private Long userId;
  private String bizId;
  private Integer amount;
  private String eventTime;
  private String extraInfo;

  public PointsReviewSuccessEvent() {}

  public PointsReviewSuccessEvent(
      Long userId, String bizId, Integer amount, String eventTime, String extraInfo) {
    this.userId = userId;
    this.bizId = bizId;
    this.amount = amount;
    this.eventTime = eventTime;
    this.extraInfo = extraInfo;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getBizId() {
    return bizId;
  }

  public void setBizId(String bizId) {
    this.bizId = bizId;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }

  public String getEventTime() {
    return eventTime;
  }

  public void setEventTime(String eventTime) {
    this.eventTime = eventTime;
  }

  public String getExtraInfo() {
    return extraInfo;
  }

  public void setExtraInfo(String extraInfo) {
    this.extraInfo = extraInfo;
  }
}

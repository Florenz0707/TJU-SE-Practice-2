package cn.edu.tju.merchant.model;

import cn.edu.tju.merchant.model.BaseEntity;
import cn.edu.tju.merchant.model.UserSummaryView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

@Entity
public class BusinessApplication extends BaseEntity {
  @ManyToOne
  @JoinColumn(name = "business_id", nullable = false)
  private Business business;

  @Column(name = "handler_id", nullable = false)
  private Long handlerId;

  @Transient private UserSummaryView handler;

  @Column(columnDefinition = "TEXT")
  private String applicationExplain;

  private Integer applicationState;

  @Column(length = 500)
  private String rejectReason;

  public Business getBusiness() {
    return business;
  }

  public void setBusiness(Business business) {
    this.business = business;
  }

  public Long getHandlerId() {
    return handlerId;
  }

  public void setHandlerId(Long handlerId) {
    this.handlerId = handlerId;
  }

  public UserSummaryView getHandler() {
    return handler;
  }

  public void setHandler(UserSummaryView handler) {
    this.handler = handler;
  }

  public String getApplicationExplain() {
    return applicationExplain;
  }

  public void setApplicationExplain(String applicationExplain) {
    this.applicationExplain = applicationExplain;
  }

  public Integer getApplicationState() {
    return applicationState;
  }

  public void setApplicationState(Integer applicationState) {
    this.applicationState = applicationState;
  }

  public String getRejectReason() {
    return rejectReason;
  }

  public void setRejectReason(String rejectReason) {
    this.rejectReason = rejectReason;
  }
}

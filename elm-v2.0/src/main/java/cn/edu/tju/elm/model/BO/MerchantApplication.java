package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.elm.model.VO.UserSummaryView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

@Entity
public class MerchantApplication extends BaseEntity {
  @Column(name = "applicant_id", nullable = false)
  private Long applicantId;

  @Transient private UserSummaryView applicant;

  @Column(columnDefinition = "TEXT")
  private String applicationExplain;

  private Integer applicationState;

  @Column(length = 500)
  private String rejectReason;

  @Column(name = "handler_id", nullable = false)
  private Long handlerId;

  @Transient private UserSummaryView handler;

  public Long getApplicantId() {
    return applicantId;
  }

  public void setApplicantId(Long applicantId) {
    this.applicantId = applicantId;
  }

  public UserSummaryView getApplicant() {
    return applicant;
  }

  public void setApplicant(UserSummaryView applicant) {
    this.applicant = applicant;
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

  public String getRejectReason() {
    return rejectReason;
  }

  public void setRejectReason(String rejectReason) {
    this.rejectReason = rejectReason;
  }
}

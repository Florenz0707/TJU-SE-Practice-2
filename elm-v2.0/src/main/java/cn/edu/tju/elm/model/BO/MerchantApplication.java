package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class MerchantApplication extends BaseEntity {
  @Column(name = "applicant_id", nullable = false)
  private Long applicantId;

  @Column(columnDefinition = "TEXT")
  private String applicationExplain;

  private Integer applicationState;

  @Column(length = 500)
  private String rejectReason;

  @Column(name = "handler_id", nullable = false)
  private Long handlerId;

  public Long getApplicantId() {
    return applicantId;
  }

  public void setApplicantId(Long applicantId) {
    this.applicantId = applicantId;
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

  public String getRejectReason() {
    return rejectReason;
  }

  public void setRejectReason(String rejectReason) {
    this.rejectReason = rejectReason;
  }
}

package cn.edu.tju.elm.model;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.core.model.User;
import jakarta.persistence.*;

@Entity
public class MerchantApplication extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Column(columnDefinition = "TEXT")
    private String applicationExplain;

    private Integer applicationState;

    @ManyToOne
    @JoinColumn(name = "handler_id", nullable = false)
    private User handler;

    public User getApplicant() { return applicant; }

    public void setApplicant(User applicant) { this.applicant = applicant; }

    public String getApplicationExplain() { return applicationExplain; }

    public void setApplicationExplain(String applicationExplain) { this.applicationExplain = applicationExplain; }

    public Integer getApplicationState() { return applicationState; }

    public void setApplicationState(Integer applicationState) { this.applicationState = applicationState; }

    public User getHandler() { return handler; }

    public void setHandler(User handler) { this.handler = handler; }
}

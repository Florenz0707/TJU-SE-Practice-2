package cn.edu.tju.elm.model;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.core.model.User;
import jakarta.persistence.*;

@Entity
public class BusinessApplication extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Column(columnDefinition = "TEXT")
    private String applicationExplein;

    private Integer applicationState;

    public Business getBusiness() { return business; }

    public void setBusiness(Business business) { this.business = business; }

    public User getApprover() { return approver; }

    public void setApprover(User approver) { this.approver = approver; }

    public String getApplicationExplein() { return applicationExplein; }

    public void setApplicationExplein(String applicationExplein) {this.applicationExplein = applicationExplein; }

    public Integer getApplicationState() { return applicationState; }

    public void setApplicationState(Integer applicationState) { this.applicationState = applicationState; }
}

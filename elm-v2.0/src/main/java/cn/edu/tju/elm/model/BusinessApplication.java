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
    @JoinColumn(name = "handler_id", nullable = false)
    private User handler;

    @Column(columnDefinition = "TEXT")
    private String applicationExplain;

    private Integer applicationState;

    public Business getBusiness() { return business; }

    public void setBusiness(Business business) { this.business = business; }

    public User getHandler() { return handler; }

    public void setHandler(User handler) { this.handler = handler; }

    public String getApplicationExplain() { return applicationExplain; }

    public void setApplicationExplain(String applicationExplain) {this.applicationExplain = applicationExplain; }

    public Integer getApplicationState() { return applicationState; }

    public void setApplicationState(Integer applicationState) { this.applicationState = applicationState; }
}

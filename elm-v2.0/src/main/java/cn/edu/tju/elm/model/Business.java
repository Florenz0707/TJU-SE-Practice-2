package cn.edu.tju.elm.model;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.core.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class Business extends BaseEntity {
    @Column(nullable=false)
    private String businessName;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User businessOwner;
    private String businessAddress;
    private String businessExplain;
    @Column(columnDefinition = "TEXT")
    private String businessImg;
    private Integer orderTypeId;
    @Column(precision = 10, scale = 2)
    private BigDecimal startPrice;
    @Column(precision = 10, scale = 2)
    private BigDecimal deliveryPrice;
    private String remarks;

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }

    public String getBusinessExplain() {
        return businessExplain;
    }

    public void setBusinessExplain(String businessExplain) {
        this.businessExplain = businessExplain;
    }

    public String getBusinessImg() {
        return businessImg;
    }

    public void setBusinessImg(String businessImg) {
        this.businessImg = businessImg;
    }

    public Integer getOrderTypeId() {
        return orderTypeId;
    }

    public void setOrderTypeId(Integer orderTypeId) {
        this.orderTypeId = orderTypeId;
    }

    public BigDecimal getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(BigDecimal startPrice) {
        this.startPrice = startPrice;
    }

    public BigDecimal getDeliveryPrice() {
        return deliveryPrice;
    }

    public void setDeliveryPrice(BigDecimal deliveryPrice) {
        this.deliveryPrice = deliveryPrice;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public User getBusinessOwner() {
        return businessOwner;
    }

    public void setBusinessOwner(User businessOwner) {
        this.businessOwner = businessOwner;
    }
}

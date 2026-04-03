package cn.edu.tju.product.model;

import cn.edu.tju.core.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "food")
public class Food extends BaseEntity {

    @Column(name = "food_name", nullable = false)
    private String foodName;

    @Column(name = "food_explain")
    private String foodExplain;

    @Column(name = "food_img")
    private String foodImg;

    @Column(name = "food_price", nullable = false)
    private Double foodPrice;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "remarks")
    private String remarks;

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getFoodExplain() {
        return foodExplain;
    }

    public void setFoodExplain(String foodExplain) {
        this.foodExplain = foodExplain;
    }

    public String getFoodImg() {
        return foodImg;
    }

    public void setFoodImg(String foodImg) {
        this.foodImg = foodImg;
    }

    public Double getFoodPrice() {
        return foodPrice;
    }

    public void setFoodPrice(Double foodPrice) {
        this.foodPrice = foodPrice;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    @Transient
    public BusinessDto getBusiness() {
        if (this.businessId == null) return null;
        BusinessDto dto = new BusinessDto();
        dto.setId(this.businessId);
        return dto;
    }

    @Transient
    public void setBusiness(BusinessDto business) {
        if (business != null) {
            this.businessId = business.getId();
        }
    }

    public static class BusinessDto {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }
}
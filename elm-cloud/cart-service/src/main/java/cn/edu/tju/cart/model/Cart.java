package cn.edu.tju.cart.model;

import cn.edu.tju.core.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "cart")
public class Cart extends BaseEntity {
    // 微服务内持久化仍用 *_Id，前端 contract 需要嵌套对象：food/business/customer。
    private String foodId;
    private String businessId;
    private String userId;
    private Integer quantity;

    /**
     * Transient enrichment for frontend display.
     * When present, it should contain at least: id, foodName, foodPrice, foodImg.
     */
    @Transient
    private Map<String, Object> foodDetail;

    @Transient
    private Map<String, Object> businessDetail;

    @Transient
    private Map<String, Object> customerDetail;

    public String getFoodId() {
        return foodId;
    }

    public void setFoodId(String foodId) {
        this.foodId = foodId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Map<String, Object> getFoodDetail() {
        return foodDetail;
    }

    public void setFoodDetail(Map<String, Object> foodDetail) {
        this.foodDetail = foodDetail;
    }

    public Map<String, Object> getBusinessDetail() {
        return businessDetail;
    }

    public void setBusinessDetail(Map<String, Object> businessDetail) {
        this.businessDetail = businessDetail;
    }

    public Map<String, Object> getCustomerDetail() {
        return customerDetail;
    }

    public void setCustomerDetail(Map<String, Object> customerDetail) {
        this.customerDetail = customerDetail;
    }

    /**
     * 兼容部分旧代码/数据：如果存在 cartId，优先保持；否则用 BaseEntity.id。
     * 前端只关心 data.id，因此这里不暴露 cartId 字段。
     */

    @Transient
    public Object getFood() {
        if (this.foodDetail != null) return this.foodDetail;
        if (this.foodId == null) return null;
        return new IdOnlyView(this.foodId);
    }

    @Transient
    public void setFood(Object food) {
        if (food == null) {
            this.foodId = null;
            this.foodDetail = null;
            return;
        }
        if (food instanceof IdOnlyView view) {
            this.foodId = view.getId();
            return;
        }
        if (food instanceof Map<?, ?> map) {
            Object id = map.get("id");
            this.foodId = id == null ? null : String.valueOf(id);
            java.util.LinkedHashMap<String, Object> copied = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copied.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            this.foodDetail = copied;
        }
    }

    @Transient
    public Object getBusiness() {
        if (this.businessDetail != null) return this.businessDetail;
        if (this.businessId == null) return null;
        return new IdOnlyView(this.businessId);
    }

    @Transient
    public void setBusiness(Object business) {
        if (business == null) {
            this.businessId = null;
            this.businessDetail = null;
            return;
        }
        if (business instanceof IdOnlyView view) {
            this.businessId = view.getId();
            return;
        }
        if (business instanceof Map<?, ?> map) {
            Object id = map.get("id");
            this.businessId = id == null ? null : String.valueOf(id);
            java.util.LinkedHashMap<String, Object> copied = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copied.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            this.businessDetail = copied;
        }
    }

    @Transient
    public Object getCustomer() {
        if (this.customerDetail != null) return this.customerDetail;
        if (this.userId == null) return null;
        return new IdOnlyView(this.userId);
    }

    @Transient
    public void setCustomer(Object customer) {
        if (customer == null) {
            this.userId = null;
            this.customerDetail = null;
            return;
        }
        if (customer instanceof IdOnlyView view) {
            this.userId = view.getId();
            return;
        }
        if (customer instanceof Map<?, ?> map) {
            Object id = map.get("id");
            this.userId = id == null ? null : String.valueOf(id);
            java.util.LinkedHashMap<String, Object> copied = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copied.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            this.customerDetail = copied;
        }
    }

    @Data
    public static class IdOnlyView {
        private String id;
        public IdOnlyView() {}
        public IdOnlyView(String id) { this.id = id; }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}

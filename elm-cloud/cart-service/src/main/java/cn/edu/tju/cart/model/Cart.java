package cn.edu.tju.cart.model;

import cn.edu.tju.core.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "cart")
public class Cart extends BaseEntity {
    private String cartId;
    private String foodId;
    private String businessId;
    private String userId;
    private Integer quantity;
}

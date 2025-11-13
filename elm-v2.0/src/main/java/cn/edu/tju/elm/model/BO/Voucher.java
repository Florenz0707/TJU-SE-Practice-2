package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.elm.utils.EntityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.math.BigDecimal;

@Entity
public class Voucher extends BaseEntity {
    @Column(nullable=false)
    private BigDecimal threshold;

    @Column(nullable=false)
    private BigDecimal value;

    @Column(nullable=false)
    private Boolean claimable;

    @Column(nullable = false)
    private Boolean available;

    public static Voucher createVoucher(BigDecimal threshold, BigDecimal value, Boolean claimable, Boolean available) {
        Voucher voucher = new Voucher();
        voucher.threshold = threshold;
        voucher.value = value;
        voucher.claimable = claimable;
        voucher.available = available;
        EntityUtils.setNewEntity(voucher);
        return voucher;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public BigDecimal getValue() {
        return value;
    }

    public Boolean getClaimable() {
        return claimable;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public void setClaimable(Boolean claimable) {
        this.claimable = claimable;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}

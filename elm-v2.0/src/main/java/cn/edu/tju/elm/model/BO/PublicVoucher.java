package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.elm.utils.EntityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.math.BigDecimal;

@Entity
public class PublicVoucher extends BaseEntity {
    @Column(nullable = false)
    private BigDecimal threshold;

    @Column(nullable = false)
    private BigDecimal faceValue;

    // 是否可领取
    @Column(nullable = false)
    private Boolean claimable;

    @Column(nullable = false)
    private Integer validDays;

    public static PublicVoucher createVoucher(BigDecimal threshold, BigDecimal value, Boolean claimable, Integer valid_days) {
        PublicVoucher publicVoucher = new PublicVoucher();
        publicVoucher.threshold = threshold;
        publicVoucher.faceValue = value;
        publicVoucher.claimable = claimable;
        publicVoucher.validDays = valid_days;
        EntityUtils.setNewEntity(publicVoucher);
        return publicVoucher;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public BigDecimal getFaceValue() {
        return faceValue;
    }

    public Boolean getClaimable() {
        return claimable;
    }

    public Integer getValidDays() {
        return validDays;
    }
}

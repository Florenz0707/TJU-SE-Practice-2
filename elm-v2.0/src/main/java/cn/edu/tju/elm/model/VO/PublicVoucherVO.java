package cn.edu.tju.elm.model.VO;

import cn.edu.tju.elm.model.BO.PublicVoucher;

import java.io.Serializable;
import java.math.BigDecimal;

public class PublicVoucherVO implements Serializable {
    private Long id;
    private BigDecimal threshold;
    private BigDecimal value;
    private Boolean claimable;
    private Integer valid_days;

    public PublicVoucherVO() {
    }

    public PublicVoucherVO(PublicVoucher publicVoucher) {
        id = publicVoucher.getId();
        threshold = publicVoucher.getThreshold();
        value = publicVoucher.getValue();
        claimable = publicVoucher.getClaimable();
        valid_days = publicVoucher.getValid_days();
    }

    public Long getId() {
        return id;
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

    public Integer getValid_days() {
        return valid_days;
    }

    @Override
    public String toString() {
        return "VoucherVO: " +
                "id=" + id +
                ", threshold=" + threshold +
                ", value=" + value +
                ", claimable=" + claimable +
                ", valid_days=" + valid_days;
    }
}

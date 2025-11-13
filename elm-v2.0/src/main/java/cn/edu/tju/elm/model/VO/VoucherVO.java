package cn.edu.tju.elm.model.VO;

import cn.edu.tju.elm.model.BO.Voucher;

import java.io.Serializable;
import java.math.BigDecimal;

public class VoucherVO implements Serializable {
    private final Long id;
    private final BigDecimal threshold;
    private final BigDecimal value;
    private final Boolean claimable;
    private final Boolean available;

    public VoucherVO(Voucher voucher) {
        id = voucher.getId();
        threshold = voucher.getThreshold();
        value = voucher.getValue();
        claimable = voucher.getClaimable();
        available = voucher.getAvailable();
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

    public Boolean getAvailable() {
        return available;
    }

    @Override
    public String toString() {
        return "VoucherVO: " +
                "id=" + id +
                ", threshold=" + threshold +
                ", value=" + value +
                ", claimable=" + claimable +
                ", available=" + available;
    }
}

package cn.edu.tju.elm.model.VO;

import cn.edu.tju.elm.model.BO.PrivateVoucher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PrivateVoucherVO {
    private Long id;
    private Long ownerId;
    private BigDecimal value;
    private LocalDateTime expiryDate;

    public PrivateVoucherVO() {
    }

    public PrivateVoucherVO(PrivateVoucher privateVoucher) {
        id = privateVoucher.getId();
        ownerId = privateVoucher.getOwner().getId();
        value = privateVoucher.getValue();
        expiryDate = privateVoucher.getExpiryDate();
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public BigDecimal getValue() {
        return value;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    @Override
    public String toString() {
        return "PrivateVoucherVO{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", value=" + value +
                ", expiryDate=" + expiryDate +
                '}';
    }
}
